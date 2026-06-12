<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listMyRecipes, publishRecipe } from '../api/recipe'
import { useToast } from '../composables/useToast'

const props = defineProps({
  // 공개 후보(비공개)만 보여줄지 여부 — 공개 등록 화면(P4)에서 true
  onlyPrivate: { type: Boolean, default: false },
})

const router = useRouter()
const toast = useToast()

const items = ref([])
const loading = ref(true)
const publishing = ref(null)

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
  if (publishing.value) return
  publishing.value = r.recipeId
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
    publishing.value = null
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
        <span class="badge">🤖 AI</span>
        <div class="info">
          <div class="title">{{ r.title }}</div>
          <div class="sub"><span v-if="r.cookTime">⏱ {{ r.cookTime }}분</span></div>
        </div>
      </div>
      <div class="right">
        <span v-if="r.isPublic" class="state public">공개됨</span>
        <template v-else>
          <span class="state private">비공개</span>
          <button class="pub" :disabled="publishing === r.recipeId" @click="publish(r)">
            {{ publishing === r.recipeId ? '공개 중…' : '공개하기' }}
          </button>
        </template>
      </div>
    </li>
  </ul>
</template>

<style scoped>
.list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 8px; }
.row { display: flex; align-items: center; justify-content: space-between; gap: 12px;
  background: #fff; border: 1px solid #eee; border-radius: 12px; padding: 12px 14px; }
.left { display: flex; align-items: center; gap: 12px; min-width: 0; cursor: pointer; flex: 1; }
.badge { flex: 0 0 auto; font-size: 12px; font-weight: 700; color: #7c3aed; background: #f5f3ff;
  border-radius: 999px; padding: 4px 10px; }
.info { min-width: 0; }
.title { font-size: 15px; font-weight: 600; }
.sub { font-size: 12px; color: #999; margin-top: 2px; }
.right { display: flex; align-items: center; gap: 10px; flex: 0 0 auto; }
.state { font-size: 12px; font-weight: 700; border-radius: 999px; padding: 4px 10px; }
.state.public { color: #16a34a; background: #ecfdf3; }
.state.private { color: #6b7280; background: #f3f4f6; }
.pub { border: none; background: #16a34a; color: #fff; font-size: 13px; font-weight: 700;
  border-radius: 999px; padding: 8px 16px; cursor: pointer; }
.pub:disabled { opacity: .6; }
.muted { color: #999; }
.empty { font-size: 14px; }
</style>
