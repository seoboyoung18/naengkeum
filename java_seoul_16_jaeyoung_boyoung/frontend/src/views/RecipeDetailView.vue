<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { fetchRecipeDetail, uploadRecipeImage } from '../api/recipe'
import { API_BASE } from '../api/http'
import { addRecipeWish, removeRecipeWish } from '../api/wishlist'
import ReviewSection from '../components/ReviewSection.vue'
import { useToast } from '../composables/useToast'
import InlineIcon from '../components/InlineIcon.vue'
import clockSvg from '../assets/icons/clock-outline.svg?raw'
import starSvg from '../assets/icons/star.svg?raw'
import imageSvg from '../assets/icons/image.svg?raw'

const toast = useToast()

const props = defineProps({ recipeId: { type: [String, Number], required: true } })
const router = useRouter()

const recipe = ref(null)
const loading = ref(false)
const error = ref('')

// 사진 업로드용
const fileInput = ref(null)
const uploading = ref(false)

const hasNutrition = computed(() => {
  const n = recipe.value?.nutrition
  return n && (n.calories != null || n.carbs != null || n.protein != null || n.fat != null || n.sodium != null)
})

// 이미지가 백엔드 업로드 경로(/images/...)면 백엔드 호스트를 붙여 절대경로로.
// 외부 URL(http로 시작)이면 그대로 사용.
const heroUrl = computed(() => {
  const u = recipe.value?.thumbnailUrl
  if (!u) return null
  return u.startsWith('/') ? API_BASE + u : u
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    recipe.value = await fetchRecipeDetail(props.recipeId)
  } catch (e) {
    error.value = e.response?.status === 404 ? '레시피를 찾을 수 없습니다' : (e.response?.data?.message || '불러오기 실패')
  } finally {
    loading.value = false
  }
}

// 리뷰 변경 시 평점/리뷰수만 가볍게 갱신
async function reloadStats() {
  try {
    const fresh = await fetchRecipeDetail(props.recipeId)
    if (recipe.value) {
      recipe.value.avgRating = fresh.avgRating
      recipe.value.reviewCount = fresh.reviewCount
    }
  } catch (_) {}
}

async function toggleWish() {
  const prev = recipe.value.isWishlisted
  recipe.value.isWishlisted = !prev
  try {
    if (prev) { await removeRecipeWish(recipe.value.recipeId); toast.info('찜을 해제했어요') }
    else { await addRecipeWish(recipe.value.recipeId); toast.success('찜에 추가했어요') }
  } catch (e) {
    recipe.value.isWishlisted = prev
    if (e.response?.status !== 409) toast.error(e.response?.data?.message || '찜 처리에 실패했어요')
  }
}

// 파일 선택창 열기
function pickImage() {
  fileInput.value?.click()
}

// 사진 선택 → 업로드 → hero 이미지 즉시 갱신
async function onImagePicked(e) {
  const file = e.target.files?.[0]
  e.target.value = '' // 같은 파일 다시 선택 가능하게 초기화
  if (!file) return

  // 가벼운 클라이언트 검증 (서버도 검증하지만 UX용)
  if (!/^image\/(jpeg|png|webp)$/.test(file.type)) {
    toast.error('jpg, png, webp 이미지만 올릴 수 있어요')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    toast.error('이미지는 5MB 이하만 올릴 수 있어요')
    return
  }

  uploading.value = true
  try {
    const { imageUrl } = await uploadRecipeImage(recipe.value.recipeId, file)
    // 백엔드가 준 경로는 /images/... → 절대경로로 만들어 hero에 반영
    recipe.value.thumbnailUrl = imageUrl
    toast.success('사진을 올렸어요')
  } catch (err) {
    toast.error(err.response?.data?.message || '사진 업로드에 실패했어요')
  } finally {
    uploading.value = false
  }
}

onMounted(load)
</script>

