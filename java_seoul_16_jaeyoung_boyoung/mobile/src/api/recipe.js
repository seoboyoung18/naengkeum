import http from './http'

/**
 * 레시피 검색/목록 — PageResponse
 * params: { keyword, sort(LATEST|POPULAR|RATING|COOK_TIME), ingredients(csv), minCookTime, maxCookTime, mine, page, size }
 * → { content:[RecipeListItem], page, size, totalElements, totalPages }
 */
export async function searchRecipes(params = {}) {
  const { data } = await http.get('/api/recipe', { params })
  return data
}

/** 제목 자동완성 — ?keyword= → [{ recipeId, title }] */
export async function autocompleteRecipes(keyword) {
  const { data } = await http.get('/api/recipe/autocomplete', { params: { keyword } })
  return data
}

/** 인기 레시피 — ?limit= → [RecipeListItem] */
export async function popularRecipes(limit = 8) {
  const { data } = await http.get('/api/recipe/popular', { params: { limit } })
  return data
}

/** 레시피 상세 */
export async function fetchRecipeDetail(recipeId) {
  const { data } = await http.get(`/api/recipe/${recipeId}`)
  return data
}
