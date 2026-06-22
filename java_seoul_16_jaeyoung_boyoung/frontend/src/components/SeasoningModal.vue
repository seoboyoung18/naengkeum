<script setup>
import { ref, onMounted } from 'vue'
import { getSeasonings, saveSeasonings } from '../api/seasoning'

const emit = defineEmits(['close', 'saved'])

const STORAGE_LABEL = { FRIDGE: '냉장', FREEZER: '냉동', ROOM_TEMP: '실온' }

const items = ref([])
const selected = ref(new Set())
const loading = ref(false)
const saving = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await getSeasonings()
    items.value = data
    selected.value = new Set(data.filter((s) => s.owned).map((s) => s.seasoningId))
  } catch (e) {
    error.value = e.response?.data?.message || '조미료를 불러오지 못했습니다'
  } finally {
    loading.value = false
  }
}

// Set 재할당으로 반응성 보장(템플릿의 selected.has 재평가)
function toggle(id) {
  const next = new Set(selected.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  selected.value = next
}

async function onSave() {
  saving.value = true
  error.value = ''
  try {
    const updated = await saveSeasonings([...selected.value])
    emit('saved', updated)
  } catch (e) {
    error.value = e.response?.data?.message || '저장에 실패했습니다'
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="overlay" @click.self="emit('close')">
    <div class="sheet">
      <div class="sheet-hd">
        <h3>조미료 선택 <span class="sub">(중복 가능)</span></h3>
        <button class="x" @click="emit('close')">✕</button>
      </div>

      <p v-if="loading" class="muted">불러오는 중…</p>
      <p v-else-if="error" class="err">{{ error }}</p>

      <template v-else>
        <p class="hint">보유한 조미료를 탭하세요. 무게는 신경 쓰지 않아요.</p>
        <div class="grid">
          <button
            v-for="s in items" :key="s.seasoningId"
            type="button"
            :class="{ on: selected.has(s.seasoningId) }"
            :title="s.storageTip || ''"
            @click="toggle(s.seasoningId)"
          >
            <span class="nm">{{ s.name }}</span>
            <span class="st" :class="'st-' + s.storageType">{{ STORAGE_LABEL[s.storageType] }}</span>
          </button>
        </div>

        <div class="foot">
          <span class="count">{{ selected.size }}개 선택</span>
          <button class="submit" :disabled="saving" @click="onSave">
            {{ saving ? '저장 중…' : '선택 완료' }}
          </button>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.overlay { position: fixed; inset: 0; background: rgba(17,24,39,.5); display: flex; align-items: center; justify-content: center; padding: 20px; box-sizing: border-box; z-index: 50; }
.sheet { width: 100%; max-width: 480px; background: #fff; border-radius: 16px; padding: 22px; max-height: 90vh; overflow-y: auto; box-shadow: 0 20px 48px rgba(0,0,0,.18); }
.sheet-hd { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.sheet-hd h3 { margin: 0; font-size: 17px; }
.sheet-hd .sub { font-size: 13px; color: #999; font-weight: 400; }
.x { border: none; background: none; font-size: 18px; cursor: pointer; color: #888; }
.hint { font-size: 12px; color: #888; margin: 4px 0 12px; }
.grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; max-height: 56vh; overflow-y: auto; padding: 2px; }
.grid button { display: flex; flex-direction: column; align-items: center; gap: 4px; padding: 10px 4px; border: 1px solid var(--line); background: #fff; border-radius: 10px; font-size: 14px; color: #555; cursor: pointer; transition: all .12s; }
.grid button:hover { border-color: var(--primary); }
.grid button.on { border-color: var(--primary-deep); background: var(--primary-tint); color: var(--primary-deep); font-weight: 700; }
.grid .nm { font-size: 14px; line-height: 1.1; }
.grid .st { font-size: 10px; font-weight: 600; color: #999; background: #f1f3f5; border-radius: 5px; padding: 1px 6px; }
.grid .st-FRIDGE { color: var(--primary-deep); background: var(--primary-tint); }
.grid .st-FREEZER { color: #2563eb; background: #eff6ff; }
.grid .st-ROOM_TEMP { color: #b45309; background: #fff7ed; }
.foot { display: flex; align-items: center; justify-content: space-between; margin-top: 16px; }
.count { font-size: 13px; color: #888; }
.submit { padding: 12px 22px; border: none; border-radius: 8px; background: var(--primary); color: var(--on-primary); font-size: 15px; font-weight: 700; cursor: pointer; }
.submit:disabled { opacity: .6; }
.muted { color: #999; }
.err { color: #e11d48; font-size: 13px; }
</style>
