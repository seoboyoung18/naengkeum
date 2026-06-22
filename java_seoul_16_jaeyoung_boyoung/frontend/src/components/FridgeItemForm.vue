<script setup>
import { ref, reactive, watch, onBeforeUnmount } from 'vue'
import { autocompleteIngredients, suggestIngredient } from '../api/ingredients'
import { createFridgeItem, updateFridgeItem } from '../api/fridge'
import { todayISO, addDaysISO, daysFieldFor } from '../utils/date'

const props = defineProps({
  item: { type: Object, default: null }, // null이면 추가, 있으면 수정
})
const emit = defineEmits(['close', 'saved'])

const isEdit = !!props.item

const form = reactive({
  name: props.item?.name ?? '',
  qty: props.item?.qty ?? 1,
  unit: props.item?.unit ?? '개',
  storageType: props.item?.storageType ?? 'FRIDGE',
  expiryDate: props.item?.expiryDate ?? todayISO(),
  memo: props.item?.memo ?? '',
})

const loading = ref(false)
const error = ref('')

// ----- 식재료 자동완성 -----
const suggestions = ref([])
const showSuggest = ref(false)
const lastSuggest = ref(null) // suggest 응답 보관(보관위치 변경 시 기한 재계산용)
let debounceTimer = null

function onNameInput() {
  lastSuggest.value = null
  clearTimeout(debounceTimer)
  const kw = form.name.trim()
  if (!kw) { suggestions.value = []; showSuggest.value = false; return }
  debounceTimer = setTimeout(async () => {
    try {
      suggestions.value = await autocompleteIngredients(kw)
      showSuggest.value = suggestions.value.length > 0
    } catch (_) { /* 무시 */ }
  }, 200)
}

async function pickSuggestion(s) {
  form.name = s.name
  showSuggest.value = false
  await applySuggest(s.name)
}

// 보관기한/보관법 제안 적용: 보관위치·유통기한 자동 세팅
async function applySuggest(name) {
  try {
    const sug = await suggestIngredient(name)
    lastSuggest.value = sug
    if (sug.found && sug.defaultStorageType) {
      form.storageType = sug.defaultStorageType
    }
    recomputeExpiry()
  } catch (_) { /* 무시 */ }
}

function recomputeExpiry() {
  const sug = lastSuggest.value
  if (!sug) return
  const field = daysFieldFor(form.storageType)
  const days = sug[field] ?? sug.fridgeDays ?? sug.roomTempDays ?? sug.freezerDays ?? 7
  form.expiryDate = addDaysISO(todayISO(), days)
}

// 보관위치를 수동으로 바꾸면, 제안값이 있으면 기한 재계산
watch(() => form.storageType, () => { if (lastSuggest.value) recomputeExpiry() })

const storageTip = () => (lastSuggest.value?.found ? lastSuggest.value.storageTip : '')

async function onSubmit() {
  error.value = ''
  loading.value = true
  try {
    const payload = {
      name: form.name.trim(),
      qty: Number(form.qty),
      unit: form.unit.trim(),
      storageType: form.storageType,
      expiryDate: form.expiryDate,
      memo: form.memo?.trim() || null,
    }
    const saved = isEdit
      ? await updateFridgeItem(props.item.fridgeItemId, payload)
      : await createFridgeItem(payload)
    emit('saved', saved)
  } catch (e) {
    error.value = e.response?.data?.message || '저장에 실패했습니다'
  } finally {
    loading.value = false
  }
}

onBeforeUnmount(() => clearTimeout(debounceTimer))
</script>

