<script setup>
import { ref, computed, onMounted } from 'vue'
import { listFridge, deleteFridgeItem } from '../api/fridge'
import { getSeasonings } from '../api/seasoning'
import FridgeItemForm from '../components/FridgeItemForm.vue'
import SeasoningModal from '../components/SeasoningModal.vue'
import InlineIcon from '../components/InlineIcon.vue'
import editSvg from '../assets/icons/edit.svg?raw'
import trashSvg from '../assets/icons/trash.svg?raw'
import hourglassSvg from '../assets/icons/hourglass.svg?raw'
import { useToast } from '../composables/useToast'

const toast = useToast()

const STORAGES = [
  { key: 'ALL', label: '전체' },
  { key: 'FRIDGE', label: '냉장' },
  { key: 'FREEZER', label: '냉동' },
  { key: 'ROOM_TEMP', label: '실온' },
]
const SORTS = [
  { key: 'EXPIRY_ASC', label: '임박순' },
  { key: 'CREATED_DESC', label: '최신순' },
  { key: 'NAME_ASC', label: '이름순' },
]

const items = ref([])
const summary = ref({ fridgeCount: 0, freezerCount: 0, roomTempCount: 0 })
const activeStorage = ref('ALL')
const sort = ref('EXPIRY_ASC')
const loading = ref(false)
const error = ref('')

const showForm = ref(false)
const editing = ref(null)

const showSeasoning = ref(false)
const ownedSeasonings = ref([])

const SEASONING_STORAGES = [
  { key: 'FRIDGE', label: '냉장' },
  { key: 'FREEZER', label: '냉동' },
  { key: 'ROOM_TEMP', label: '실온' },
]
// 보유 조미료를 냉장/냉동/실온으로 분류(권장 보관 위치 기준), 비어있는 그룹은 숨김
const seasoningGroups = computed(() =>
  SEASONING_STORAGES
    .map((g) => ({ ...g, items: ownedSeasonings.value.filter((s) => s.storageType === g.key) }))
    .filter((g) => g.items.length),
)

const imminentCount = computed(() => items.value.filter((i) => i.dDay <= 3).length)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await listFridge({ storageType: activeStorage.value, sort: sort.value })
    items.value = data.items
    summary.value = data.summary
  } catch (e) {
    error.value = e.response?.data?.message || '냉장고를 불러오지 못했습니다'
  } finally {
    loading.value = false
  }
}

function selectStorage(k) { activeStorage.value = k; load() }
function selectSort(k) { sort.value = k; load() }

function openAdd() { editing.value = null; showForm.value = true }
function openEdit(item) { editing.value = item; showForm.value = true }
function onSaved() { showForm.value = false; load(); toast.success('냉장고에 저장했어요') }

function openSeasoning() { showSeasoning.value = true }
async function loadSeasonings() {
  try {
    const data = await getSeasonings()
    ownedSeasonings.value = data.filter((s) => s.owned)
  } catch (_) { /* 무시 */ }
}
function onSeasoningSaved(updated) {
  ownedSeasonings.value = updated.filter((s) => s.owned)
  showSeasoning.value = false
  toast.success('조미료를 저장했어요')
}

async function onDelete(item) {
  if (!confirm(`'${item.name}'을(를) 삭제할까요?`)) return
  try {
    await deleteFridgeItem(item.fridgeItemId)
    load()
    toast.success('삭제했어요')
  } catch (e) {
    toast.error(e.response?.data?.message || '삭제에 실패했어요')
  }
}

const STORAGE_LABEL = { FRIDGE: '냉장', FREEZER: '냉동', ROOM_TEMP: '실온' }

function dDayText(d) {
  if (d < 0) return `D+${Math.abs(d)}`
  if (d === 0) return 'D-DAY'
  return `D-${d}`
}
function dDayClass(d) {
  if (d < 0) return 'expired'
  if (d <= 3) return 'soon'
  return 'ok'
}

onMounted(() => { load(); loadSeasonings() })
</script>

