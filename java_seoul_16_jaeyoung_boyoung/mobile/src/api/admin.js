import http from './http'

/** 대시보드 통계 — { totalMembers, totalRecipes, totalReviews, activeChallengeParticipants, pendingReports } */
export async function getStats() {
  const { data } = await http.get('/api/admin/stats')
  return data
}

/** 회원 목록 — ?keyword= (선택) */
export async function getUsers(keyword) {
  const { data } = await http.get('/api/admin/users', { params: keyword ? { keyword } : {} })
  return data
}

/** 회원 역할 변경 — role: 'USER' | 'ADMIN' */
export async function setUserRole(memberId, role) {
  const { data } = await http.patch(`/api/admin/users/${memberId}/role`, { role })
  return data
}

/** 회원 삭제(hard) */
export async function deleteUser(memberId) {
  const { data } = await http.delete(`/api/admin/users/${memberId}`)
  return data
}

/** 레시피 목록(사용자 등록분, 신고 누적순) — ?keyword= (선택) */
export async function getRecipes(keyword) {
  const { data } = await http.get('/api/admin/recipes', { params: keyword ? { keyword } : {} })
  return data
}

/** 레시피 삭제 */
export async function deleteAdminRecipe(recipeId) {
  const { data } = await http.delete(`/api/recipe/${recipeId}`)
  return data
}

/** 리뷰 목록(신고 누적순) */
export async function getReviews() {
  const { data } = await http.get('/api/admin/reviews')
  return data
}

/** 리뷰 삭제(관리자) */
export async function deleteAdminReview(reviewId) {
  const { data } = await http.delete(`/api/review/${reviewId}`)
  return data
}

/** 신고 목록(PENDING, 신고 많은 순) */
export async function getReports() {
  const { data } = await http.get('/api/admin/reports')
  return data
}

/** 레시피 신고 무시(처리완료) */
export async function resolveRecipeReports(recipeId) {
  const { data } = await http.patch(`/api/admin/reports/recipe/${recipeId}/resolve`)
  return data
}

/** 리뷰 신고 무시(처리완료) */
export async function resolveReviewReports(reviewId) {
  const { data } = await http.patch(`/api/admin/reports/review/${reviewId}/resolve`)
  return data
}
