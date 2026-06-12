<script setup>
import { useRouter } from 'vue-router'
import MyRecipeList from '../components/MyRecipeList.vue'

const router = useRouter()
</script>

<template>
  <section>
    <button class="back" @click="router.back()">← 뒤로</button>
    <h2 class="h">🤖 마이 레시피 → 공개 등록</h2>
    <p class="lead">
      공개할 레시피를 선택하세요. 공개하면 모두가 검색·찜할 수 있는 공개 레시피가 됩니다 (출처: 🤖 AI).
    </p>

    <!-- 비공개(공개 후보)만 노출 -->
    <MyRecipeList :only-private="true" />
  </section>
</template>

<style scoped>
.back { border: none; background: none; color: #16a34a; font-size: 14px; cursor: pointer; padding: 0 0 10px; }
.h { font-size: 22px; margin: 0 0 6px; }
.lead { color: #777; font-size: 14px; margin: 0 0 18px; }
</style>
