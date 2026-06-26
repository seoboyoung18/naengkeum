import http from './http'

/** 레시피 검색/목록 — PageResponse */
export async function searchRecipes(params = {}) {
  const { data } = await http.get('/api/recipe', { params })
  return data
}

/** 제목 자동완성 */
export async function autocompleteRecipes(keyword) {
  const { data } = await http.get('/api/recipe/autocomplete', { params: { keyword } })
  return data
}

/** 인기 레시피 */
export async function popularRecipes(limit = 8) {
  const { data } = await http.get('/api/recipe/popular', { params: { limit } })
  return data
}

/** 레시피 상세 */
export async function fetchRecipeDetail(recipeId) {
  const { data } = await http.get(`/api/recipe/${recipeId}`)
  return data
}

/** AI 결과를 마이 레시피(비공개)로 담기 — { title, summary?, ingredientsJson, stepsJson, cookTime?, note? } */
export async function registerFromAi(payload) {
  const { data } = await http.post('/api/recipe/from-ai', payload)
  return data
}

/** 마이 레시피 목록(공개/비공개) */
export async function listMyRecipes() {
  const { data } = await http.get('/api/recipe/mine')
  return data
}

/** 공개하기 (사진 없으면 400) → { recipeId, isPublic, consumed } */
export async function publishRecipe(recipeId) {
  const { data } = await http.patch(`/api/recipe/${recipeId}/publish`)
  return data
}

/** 비공개 전환 */
export async function unpublishRecipe(recipeId) {
  const { data } = await http.patch(`/api/recipe/${recipeId}/unpublish`)
  return data
}

/** 레시피 삭제(본인/관리자) */
export async function deleteRecipe(recipeId) {
  const { data } = await http.delete(`/api/recipe/${recipeId}`)
  return data
}

/** 내 후기 작성/수정(본인 레시피) — 빈 값이면 삭제 → { authorReview } */
export async function updateRecipeReview(recipeId, review) {
  const { data } = await http.patch(`/api/recipe/${recipeId}/review`, { review })
  return data
}

/**
 * 레시피 대표 사진 업로드(본인) — RN: file = { uri, name, type }
 * @returns {Promise<{ imageUrl: string }>}
 */
export async function uploadRecipeImage(recipeId, file) {
  const form = new FormData()
  form.append('image', { uri: file.uri, name: file.name || 'photo.jpg', type: file.type || 'image/jpeg' })
  const { data } = await http.post(`/api/recipe/${recipeId}/image`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data
}
