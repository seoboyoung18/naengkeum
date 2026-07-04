import http from './http'

/** 냉장고 목록 — params: { storageType, sort } → { items, summary } */
export async function listFridge(params = {}) {
  const { data } = await http.get('/api/fridge', { params })
  return data
}

/** 재료 등록 → 201 FridgeItem */
export async function createFridgeItem(payload) {
  const { data } = await http.post('/api/fridge', payload)
  return data
}

/** 재료 수정 → FridgeItem */
export async function updateFridgeItem(id, payload) {
  const { data } = await http.put(`/api/fridge/${id}`, payload)
  return data
}

/** 재료 삭제 → { message } */
export async function deleteFridgeItem(id) {
  const { data } = await http.delete(`/api/fridge/${id}`)
  return data
}

/** 대시보드 — { summary, expiringItems } (D-3 이내) */
export async function fetchDashboard() {
  const { data } = await http.get('/api/fridge/dashboard')
  return data
}
