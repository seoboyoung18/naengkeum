import http from './http'

/** 챌린지 목록 — status: 'active'|'ended' (없으면 전체) */
export async function listChallenges(status) {
  const { data } = await http.get('/api/challenge', { params: status ? { status } : {} })
  return data
}

/** 내가 참여 중인 챌린지 */
export async function myChallenges() {
  const { data } = await http.get('/api/challenge/my')
  return data
}

/** 활성 사용자 통계 → { activeParticipants } */
export async function fetchChallengeStats() {
  const { data } = await http.get('/api/challenge/stats')
  return data
}

/** 챌린지 상세 */
export async function fetchChallengeDetail(challengeId) {
  const { data } = await http.get(`/api/challenge/${challengeId}`)
  return data
}

/** 참여 → { joined: true } (중복 409) */
export async function joinChallenge(challengeId) {
  const { data } = await http.post(`/api/challenge/${challengeId}/join`)
  return data
}

/** 참여 취소 → { joined: false } */
export async function unjoinChallenge(challengeId) {
  const { data } = await http.delete(`/api/challenge/${challengeId}/join`)
  return data
}

/** 진행률 갱신 → { progress, achieved, badgeEarned } */
export async function updateProgress(challengeId, progress) {
  const { data } = await http.patch(`/api/challenge/${challengeId}/progress`, { progress })
  return data
}
