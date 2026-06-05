<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { API_BASE } from '../api/http'
import { TOKEN_KEY } from '../stores/auth'
import { addRecipeWish, saveAiRecipe } from '../api/wishlist'

const router = useRouter()

const options = reactive({ prioritizeExpiry: true, useAllFridge: false, applyAllergy: true })

const streaming = ref(false)
const started = ref(false)
const done = ref(false)
const error = ref('')          // 연결 전 오류(빈 냉장고 400 / 키 미설정 503)
const streamError = ref('')    // 스트리밍 중 error 이벤트

const result = reactive({
  source: null,               // { origin: 'DB'|'AI', recipeId? }
  title: '',
  summary: '',
  ingredients: [],
  steps: [],
  meta: null,
})
const rawLog = ref([])

const saving = ref(false)
const saved = ref(false)

function resetResult() {
  result.source = null
  result.title = ''
  result.summary = ''
  result.ingredients = []
  result.steps = []
  result.meta = null
  rawLog.value = []
  streamError.value = ''
  done.value = false
  saved.value = false
}

async function run() {
  resetResult()
  error.value = ''
  started.value = true
  streaming.value = true

  try {
    const res = await fetch(`${API_BASE}/api/ai/recommend`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem(TOKEN_KEY)}`,
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream',
      },
      body: JSON.stringify(options),
    })

    // 연결 전 차단(400 빈 냉장고 / 503 키 미설정 등) → JSON 에러
    if (!res.ok) {
      let msg = `요청 실패 (HTTP ${res.status})`
      try { const j = await res.json(); msg = j.message || msg } catch (_) {}
      error.value = msg
      streaming.value = false
      return
    }

    const reader = res.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''

    for (;;) {
      const { value, done: rdDone } = await reader.read()
      if (rdDone) break
      buffer += decoder.decode(value, { stream: true })

      let idx
      while ((idx = buffer.indexOf('\n\n')) >= 0) {
        const chunk = buffer.slice(0, idx).trim()
        buffer = buffer.slice(idx + 2)
        if (!chunk) continue
        const dataLine = chunk.split('\n').find((l) => l.startsWith('data:'))
        if (!dataLine) continue
        const json = dataLine.slice(5).trim()
        rawLog.value.push(json)
        try { handleEvent(JSON.parse(json)) } catch (_) {}
      }
    }
    done.value = true
  } catch (e) {
    error.value = '스트리밍 중 네트워크 오류가 발생했습니다'
  } finally {
    streaming.value = false
  }
}

function handleEvent(ev) {
  switch (ev.type) {
    case 'source': result.source = ev.value; break
    case 'title': result.title = ev.value; break
    case 'summary': result.summary = ev.value; break
    case 'ingredient': result.ingredients.push(ev.value); break
    case 'step': result.steps.push(ev.value); break
    case 'meta': result.meta = ev.value; break
    case 'error': streamError.value = ev.value || 'AI 처리 중 오류'; break
    case 'done': done.value = true; break
  }
}

async function save() {
  if (saving.value || saved.value) return
  saving.value = true
  try {
    if (result.source?.origin === 'DB' && result.source.recipeId) {
      await addRecipeWish(result.source.recipeId)
    } else {
      await saveAiRecipe({
        title: result.title,
        summary: result.summary || null,
        ingredientsJson: result.ingredients,
        stepsJson: result.steps,
        cookTime: result.meta?.cookTime ?? null,
      })
    }
    saved.value = true
  } catch (e) {
    if (e.response?.status === 409) saved.value = true // 이미 찜됨
    else alert(e.response?.data?.message || '저장 실패')
  } finally {
    saving.value = false
  }
}

function goRecipe() {
  if (result.source?.origin === 'DB' && result.source.recipeId) {
    router.push({ name: 'recipe-detail', params: { recipeId: result.source.recipeId } })
  }
}
</script>

<template>
  <section>
    <h2 class="h">🤖 AI 레시피 추천</h2>
    <p class="lead">냉장고 재료로 만들 수 있는 레시피를 추천해 드려요.</p>

    <!-- 옵션 -->
    <div class="opts">
      <label><input type="checkbox" v-model="options.prioritizeExpiry" /> 유통기한 임박 우선</label>
      <label><input type="checkbox" v-model="options.useAllFridge" /> 냉장고 재료 전부 사용</label>
      <label><input type="checkbox" v-model="options.applyAllergy" /> 알레르기 반영</label>
    </div>

    <button class="run" :disabled="streaming" @click="run">
      {{ streaming ? '추천 받는 중…' : started ? '다시 추천 받기' : '🍳 추천 받기' }}
    </button>

    <p v-if="error" class="err">⚠️ {{ error }}</p>

    <!-- 결과 -->
    <div v-if="started && !error" class="result">
      <div v-if="result.source" class="badge" :class="result.source.origin === 'DB' ? 'db' : 'ai'">
        {{ result.source.origin === 'DB' ? `📚 보유 재료 기반 추천 레시피` : '🤖 AI 생성 레시피' }}
      </div>

      <h3 v-if="result.title" class="title">{{ result.title }}</h3>
      <p v-if="result.summary" class="summary">{{ result.summary }}</p>

      <template v-if="result.ingredients.length">
        <div class="sec">재료</div>
        <ul class="ings">
          <li v-for="(i, idx) in result.ingredients" :key="idx" :class="{ owned: i.owned }">
            <span>{{ i.owned ? '✓ ' : '' }}{{ i.name }} <span class="q">{{ i.qty }}{{ i.unit || '' }}</span></span>
            <span class="tag" :class="i.owned ? 'y' : 'n'">{{ i.owned ? '보유' : '구매' }}</span>
          </li>
        </ul>
      </template>

      <template v-if="result.steps.length">
        <div class="sec">조리 순서</div>
        <ol class="steps">
          <li v-for="s in result.steps" :key="s.stepNumber">
            <div class="n">{{ s.stepNumber }}</div>
            <div class="d">{{ s.description }}</div>
          </li>
        </ol>
      </template>

      <div v-if="result.meta" class="meta">
        <span v-if="result.meta.cookTime">⏱ {{ result.meta.cookTime }}분</span>
        <span v-if="result.meta.difficulty">📊 {{ result.meta.difficulty }}</span>
        <span v-if="result.meta.servings">🍽 {{ result.meta.servings }}인분</span>
      </div>

      <p v-if="streamError" class="err">⚠️ {{ streamError }} (다시 시도해 보세요)</p>
      <p v-else-if="streaming" class="muted">스트리밍 중…</p>

      <!-- 저장/이동 -->
      <div v-if="done && result.title" class="actions">
        <button v-if="result.source?.origin === 'DB'" class="ghost" @click="goRecipe">레시피 상세 보기</button>
        <button class="save" :disabled="saving || saved" @click="save">
          {{ saved ? '✓ 저장됨' : saving ? '저장 중…' : '찜 저장' }}
        </button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.lead { color: #777; font-size: 13px; margin: 0 0 14px; }
.opts { display: flex; flex-direction: column; gap: 10px; background: #fff; border: 1px solid #eee; border-radius: 12px; padding: 14px; }
.opts label { display: flex; align-items: center; gap: 8px; font-size: 14px; color: #444; }
.run { width: 100%; margin-top: 14px; padding: 14px; border: none; border-radius: 10px; background: #16a34a; color: #fff; font-size: 15px; font-weight: 700; cursor: pointer; }
.run:disabled { opacity: .6; }
.err { color: #e11d48; font-size: 13px; margin: 12px 0 0; }

.result { margin-top: 18px; background: #fff; border: 1px solid #eee; border-radius: 14px; padding: 16px; }
.badge { display: inline-block; font-size: 12px; font-weight: 700; padding: 5px 10px; border-radius: 999px; margin-bottom: 8px; }
.badge.db { background: #eff6ff; color: #2563eb; }
.badge.ai { background: #f5f3ff; color: #7c3aed; }
.title { font-size: 19px; margin: 4px 0; }
.summary { color: #666; font-size: 14px; margin: 0 0 6px; }
.sec { font-size: 13px; color: #999; margin: 16px 0 6px; }
.ings { list-style: none; padding: 0; margin: 0; }
.ings li { display: flex; justify-content: space-between; align-items: center; padding: 8px 10px; border: 1px solid #eee; border-radius: 8px; margin-bottom: 6px; font-size: 14px; }
.ings li.owned { border-color: #bbf7d0; }
.ings .q { color: #999; font-size: 13px; }
.tag { font-size: 11px; padding: 2px 8px; border-radius: 999px; }
.tag.y { background: #ecfdf3; color: #16a34a; }
.tag.n { background: #fff7ed; color: #f59e0b; }
.steps { list-style: none; padding: 0; margin: 0; }
.steps li { display: flex; gap: 10px; margin-bottom: 10px; }
.steps .n { flex: 0 0 24px; height: 24px; border-radius: 50%; background: #ecfdf3; color: #16a34a; font-weight: 700; display: flex; align-items: center; justify-content: center; font-size: 12px; }
.steps .d { flex: 1; font-size: 14px; }
.meta { display: flex; gap: 14px; color: #888; font-size: 13px; margin-top: 12px; }
.muted { color: #999; font-size: 13px; margin-top: 10px; }
.actions { display: flex; gap: 8px; margin-top: 16px; }
.ghost { flex: 1; border: 1px solid #16a34a; color: #16a34a; background: #fff; border-radius: 8px; padding: 11px; font-size: 14px; cursor: pointer; }
.save { flex: 1; border: none; background: #16a34a; color: #fff; border-radius: 8px; padding: 11px; font-size: 14px; font-weight: 700; cursor: pointer; }
.save:disabled { opacity: .6; }
</style>
