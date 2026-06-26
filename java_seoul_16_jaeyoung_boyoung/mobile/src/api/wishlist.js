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

/** 내 찜 목록(일반+AI) — PageResponse */
export async function listWishlist(params = {}) {
  const { data } = await http.get('/api/wishlist/me', { params })
  return data
}