<template>
  <div class="overlay" @click.self="emit('close')">
    <div class="sheet">
      <div class="sheet-hd">
        <h3>{{ isEdit ? '재료 수정' : '재료 추가' }}</h3>
        <button class="x" @click="emit('close')">✕</button>
      </div>

      <form @submit.prevent="onSubmit">
        <label>재료명</label>
        <div class="ac">
          <input
            v-model="form.name"
            type="text"
            placeholder="예: 양파"
            autocomplete="off"
            @input="onNameInput"
            @focus="onNameInput"
            @blur="() => setTimeout(() => (showSuggest = false), 150)"
          />
          <ul v-if="showSuggest" class="ac-list">
            <li v-for="s in suggestions" :key="s.ingredientDictId" @mousedown.prevent="pickSuggestion(s)">
              {{ s.name }} <span class="cat">{{ s.category }}</span>
            </li>
          </ul>
        </div>
        <p v-if="storageTip()" class="tip">💡 {{ storageTip() }}</p>

        <div class="grid2">
          <div>
            <label>수량</label>
            <input v-model="form.qty" type="number" min="0" step="0.1" />
          </div>
          <div>
            <label>단위</label>
            <div class="seg seg-unit">
              <button type="button" :class="{ on: form.unit === '개' }" @click="form.unit = '개'">개</button>
              <button type="button" :class="{ on: form.unit === 'g' }" @click="form.unit = 'g'">g</button>
              <button type="button" :class="{ on: form.unit === 'ml' }" @click="form.unit = 'ml'">ml</button>
            </div>
          </div>
        </div>

        <label>보관 위치</label>
        <div class="seg">
          <button type="button" :class="{ on: form.storageType === 'FRIDGE' }" @click="form.storageType = 'FRIDGE'">냉장</button>
          <button type="button" :class="{ on: form.storageType === 'FREEZER' }" @click="form.storageType = 'FREEZER'">냉동</button>
          <button type="button" :class="{ on: form.storageType === 'ROOM_TEMP' }" @click="form.storageType = 'ROOM_TEMP'">실온</button>
        </div>

        <label>유통기한</label>
        <input v-model="form.expiryDate" type="date" />

        <label>메모 (선택)</label>
        <input v-model="form.memo" type="text" placeholder="예: 반 개 사용함" />

        <p v-if="error" class="err">{{ error }}</p>

        <button class="submit" type="submit" :disabled="loading">
          {{ loading ? '저장 중…' : isEdit ? '수정' : '추가' }}
        </button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.overlay { position: fixed; inset: 0; background: rgba(17,24,39,.5); display: flex; align-items: center; justify-content: center; padding: 20px; box-sizing: border-box; z-index: 50; }
.sheet { width: 100%; max-width: 480px; background: #fff; border-radius: 16px; padding: 22px; max-height: 90vh; overflow-y: auto; box-shadow: 0 20px 48px rgba(0,0,0,.18); }
.sheet-hd { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.sheet-hd h3 { margin: 0; font-size: 17px; }
.x { border: none; background: none; font-size: 18px; cursor: pointer; color: #888; }
form { display: flex; flex-direction: column; }
label { font-size: 13px; color: #555; margin: 12px 0 4px; }
input { padding: 11px 12px; border: 1px solid var(--line); border-radius: 8px; font-size: 14px; width: 100%; }
.ac { position: relative; }
.ac-list { position: absolute; top: 100%; left: 0; right: 0; background: #fff; border: 1px solid var(--line); border-radius: 8px; margin: 4px 0 0; padding: 4px 0; list-style: none; max-height: 200px; overflow-y: auto; z-index: 5; box-shadow: 0 6px 18px rgba(0,0,0,.08); }
.ac-list li { padding: 9px 12px; cursor: pointer; font-size: 14px; }
.ac-list li:hover { background: #f5f7f9; }
.ac-list .cat { color: #999; font-size: 12px; margin-left: 6px; }
.tip { font-size: 12px; color: var(--primary-deep); margin: 6px 0 0; }
.grid2 { display: flex; gap: 10px; }
.grid2 > div { flex: 1; }
.seg { display: flex; gap: 6px; }
.seg button { flex: 1; padding: 10px; border: 1px solid var(--line); background: #fff; border-radius: 8px; font-size: 14px; cursor: pointer; color: #555; }
.seg button.on { border-color: var(--primary-deep); background: var(--primary-tint); color: var(--primary-deep); font-weight: 700; }
.seg-unit button { padding: 11px 6px; }
.err { color: #e11d48; font-size: 13px; margin: 12px 0 0; }
.submit { margin-top: 18px; padding: 13px; border: none; border-radius: 8px; background: var(--primary); color: var(--on-primary); font-size: 15px; font-weight: 700; cursor: pointer; }
.submit:disabled { opacity: .6; }
</style>
