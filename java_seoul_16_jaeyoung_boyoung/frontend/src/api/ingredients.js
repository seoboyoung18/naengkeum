import http from './http'

/** 식재료 자동완성 — ?keyword= → [{ ingredientDictId, name, category }] */
export async function autocompleteIngredients(keyword) {
  const { data } = await http.get('/api/ingredients/autocomplete', { params: { keyword } })
  return data
}

/**
 * 보관기한/보관법 제안 — ?name= →
 * { name, category, defaultStorageType, fridgeDays, freezerDays, roomTempDays, storageTip, found }
 * 사전에 없으면 found=false 폴백.
 */
export async function suggestIngredient(name) {
  const { data } = await http.get('/api/ingredients/suggest', { params: { name } })
  return data
}
