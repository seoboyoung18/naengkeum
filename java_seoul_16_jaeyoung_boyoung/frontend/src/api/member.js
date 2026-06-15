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

/** 내가 획득한 배지 목록 — BadgeItem[] { badgeId, name, iconUrl, earnedAt } */
export async function listBadges() {
  const { data } = await http.get('/api/member/me/badges')
  return data
}

/** 타 유저 프로필(공개) — { memberId, nickname, reviewCount, followerCount, isFollowing } */
export async function fetchProfile(userId) {
  const { data } = await http.get(`/api/member/${userId}/profile`)
  return data
}

/** 타 유저가 공개한 레시피 목록(공개) — RecipeListItem[] */
export async function fetchUserRecipes(userId) {
  const { data } = await http.get(`/api/member/${userId}/recipes`)
  return data
}

/** 내가 팔로우하는 목록 — FollowUserItem[] */
export async function listFollowing() {
  const { data } = await http.get('/api/member/me/following')
  return data
}

/** 나를 팔로우하는 목록 — FollowUserItem[] */
export async function listFollowers() {
  const { data } = await http.get('/api/member/me/followers')
  return data
}
