import http from './http'

/**
 * 관리자(Admin) API — 전부 ROLE_ADMIN 전용.
 * 인수인계 문서(6/16 최종 계약) 기준.
 */

/** 대시보드 통계 — { totalMembers, totalRecipes, totalReviews, activeChallengeParticipants, pendingReports } */
export async function getStats() {
  const { data } = await http.get('/api/admin/stats')
  return data
}

/** 회원 목록 — ?keyword= 닉네임/이메일 부분일치(선택). AdminUserRow[] */
export async function getUsers(keyword) {
  const { data } = await http.get('/api/admin/users', { params: keyword ? { keyword } : {} })
  return data
}

/** 회원 차단/해제 — active=false 차단 / true 해제. (ADMIN 차단 시 400) */
export async function setUserActive(memberId, active) {
  const { data } = await http.patch(`/api/admin/users/${memberId}/active`, { active })
  return data
}

/** 레시피 목록(사용자 등록분, 미처리 신고 누적순) — ?keyword= 제목 부분일치(선택). AdminRecipeRow[] */
export async function getRecipes(keyword) {
  const { data } = await http.get('/api/admin/recipes', { params: keyword ? { keyword } : {} })
  return data
}

/** 레시피 삭제(본인/관리자) — 자식 CASCADE. */
export async function deleteRecipe(recipeId) {
  const { data } = await http.delete(`/api/recipe/${recipeId}`)
  return data
}

/** 리뷰 목록(미처리 신고 누적순) — AdminReviewRow[] */
export async function getReviews() {
  const { data } = await http.get('/api/admin/reviews')
  return data
}

/** 리뷰 삭제(관리자는 타인 리뷰도 가능). */
export async function deleteReview(reviewId) {
  const { data } = await http.delete(`/api/review/${reviewId}`)
  return data
}

/** 신고 목록(PENDING, 대상별 묶음, 신고 많은 순) — AdminReportRow[] */
export async function getReports() {
  const { data } = await http.get('/api/admin/reports')
  return data
}

/** 레시피 신고 무시(처리완료) — 목록에서 사라짐. */
export async function resolveRecipeReports(recipeId) {
  const { data } = await http.patch(`/api/admin/reports/recipe/${recipeId}/resolve`)
  return data
}

/** 리뷰 신고 무시(처리완료) — 목록에서 사라짐. */
export async function resolveReviewReports(reviewId) {
  const { data } = await http.patch(`/api/admin/reports/review/${reviewId}/resolve`)
  return data
}
