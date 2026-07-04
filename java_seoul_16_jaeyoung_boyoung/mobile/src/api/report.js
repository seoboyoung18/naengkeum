import http from './http'

/** 콘텐츠 신고 — { targetType:'RECIPE'|'REVIEW', targetId, reason? } (중복 409) */
export async function createReport(payload) {
  const { data } = await http.post('/api/report', payload)
  return data
}
