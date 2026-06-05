import http from './http'

/** 일반 레시피 찜 등록 → { wishlisted: true } (중복 409) */
export async function addRecipeWish(recipeId) {
  const { data } = await http.post(`/api/wishlist/${recipeId}`)
  return data
}

/** 일반 레시피 찜 해제 → { wishlisted: false } */
export async function removeRecipeWish(recipeId) {
  const { data } = await http.delete(`/api/wishlist/${recipeId}`)
  return data
}

/** 내 찜 목록(일반+AI) — { content, page, size, totalElements, totalPages } */
export async function listWishlist(params = {}) {
  const { data } = await http.get('/api/wishlist/me', { params })
  return data
}

/** AI 레시피 찜 저장 → { aiRecipeId } */
export async function saveAiRecipe(payload) {
  const { data } = await http.post('/api/wishlist/ai', payload)
  return data
}

/** AI 레시피 찜 해제 → { wishlisted: false } */
export async function removeAiWish(aiRecipeId) {
  const { data } = await http.delete(`/api/wishlist/ai/${aiRecipeId}`)
  return data
}

/** 저장한 AI 레시피 단건 상세(재료·단계 전체) — 본인만(403/404) */
export async function fetchAiRecipe(aiRecipeId) {
  const { data } = await http.get(`/api/wishlist/ai/${aiRecipeId}`)
  return data
}
