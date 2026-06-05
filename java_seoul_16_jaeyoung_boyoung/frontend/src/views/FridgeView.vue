<script setup>
import { ref, computed, onMounted } from 'vue'
import { listFridge, deleteFridgeItem } from '../api/fridge'
import FridgeItemForm from '../components/FridgeItemForm.vue'
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

onMounted(load)
</script>

<template>
  <section>
    <h2 class="h">내 냉장고</h2>

    <!-- 요약 -->
    <ul class="summary">
      <li><span>{{ summary.fridgeCount }}</span>냉장</li>
      <li><span>{{ summary.freezerCount }}</span>냉동</li>
      <li><span>{{ summary.roomTempCount }}</span>실온</li>
      <li class="warn"><span>{{ imminentCount }}</span>임박</li>
    </ul>

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
    <p v-else-if="items.length === 0" class="muted empty">재료가 없습니다. 아래 + 버튼으로 추가해 보세요.</p>

    <ul v-else class="list">
      <li v-for="it in items" :key="it.fridgeItemId" class="card">
        <div class="left">
          <span class="dday" :class="dDayClass(it.dDay)">{{ dDayText(it.dDay) }}</span>
        </div>
        <div class="mid">
          <div class="name">{{ it.name }} <span class="qty">{{ it.qty }}{{ it.unit }}</span></div>
          <div class="sub">{{ STORAGE_LABEL[it.storageType] }} · {{ it.expiryDate }}<template v-if="it.memo"> · {{ it.memo }}</template></div>
        </div>
        <div class="right">
          <button class="ic" @click="openEdit(it)">✏️</button>
          <button class="ic" @click="onDelete(it)">🗑️</button>
        </div>
      </li>
    </ul>

    <!-- 추가 FAB -->
    <button class="fab" @click="openAdd">＋</button>

    <!-- 추가/수정 폼 -->
    <FridgeItemForm
      v-if="showForm"
      :item="editing"
      @close="showForm = false"
      @saved="onSaved"
    />
  </section>
</template>

<style scoped>
.summary { list-style: none; display: flex; gap: 8px; padding: 0; margin: 0 0 14px; }
.summary li { flex: 1; background: #fff; border: 1px solid #eee; border-radius: 10px; padding: 10px; text-align: center; font-size: 12px; color: #666; }
.summary li span { display: block; font-size: 18px; font-weight: 800; color: #16a34a; }
.summary li.warn span { color: #f59e0b; }

.filters { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-bottom: 12px; }
.tabs { display: flex; gap: 6px; }
.tabs button { padding: 7px 12px; border: 1px solid #ddd; background: #fff; border-radius: 999px; font-size: 13px; color: #666; cursor: pointer; }
.tabs button.on { border-color: #16a34a; background: #ecfdf3; color: #16a34a; font-weight: 700; }
.sort { padding: 7px 10px; border: 1px solid #ddd; border-radius: 8px; font-size: 13px; background: #fff; }

.list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 8px; }
.card { display: flex; align-items: center; gap: 12px; background: #fff; border: 1px solid #eee; border-radius: 12px; padding: 12px; }
.left { flex: 0 0 auto; }
.dday { display: inline-block; min-width: 48px; text-align: center; font-size: 12px; font-weight: 800; padding: 6px 8px; border-radius: 8px; }
.dday.ok { background: #ecfdf3; color: #16a34a; }
.dday.soon { background: #fff7ed; color: #f59e0b; }
.dday.expired { background: #fef2f2; color: #ef4444; }
.mid { flex: 1; min-width: 0; }
.name { font-size: 15px; font-weight: 600; }
.name .qty { font-size: 13px; color: #888; font-weight: 400; margin-left: 4px; }
.sub { font-size: 12px; color: #999; margin-top: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.right { display: flex; gap: 2px; }
.ic { border: none; background: none; font-size: 16px; cursor: pointer; padding: 4px; }

.muted { color: #999; }
.empty { text-align: center; padding: 40px 0; }
.err { color: #e11d48; }

.fab { position: fixed; bottom: 80px; right: calc(50% - 240px + 16px); width: 52px; height: 52px; border-radius: 50%; border: none; background: #16a34a; color: #fff; font-size: 26px; cursor: pointer; box-shadow: 0 6px 16px rgba(22,163,74,.4); }
@media (max-width: 520px) { .fab { right: 16px; } }
</style>
