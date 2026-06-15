<script setup>
import { API_BASE } from '../api/http'
import InlineIcon from './InlineIcon.vue'
import clockSvg from '../assets/icons/clock-outline.svg?raw'
import starSvg from '../assets/icons/star.svg?raw'
import botUrl from '../assets/icons/message-bot.svg'

const props = defineProps({
  recipe: { type: Object, required: true },
  // 찜 하트 노출 여부 (타 유저 프로필 등에서 끄고 싶을 때 false)
  wishable: { type: Boolean, default: true },
})
const emit = defineEmits(['open', 'toggle-wish'])

// 업로드 이미지(/images/...)는 백엔드 호스트를 붙여 절대경로로. 외부 URL은 그대로.
function imageUrl(u) {
  if (!u) return null
  return u.startsWith('/') ? API_BASE + u : u
}

// 출처 배지 (공공/직접/AI)
const SOURCE_BADGE = {
  PUBLIC: { label: '공공', cls: 'pub' },
  USER: { label: '✍️ 직접', cls: 'user' },
  AI_SAVED: { label: 'AI', cls: 'ai' },
}
function badge(source) { return SOURCE_BADGE[source] || SOURCE_BADGE.PUBLIC }
</script>

<template>
  <li class="card" @click="emit('open', recipe.recipeId)">
    <div class="thumb" :style="imageUrl(recipe.thumbnailUrl) ? { backgroundImage: `url(${imageUrl(recipe.thumbnailUrl)})` } : null">
      <span v-if="!recipe.thumbnailUrl">🍽️</span>
      <button v-if="wishable" class="heart" :class="{ on: recipe.isWishlisted }" @click.stop="emit('toggle-wish', recipe)">
        {{ recipe.isWishlisted ? '♥' : '♡' }}
      </button>
    </div>
    <div class="info">
      <div class="trow">
        <div class="title">{{ recipe.title }}</div>
        <span class="src" :class="badge(recipe.source).cls"><img v-if="recipe.source === 'AI_SAVED'" :src="botUrl" class="bi" alt="" />{{ badge(recipe.source).label }}</span>
      </div>
      <div class="meta">
        <span v-if="recipe.cookTime"><InlineIcon :svg="clockSvg" :size="12" /> {{ recipe.cookTime }}분</span>
        <span><InlineIcon :svg="starSvg" :size="13" style="transform: translateY(-2px)" /> {{ Number(recipe.avgRating).toFixed(1) }}<template v-if="recipe.reviewCount"> ({{ recipe.reviewCount }})</template></span>
      </div>
      <div class="ings" v-if="recipe.mainIngredients?.length">{{ recipe.mainIngredients.join(' · ') }}</div>
    </div>
  </li>
</template>

<style scoped>
.card { background: #fff; border: 1px solid var(--line); box-shadow: var(--shadow-card); border-radius: 14px; overflow: hidden; cursor: pointer; transition: box-shadow .15s; }
.card:hover { box-shadow: 0 6px 18px rgba(0,0,0,.06); }
.thumb { position: relative; height: 150px; background: #f1f3f5 center/cover no-repeat; display: flex; align-items: center; justify-content: center; font-size: 34px; color: #c7ccd1; }
.heart { position: absolute; top: 10px; right: 10px; border: none; background: rgba(255,255,255,.9); width: 32px; height: 32px; border-radius: 50%; font-size: 17px; color: #bbb; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.heart.on { color: #ef4444; }
.info { padding: 14px 16px 16px; }
.trow { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.title { font-size: 15px; font-weight: 700; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.src { flex: 0 0 auto; font-size: 11px; font-weight: 700; border-radius: 999px; padding: 3px 8px; background: #eef2f7; color: #64748b; }
.src.user { background: #fff7ed; color: #c2410c; }
.src.ai { background: #f5f3ff; color: #7c3aed; }
.bi { width: 14px; height: 14px; object-fit: contain; vertical-align: -2px; }
.meta { display: flex; gap: 10px; font-size: 12px; color: #888; margin-top: 8px; }
.ings { font-size: 12px; color: #aaa; margin-top: 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
