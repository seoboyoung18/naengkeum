import http from './http'

/** 레시피 리뷰 목록 → { content, totalElements, ratingStats:{avg, dist} } */
export async function listReviews(recipeId, params = {}) {
  const { data } = await http.get('/api/review', { params: { recipeId, ...params } })
  return data
}

/** 리뷰 작성 — { recipeId, rating, content } (중복 409) */
export async function createReview(payload) {
  const { data } = await http.post('/api/review', payload)
  return data
}

/** 리뷰 수정 — { rating, content } (본인만) */
export async function updateReview(reviewId, payload) {
  const { data } = await http.put(`/api/review/${reviewId}`, payload)
  return data
}

/** 리뷰 삭제 */
export async function deleteReview(reviewId) {
  const { data } = await http.delete(`/api/review/${reviewId}`)
  return data
}
