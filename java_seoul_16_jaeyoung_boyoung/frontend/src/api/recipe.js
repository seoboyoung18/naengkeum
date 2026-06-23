import http from './http'

/**
 * 레시피 검색/목록 — PageResponse
 * params: { keyword, sort(LATEST|POPULAR|RATING|COOK_TIME), ingredients(csv), maxCookTime, page, size }
 * → { content:[RecipeListItem], page, size, totalElements, totalPages }
 */
export async function searchRecipes(params = {}) {
  const { data } = await http.get('/api/recipe', { params })
  return data
}

/** 제목 자동완성 — ?keyword= → [{ recipeId, title }] */
export async function autocompleteRecipes(keyword) {
  const { data } = await http.get('/api/recipe/autocomplete', { params: { keyword } })
  return data
}

/** 인기 레시피 — ?limit= → [RecipeListItem] */
export async function popularRecipes(limit = 8) {
  const { data } = await http.get('/api/recipe/popular', { params: { limit } })
  return data
}

/** 레시피 상세 — { recipeId, title, summary, ingredients, steps, nutrition, avgRating, isWishlisted, ... } */
export async function fetchRecipeDetail(recipeId) {
  const { data } = await http.get(`/api/recipe/${recipeId}`)
  return data
}

/**
 * "내 레시피로 담기" — AI 결과 콘텐츠를 마이 레시피(비공개)로 등록 → { recipeId }
 * payload: { title, summary, note(작성자 소감, 선택), ingredientsJson:[{name,qty,unit}], stepsJson:[{stepNumber,description}], cookTime }
 */
export async function registerFromAi(payload) {
  const { data } = await http.post('/api/recipe/from-ai', payload)
  return data
}

/** 마이 레시피 목록(공개/비공개 포함) → [{ recipeId, title, cookTime, isPublic, imageUrl, source, createdAt }] */
export async function listMyRecipes() {
  const { data } = await http.get('/api/recipe/mine')
  return data
}

/**
 * "공개하기" — 마이 레시피를 공개 카탈로그에 게시. 사진 없으면 400.
 * → { recipeId, isPublic, consumed:[{ name, used, unit, removed }] }
 * consumed는 최초 공개 시 냉장고에서 차감된 재료(정량·이름일치·단위호환분). 재공개 시엔 빈 배열.
 */
export async function publishRecipe(recipeId) {
  const { data } = await http.patch(`/api/recipe/${recipeId}/publish`)
  return data
}

/** "비공개로 전환" — 공개했던 내 레시피를 다시 비공개로 → { recipeId, isPublic } */
export async function unpublishRecipe(recipeId) {
  const { data } = await http.patch(`/api/recipe/${recipeId}/unpublish`)
  return data
}

/** 레시피 삭제 — 본인이 등록한 레시피만(관리자 포함). → { message } */
export async function deleteRecipe(recipeId) {
  const { data } = await http.delete(`/api/recipe/${recipeId}`)
  return data
}

/** "내 후기" 작성/수정 — 본인이 등록한 레시피만. 빈 값이면 후기 삭제 → { authorReview } */
export async function updateRecipeReview(recipeId, review) {
  const { data } = await http.patch(`/api/recipe/${recipeId}/review`, { review })
  return data
}

/**
 * 레시피 대표 사진 업로드 — 본인이 등록한 레시피만 가능.
 * @param {number} recipeId
 * @param {File} file  jpg/png/webp, 5MB 이하
 * @returns {Promise<{imageUrl: string}>}
 */
export async function uploadRecipeImage(recipeId, file) {
  const form = new FormData()
  form.append('image', file)
  const { data } = await http.post(`/api/recipe/${recipeId}/image`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data
}