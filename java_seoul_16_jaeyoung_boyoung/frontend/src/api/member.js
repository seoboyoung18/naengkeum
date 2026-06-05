import http from './http'

/** 마이페이지(내 정보 + 통계) — MyPageResponse */
export async function getMyPage() {
  const { data } = await http.get('/api/member/me')
  return data
}

/** 회원정보 수정(부분) — { nickname?, currentPassword?, newPassword?, allergies? } → MyPageResponse */
export async function updateMe(payload) {
  const { data } = await http.put('/api/member/me', payload)
  return data
}

/** 내가 쓴 리뷰 목록 — ?page=&size= → PageResponse<MyReviewItem> */
export async function listMyReviews(params = {}) {
  const { data } = await http.get('/api/member/me/reviews', { params })
  return data
}