<template>
  <section>
    <div class="head">
      <h2 class="h">내 냉장고</h2>
      <div class="actions">
        <button class="add-btn" @click="openAdd">＋ 재료 추가</button>
        <button class="add-btn alt" @click="openSeasoning">＋ 조미료 추가</button>
      </div>
    </div>

    <!-- 요약 -->
    <ul class="summary">
      <li><span>{{ summary.fridgeCount }}</span>냉장</li>
      <li><span>{{ summary.freezerCount }}</span>냉동</li>
      <li><span>{{ summary.roomTempCount }}</span>실온</li>
      <li class="warn"><span>{{ imminentCount }}</span>임박</li>
    </ul>

    <!-- 보유 조미료 (냉장/냉동/실온 분류) -->
    <div class="seasoning-row">
      <span class="sr-label"><InlineIcon :svg="hourglassSvg" :size="16" /> 내 조미료</span>
      <div v-if="ownedSeasonings.length" class="sr-groups">
        <div class="sr-group" v-for="g in seasoningGroups" :key="g.key">
          <span class="sr-gl" :class="'sr-gl-' + g.key">{{ g.label }}</span>
          <div class="sr-chips">
            <span class="chip" v-for="s in g.items" :key="s.seasoningId">{{ s.name }}</span>
          </div>
        </div>
      </div>
      <span v-else class="sr-empty">아직 없어요 · ＋ 조미료 추가로 체크하세요</span>
    </div>

    <!-- 필터 -->
    <div class="filters">
      <div class="tabs">
        <button
          v-for="s in STORAGES" :key="s.key"
          :class="{ on: activeStorage === s.key }"
          @click="selectStorage(s.key)"
        >{{ s.label }}</button>
      </div>
      <select :value="sort" @change="selectSort($event.target.value)" class="sort">
        <option v-for="s in SORTS" :key="s.key" :value="s.key">{{ s.label }}</option>
      </select>
    </div>

    <!-- 목록 -->
    <p v-if="loading" class="muted">불러오는 중…</p>
    <p v-else-if="error" class="err">{{ error }}</p>
    <p v-else-if="items.length === 0" class="muted empty">재료가 없습니다. 우측 상단 ＋ 재료 추가 버튼으로 추가해 보세요.</p>

    <div v-else class="table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th>재료명</th><th>수량</th><th>보관</th><th>유통기한</th><th>D-day</th><th class="man">관리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="it in items" :key="it.fridgeItemId">
            <td class="name">{{ it.name }}<span v-if="it.memo" class="memo">· {{ it.memo }}</span></td>
            <td>{{ it.qty }}{{ it.unit }}</td>
            <td>{{ STORAGE_LABEL[it.storageType] }}</td>
            <td>{{ it.expiryDate }}</td>
            <td><span class="dday" :class="dDayClass(it.dDay)">{{ dDayText(it.dDay) }}</span></td>
            <td class="man">
              <button class="ic" @click="openEdit(it)"><InlineIcon :svg="editSvg" :size="16" /></button>
              <button class="ic" @click="onDelete(it)"><InlineIcon :svg="trashSvg" :size="16" /></button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 추가/수정 폼 -->
    <FridgeItemForm
      v-if="showForm"
      :item="editing"
      @close="showForm = false"
      @saved="onSaved"
    />

    <!-- 조미료 선택 모달 -->
    <SeasoningModal
      v-if="showSeasoning"
      @close="showSeasoning = false"
      @saved="onSeasoningSaved"
    />
  </section>
</template>

<style scoped>
.head { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 20px; }
.h { margin: 0; font-size: 26px; }
.actions { display: flex; gap: 8px; flex: 0 0 auto; flex-wrap: wrap; justify-content: flex-end; }
.add-btn { flex: 0 0 auto; border: none; background: var(--primary); color: var(--on-primary); border-radius: 8px;
  padding: 10px 16px; font-size: 14px; font-weight: 700; cursor: pointer; }
