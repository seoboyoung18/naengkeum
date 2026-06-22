<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listMyRecipes, publishRecipe, unpublishRecipe, deleteRecipe } from '../api/recipe'
import { useToast } from '../composables/useToast'
import InlineIcon from './InlineIcon.vue'
import clockSvg from '../assets/icons/clock-outline.svg?raw'
import trashSvg from '../assets/icons/trash.svg?raw'
import botUrl from '../assets/icons/message-bot.svg'

const props = defineProps({
  // 공개 후보(비공개)만 보여줄지 여부 — 공개 등록 화면(P4)에서 true
  onlyPrivate: { type: Boolean, default: false },
})

const router = useRouter()
const toast = useToast()

const items = ref([])
const loading = ref(true)
const busy = ref(null) // 처리 중인 recipeId (공개/비공개/삭제 공통)

async function load() {
  loading.value = true
  try {
    const all = await listMyRecipes()
    items.value = props.onlyPrivate ? all.filter((r) => !r.isPublic) : all
  } finally {
    loading.value = false
  }
}

async function publish(r) {
  if (busy.value) return
  // 사진 없으면 공개 불가 — 먼저 상세 화면으로 안내해 사진을 등록하게 한다.
  if (!r.imageUrl) {
    toast.info('대표 사진을 먼저 등록해야 공개할 수 있어요')
    openDetail(r)
    return
  }
  busy.value = r.recipeId
  try {
    await publishRecipe(r.recipeId)
    if (props.onlyPrivate) {
      items.value = items.value.filter((x) => x.recipeId !== r.recipeId)
    } else {
      r.isPublic = true
    }
    toast.success('공개했어요. 이제 모두가 검색·찜할 수 있어요')
  } catch (e) {
    toast.error(e.response?.data?.message || '공개에 실패했어요')
  } finally {
    busy.value = null
  }
}

async function unpublish(r) {
  if (busy.value) return
  busy.value = r.recipeId
  try {
    await unpublishRecipe(r.recipeId)
    r.isPublic = false
    toast.success('비공개로 전환했어요')
  } catch (e) {
    toast.error(e.response?.data?.message || '비공개 전환에 실패했어요')
  } finally {
    busy.value = null
  }
}

async function removeRecipe(r) {
  if (busy.value) return
  if (!confirm(`'${r.title}' 레시피를 삭제할까요? 되돌릴 수 없어요.`)) return
  busy.value = r.recipeId
  try {
    await deleteRecipe(r.recipeId)
    items.value = items.value.filter((x) => x.recipeId !== r.recipeId)
    toast.success('레시피를 삭제했어요')
  } catch (e) {
    toast.error(e.response?.data?.message || '삭제에 실패했어요')
  } finally {
    busy.value = null
  }
}

function openDetail(r) {
  router.push({ name: 'recipe-detail', params: { recipeId: r.recipeId } })
}

onMounted(load)
defineExpose({ load })
</script>

<template>
  <p v-if="loading" class="muted">불러오는 중…</p>
  <p v-else-if="items.length === 0" class="muted empty">
    {{ onlyPrivate
      ? '공개할 레시피가 없어요. AI 추천에서 "내 레시피로 담기"로 먼저 담아보세요.'
      : '아직 담은 레시피가 없어요. AI 추천에서 "내 레시피로 담기"를 눌러보세요.' }}
  </p>

  <ul v-else class="list">
    <li v-for="r in items" :key="r.recipeId" class="row">
      <div class="left" @click="openDetail(r)">
        <span class="badge"><img :src="botUrl" class="bi" alt="" /> AI</span>
        <div class="info">
          <div class="title">{{ r.title }}</div>
          <div class="sub">
            <span v-if="r.cookTime"><InlineIcon :svg="clockSvg" :size="12" /> {{ r.cookTime }}분</span>
            <span v-if="!r.imageUrl" class="nophoto">📷 사진 없음</span>
          </div>
        </div>
      </div>
      <div class="right">
        <template v-if="r.isPublic">
          <span class="state public">공개됨</span>
          <button class="ghost" :disabled="busy === r.recipeId" @click="unpublish(r)">
            {{ busy === r.recipeId ? '처리 중…' : '비공개로' }}
          </button>
        </template>
        <template v-else>
          <span class="state private">비공개</span>
          <button class="pub" :disabled="busy === r.recipeId" @click="publish(r)">
            {{ busy === r.recipeId ? '공개 중…' : (r.imageUrl ? '공개하기' : '사진 등록') }}
          </button>
        </template>
        <button class="ic" :disabled="busy === r.recipeId" title="삭제" @click="removeRecipe(r)"><InlineIcon :svg="trashSvg" :size="16" /></button>
      </div>
    </li>
  </ul>
</template>

<style scoped>
.list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 8px; }
.row { display: flex; align-items: center; justify-content: space-between; gap: 12px;
  background: #fff; border: 1px solid var(--line); box-shadow: var(--shadow-card); border-radius: 12px; padding: 12px 14px; }
.left { display: flex; align-items: center; gap: 12px; min-width: 0; cursor: pointer; flex: 1; }
.badge { flex: 0 0 auto; display: inline-flex; align-items: center; gap: 3px; font-size: 12px; font-weight: 700; color: #7c3aed; background: #f5f3ff;
  border-radius: 999px; padding: 4px 10px; }
.bi { width: 14px; height: 14px; object-fit: contain; }
.info { min-width: 0; }
.title { font-size: 15px; font-weight: 600; }
.sub { font-size: 12px; color: #999; margin-top: 2px; display: flex; gap: 10px; align-items: center; }
.nophoto { color: #f59e0b; font-weight: 600; }
.right { display: flex; align-items: center; gap: 8px; flex: 0 0 auto; }
.state { font-size: 12px; font-weight: 700; border-radius: 999px; padding: 4px 10px; }
.state.public { color: var(--primary-deep); background: var(--primary-tint); }
.state.private { color: #6b7280; background: #f3f4f6; }
.pub { border: none; background: var(--primary); color: var(--on-primary); font-size: 13px; font-weight: 700;
  border-radius: 999px; padding: 8px 16px; cursor: pointer; }
.pub:disabled { opacity: .6; }
.ghost { border: 1px solid var(--line); background: #fff; color: #555; font-size: 13px; font-weight: 600;
  border-radius: 999px; padding: 8px 14px; cursor: pointer; }
.ghost:disabled { opacity: .6; }
.ic { border: none; background: none; cursor: pointer; padding: 4px; color: #8b95a1; display: inline-flex; align-items: center; }
.ic:hover { color: var(--text, #23272e); }
.ic:disabled { opacity: .6; cursor: default; }
.muted { color: #999; }
.empty { font-size: 14px; }
</style>
