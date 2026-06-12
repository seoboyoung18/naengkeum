import http from './http'

/**
 * 레시피 검색/목록 — PageResponse
 * params: { keyword, sort(LATEST|POPULAR|RATING|COOK_TIME), ingredients(csv), maxCookTime, page, size }
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

/** 레시피 상세 — { recipeId, title, summary, ingredients, steps, nutrition, avgRating, isWishlisted, ... } */
export async function fetchRecipeDetail(recipeId) {
  const { data } = await http.get(`/api/recipe/${recipeId}`)
  return data
}

/**
 * "내 레시피로 담기" — AI 결과 콘텐츠를 마이 레시피(비공개)로 등록 → { recipeId }
 * payload: { title, summary, ingredientsJson:[{name,qty,unit}], stepsJson:[{stepNumber,description}], cookTime }
 */
export async function registerFromAi(payload) {
  const { data } = await http.post('/api/recipe/from-ai', payload)
  return data
}

/** 마이 레시피 목록(공개/비공개 포함) → [{ recipeId, title, cookTime, isPublic, source, createdAt }] */
export async function listMyRecipes() {
  const { data } = await http.get('/api/recipe/mine')
  return data
}

/** "공개하기" — 마이 레시피를 공개 카탈로그에 게시 → { recipeId, isPublic } */
export async function publishRecipe(recipeId) {
  const { data } = await http.patch(`/api/recipe/${recipeId}/publish`)
  return data
}
