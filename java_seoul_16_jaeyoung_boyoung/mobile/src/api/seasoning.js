import http from './http'

/** 조미료 카탈로그 + 내 보유여부 → [{ seasoningId, name, owned, storageType }] */
export async function getSeasonings() {
  const { data } = await http.get('/api/seasonings')
  return data
}

/** 보유 조미료 집합 저장 → 갱신된 목록 */
export async function saveSeasonings(seasoningIds) {
  const { data } = await http.put('/api/seasonings', { seasoningIds })
  return data
}
