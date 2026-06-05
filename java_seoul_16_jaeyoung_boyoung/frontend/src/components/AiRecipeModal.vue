<script setup>
import { ref, onMounted } from 'vue'
import { fetchAiRecipe } from '../api/wishlist'

const props = defineProps({ aiRecipeId: { type: [String, Number], required: true } })
const emit = defineEmits(['close'])

const data = ref(null)
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    data.value = await fetchAiRecipe(props.aiRecipeId)
  } catch (e) {
    error.value = e.response?.data?.message || '불러오기 실패'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="overlay" @click.self="emit('close')">
    <div class="sheet">
      <div class="hd">
        <span class="badge">🤖 AI 레시피</span>
        <button class="x" @click="emit('close')">✕</button>
      </div>

      <p v-if="loading" class="muted">불러오는 중…</p>
      <p v-else-if="error" class="err">{{ error }}</p>

      <template v-else-if="data">
        <h3 class="title">{{ data.title }}</h3>
        <p v-if="data.summary" class="summary">{{ data.summary }}</p>
        <div v-if="data.cookTime" class="meta">⏱ {{ data.cookTime }}분</div>

        <div class="sec">재료</div>
        <ul class="ings">
          <li v-for="(i, idx) in data.ingredients" :key="idx" :class="{ owned: i.owned }">
            <span>{{ i.owned ? '✓ ' : '' }}{{ i.name }} <span class="q">{{ i.qty }}{{ i.unit || '' }}</span></span>
            <span class="tag" :class="i.owned ? 'y' : 'n'">{{ i.owned ? '보유' : '구매' }}</span>
          </li>
        </ul>

        <div class="sec">조리 순서</div>
        <ol class="steps">
          <li v-for="s in data.steps" :key="s.stepNumber">
            <div class="n">{{ s.stepNumber }}</div>
            <div class="d">{{ s.description }}</div>
          </li>
        </ol>
      </template>
    </div>
  </div>
</template>

<style scoped>
.overlay { position: fixed; inset: 0; background: rgba(0,0,0,.4); display: flex; align-items: flex-end; justify-content: center; z-index: 60; }
.sheet { width: 100%; max-width: 480px; background: #fff; border-radius: 16px 16px 0 0; padding: 18px; max-height: 88vh; overflow-y: auto; }
.hd { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.badge { font-size: 12px; font-weight: 700; background: #f5f3ff; color: #7c3aed; padding: 5px 10px; border-radius: 999px; }
.x { border: none; background: none; font-size: 18px; cursor: pointer; color: #888; }
.title { font-size: 19px; margin: 4px 0; }
.summary { color: #666; font-size: 14px; margin: 0; }
.meta { font-size: 13px; color: #888; margin-top: 6px; }
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
.muted { color: #999; }
.err { color: #e11d48; }
</style>
