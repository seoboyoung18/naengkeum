import http from './http'

/**
 * 콘텐츠 신고 (일반 사용자용) — POST /api/report.
 *
 * @param {{ targetType: 'RECIPE'|'REVIEW', targetId: number, reason?: string }} payload
 * @returns {Promise<{ message: string }>}
 *
 * 인증 필요. 같은 대상을 이미 신고했으면 백엔드가 409를 반환한다.
 */
export async function createReport(payload) {
  const { data } = await http.post('/api/report', payload)
  return data
}
