<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { fetchRecipeDetail } from '../api/recipe'
import { addRecipeWish, removeRecipeWish } from '../api/wishlist'
import ReviewSection from '../components/ReviewSection.vue'

const props = defineProps({ recipeId: { type: [String, Number], required: true } })
const router = useRouter()

const recipe = ref(null)
const loading = ref(false)
const error = ref('')

const hasNutrition = computed(() => {
  const n = recipe.value?.nutrition
  return n && (n.calories != null || n.carbs != null || n.protein != null || n.fat != null || n.sodium != null)
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
    if (prev) await removeRecipeWish(recipe.value.recipeId)
    else await addRecipeWish(recipe.value.recipeId)
  } catch (e) {
    recipe.value.isWishlisted = prev
    if (e.response?.status !== 409) alert(e.response?.data?.message || '찜 처리 실패')
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
      <!-- 히어로 -->
      <div class="hero" :style="recipe.thumbnailUrl ? { backgroundImage: `url(${recipe.thumbnailUrl})` } : null">
        <span v-if="!recipe.thumbnailUrl" class="ph">🍽️</span>
      </div>

      <div class="head">
        <h2 class="title">{{ recipe.title }}</h2>
        <button class="heart" :class="{ on: recipe.isWishlisted }" @click="toggleWish">
          {{ recipe.isWishlisted ? '♥' : '♡' }}
        </button>
      </div>
      <div class="meta">
        <span v-if="recipe.cookTime">⏱ {{ recipe.cookTime }}분</span>
        <span>⭐ {{ Number(recipe.avgRating).toFixed(1) }} ({{ recipe.reviewCount }})</span>
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

      <!-- 재료 -->
      <h3 class="sec">재료</h3>
      <ul v-if="recipe.ingredients?.length" class="ings">
        <li v-for="(ing, i) in recipe.ingredients" :key="i">
          <span>{{ ing.name }}</span><span class="qty">{{ ing.qty }}</span>
        </li>
      </ul>
      <p v-else class="muted">등록된 재료 정보가 없습니다.</p>

      <!-- 조리 단계 -->
      <h3 class="sec">조리 순서</h3>
      <ol v-if="recipe.steps?.length" class="steps">
        <li v-for="s in recipe.steps" :key="s.stepNumber">
          <div class="n">{{ s.stepNumber }}</div>
          <div class="desc">{{ s.description }}</div>
        </li>
      </ol>
      <p v-else class="muted">등록된 조리 순서가 없습니다.</p>

      <!-- 리뷰 -->
      <ReviewSection :recipe-id="recipe.recipeId" @changed="reloadStats" />
    </template>
  </section>
</template>

<style scoped>
.back { border: none; background: none; color: #16a34a; font-size: 14px; cursor: pointer; padding: 0 0 8px; }
.hero { width: 100%; height: 180px; border-radius: 14px; background: #f1f3f5 center/cover no-repeat; display: flex; align-items: center; justify-content: center; }
.hero .ph { font-size: 48px; }
.head { display: flex; align-items: flex-start; justify-content: space-between; gap: 10px; margin-top: 14px; }
.title { font-size: 20px; margin: 0; }
.heart { border: none; background: none; font-size: 26px; color: #ccc; cursor: pointer; }
.heart.on { color: #ef4444; }
.meta { display: flex; gap: 12px; font-size: 13px; color: #888; margin-top: 6px; }
.summary { font-size: 14px; color: #555; margin: 10px 0 0; }

.nutri { display: flex; gap: 8px; margin: 16px 0; }
.nutri div { flex: 1; background: #fff; border: 1px solid #eee; border-radius: 10px; padding: 10px 4px; text-align: center; font-size: 11px; color: #888; }
.nutri div b { display: block; font-size: 15px; color: #333; }

.sec { font-size: 16px; margin: 22px 0 10px; }
.ings { list-style: none; padding: 0; margin: 0; }
.ings li { display: flex; justify-content: space-between; padding: 9px 12px; background: #fff; border: 1px solid #eee; border-radius: 8px; margin-bottom: 6px; font-size: 14px; }
.ings .qty { color: #888; }

.steps { list-style: none; padding: 0; margin: 0; counter-reset: step; }
.steps li { display: flex; gap: 12px; margin-bottom: 12px; }
.steps .n { flex: 0 0 26px; height: 26px; border-radius: 50%; background: #ecfdf3; color: #16a34a; font-weight: 700; display: flex; align-items: center; justify-content: center; font-size: 13px; }
.steps .desc { flex: 1; font-size: 14px; padding-top: 2px; }

.muted { color: #999; }
.err { color: #e11d48; }
</style>