.add-btn.alt { background: var(--primary-tint); color: var(--primary-deep); }
.summary { list-style: none; display: flex; gap: 8px; padding: 0; margin: 0 0 18px; }
.summary li { flex: 1; background: #fff; border: 1px solid var(--line); box-shadow: var(--shadow-card); border-radius: 10px; padding: 10px; text-align: center; font-size: 12px; color: #666; }
.summary li span { display: block; font-size: 18px; font-weight: 800; color: var(--primary-deep); }
.summary li.warn span { color: #f59e0b; }

.seasoning-row { margin: 0 0 18px; }
.sr-label { display: block; font-size: 17px; font-weight: 700; color: var(--primary-deep); margin-bottom: 6px; }
.sr-groups { display: flex; flex-direction: column; gap: 6px; }
.sr-group { display: flex; align-items: center; gap: 6px; }
.sr-chips { display: flex; flex-wrap: wrap; gap: 6px; flex: 1; min-width: 0; }
.sr-gl { font-size: 12px; font-weight: 700; color: #888; background: #f1f3f5; border-radius: 6px; padding: 2px 8px; min-width: 34px; text-align: center; }
/* 냉장/냉동/실온 라벨 — 동일 디자인, 각 색을 12% 틴트로 연하게 채움(냉장 기준 통일) */
.sr-gl-FRIDGE { color: var(--primary-deep); background: var(--primary-tint); }
.sr-gl-FREEZER { color: #2563eb; background: rgba(37, 99, 235, 0.12); }
.sr-gl-ROOM_TEMP { color: #b45309; background: rgba(180, 83, 9, 0.12); }
/* 조미료 칩 — 연한 회색 채움 알약형(테두리 없음), 흰 카드 위에서 또렷하게 */
.chip { background: #e5e7eb; color: #374151; border-radius: 999px; padding: 5px 11px; font-size: 12px; font-weight: 600; }
.sr-empty { font-size: 12px; color: #aaa; }

.filters { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-bottom: 16px; }
.tabs { display: flex; gap: 6px; }
.tabs button { padding: 7px 12px; border: 1px solid var(--line); background: #fff; border-radius: 999px; font-size: 13px; color: #666; cursor: pointer; }
.tabs button.on { border-color: var(--primary-deep); background: var(--primary-tint); color: var(--primary-deep); font-weight: 700; }
.sort { padding: 7px 10px; border: 1px solid var(--line); border-radius: 8px; font-size: 13px; background: #fff; }

.table-wrap { background: #fff; border: 1px solid var(--line); box-shadow: var(--shadow-card); border-radius: 14px; overflow: hidden; }
.table { width: 100%; border-collapse: collapse; }
.table th { text-align: left; font-size: 12px; color: #999; font-weight: 600; padding: 14px 18px; background: #fafbfc; border-bottom: 1px solid var(--line); }
.table td { padding: 14px 18px; border-bottom: 1px solid var(--line); font-size: 14px; color: #333; vertical-align: middle; }
.table tbody tr:last-child td { border-bottom: none; }
.table tbody tr:hover { background: #fafbfc; }
.table .name { font-weight: 600; }
.table .memo { color: #aaa; font-weight: 400; font-size: 12px; margin-left: 6px; }
.table .man { text-align: center; white-space: nowrap; }
.dday { display: inline-block; min-width: 48px; text-align: center; font-size: 12px; font-weight: 800; padding: 5px 8px; border-radius: 8px; }
.dday.ok { background: var(--primary-tint); color: var(--primary-deep); }
.dday.soon { background: #fff7ed; color: #f59e0b; }
.dday.expired { background: #fef2f2; color: #ef4444; }
.ic { border: none; background: none; cursor: pointer; padding: 4px; color: #8b95a1; display: inline-flex; align-items: center; }
.ic:hover { color: var(--text, #23272e); }

.muted { color: #999; }
.empty { text-align: center; padding: 40px 0; }
.err { color: #e11d48; }
</style>