<template>
  <section>
    <button class="back" @click="router.back()">← 뒤로</button>

    <p v-if="loading" class="muted">불러오는 중…</p>
    <p v-else-if="error" class="err">{{ error }}</p>

    <template v-else-if="recipe">
      <!-- 상단: 이미지(좌) + 정보(우) -->
      <div class="top">
        <div class="hero" :style="heroUrl ? { backgroundImage: `url(${heroUrl})` } : null">
          <InlineIcon v-if="!heroUrl" :svg="imageSvg" :size="64" class="ph" />

          <!-- 본인이 등록한 레시피에만 사진 올리기 버튼 -->
          <button
            v-if="recipe.isOwner"
            class="upload-btn"
            :disabled="uploading"
            @click="pickImage"
          >
            {{ uploading ? '올리는 중…' : (heroUrl ? '📷 사진 변경' : '📷 사진 올리기') }}
          </button>
          <input
            ref="fileInput"
            type="file"
            accept="image/jpeg,image/png,image/webp"
            class="file-hidden"
            @change="onImagePicked"
          />
        </div>

        <div class="info">
          <div class="title-row">
            <h2 class="title">{{ recipe.title }}</h2>
          </div>
          <div class="meta">
            <span v-if="recipe.cookTime"><InlineIcon :svg="clockSvg" :size="12" /> {{ recipe.cookTime }}분</span>
            <span><InlineIcon :svg="starSvg" :size="13" style="transform: translateY(-2px)" /> {{ Number(recipe.avgRating).toFixed(1) }} (리뷰 {{ recipe.reviewCount }})</span>
          </div>
          <p v-if="recipe.summary" class="summary">{{ recipe.summary }}</p>

          <!-- 영양정보 -->
          <div v-if="hasNutrition" class="nutri">
            <div v-if="recipe.nutrition.calories != null"><b>{{ recipe.nutrition.calories }}</b>kcal</div>
            <div v-if="recipe.nutrition.carbs != null"><b>{{ recipe.nutrition.carbs }}</b>탄수</div>
            <div v-if="recipe.nutrition.protein != null"><b>{{ recipe.nutrition.protein }}</b>단백</div>
            <div v-if="recipe.nutrition.fat != null"><b>{{ recipe.nutrition.fat }}</b>지방</div>
            <div v-if="recipe.nutrition.sodium != null"><b>{{ recipe.nutrition.sodium }}</b>나트륨</div>
          </div>

          <button class="wish-btn" :class="{ on: recipe.isWishlisted }" @click="toggleWish">
            {{ recipe.isWishlisted ? '♥ 찜 해제' : '♡ 찜하기' }}
          </button>
        </div>
      </div>

      <!-- 하단: 재료·조리순서(좌) + 리뷰(우) -->
      <div class="cols">
        <div class="main-col">
          <div class="card">
            <h3 class="sec">재료</h3>
            <ul v-if="recipe.ingredients?.length" class="ings">
              <li v-for="(ing, i) in recipe.ingredients" :key="i">
                <span>{{ ing.name }}</span><span class="qty">{{ ing.qty }}</span>
              </li>
            </ul>
            <p v-else class="muted">등록된 재료 정보가 없습니다.</p>
          </div>

          <div class="card">
            <h3 class="sec">조리 순서</h3>
            <ol v-if="recipe.steps?.length" class="steps">
              <li v-for="s in recipe.steps" :key="s.stepNumber">
                <div class="n">{{ s.stepNumber }}</div>
                <div class="desc">{{ s.description }}</div>
              </li>
            </ol>
            <p v-else class="muted">등록된 조리 순서가 없습니다.</p>
          </div>
        </div>

        <div class="side-col">
          <div class="card">
            <ReviewSection :recipe-id="recipe.recipeId" @changed="reloadStats" />
          </div>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.back { border: none; background: none; color: var(--primary-deep); font-size: 14px; cursor: pointer; padding: 0 0 14px; font-weight: 600; }

/* 상단 2단 */
.top { display: grid; grid-template-columns: 1fr 1fr; gap: 28px; align-items: start; }
.hero { position: relative; width: 100%; height: 340px; border-radius: var(--r-md); background: #f1f3f5 center/cover no-repeat; display: flex; align-items: center; justify-content: center; border: 1px solid var(--line); }
.hero .ph { font-size: 56px; color: #c7ccd1; }
.upload-btn { position: absolute; right: 12px; bottom: 12px; border: none; background: rgba(0,0,0,.6);
  color: #fff; font-size: 13px; font-weight: 600; border-radius: 999px; padding: 9px 16px; cursor: pointer;
  backdrop-filter: blur(2px); }
.upload-btn:hover { background: rgba(0,0,0,.75); }
.upload-btn:disabled { opacity: .6; cursor: default; }
.file-hidden { display: none; }
.info { min-width: 0; }
.title-row { display: flex; align-items: flex-start; gap: 10px; }
.title { font-size: 26px; margin: 0; letter-spacing: -0.5px; }
.meta { display: flex; gap: 14px; font-size: 14px; color: var(--text-soft); margin-top: 10px; font-family: var(--font-mono); }
.summary { font-size: 15px; color: #555; margin: 12px 0 0; line-height: 1.6; }

/* 영양 5칸 — 하어라인 카드 + 모노 수치 (Voltagent 톤) */
.nutri { display: flex; gap: 10px; margin: 20px 0; }
.nutri div { flex: 1; background: var(--surface); border: 1px solid var(--line); border-radius: var(--r-md); padding: 14px 4px; text-align: center; font-size: 11px; color: var(--text-soft); box-shadow: var(--shadow-card); }
.nutri div b { display: block; font-family: var(--font-mono); font-size: 18px; font-weight: 600; color: var(--text); margin-bottom: 3px; }

/* CTA — 일렉트릭 그린 + near-black 글자, 6px */
.wish-btn { border: none; background: var(--primary); color: var(--on-primary); font-size: 15px; font-weight: 700;
  border-radius: var(--r-sm); padding: 13px 28px; cursor: pointer; }
.wish-btn.on { background: var(--surface); color: var(--primary-deep); border: 1px solid var(--primary); }

/* 하단 2단 */
.cols { display: grid; grid-template-columns: 1fr 380px; gap: 24px; align-items: start; margin-top: 24px; }
.main-col { display: flex; flex-direction: column; gap: 24px; min-width: 0; }
.card { background: var(--surface); border: 1px solid var(--line); border-radius: var(--r-md); padding: 20px 22px; box-shadow: var(--shadow-card); }

.sec { font-size: 16px; margin: 0 0 14px; }
.ings { list-style: none; padding: 0; margin: 0; }
.ings li { display: flex; justify-content: space-between; padding: 11px 12px; border: 1px solid var(--line); border-radius: var(--r-sm); margin-bottom: 8px; font-size: 14px; }
.ings li:last-child { margin-bottom: 0; }
.ings .qty { color: var(--text-soft); font-family: var(--font-mono); }

.steps { list-style: none; padding: 0; margin: 0; }
.steps li { display: flex; gap: 12px; margin-bottom: 14px; }
.steps li:last-child { margin-bottom: 0; }
.steps .n { flex: 0 0 26px; height: 26px; border-radius: 50%; background: rgba(0,217,146,.14); color: var(--primary-deep); font-weight: 700; display: flex; align-items: center; justify-content: center; font-size: 13px; font-family: var(--font-mono); }
.steps .desc { flex: 1; font-size: 14px; padding-top: 2px; line-height: 1.5; }

.muted { color: #999; }
.err { color: #e11d48; }

@media (max-width: 920px) {
  .top { grid-template-columns: 1fr; }
  .hero { height: 240px; }
  .cols { grid-template-columns: 1fr; }
}
</style>