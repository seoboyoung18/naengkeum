<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import DefaultLayout from './layouts/DefaultLayout.vue'

const route = useRoute()
// 로그인/회원가입 등 noLayout 라우트는 전체화면, 나머지는 공통 레이아웃
const useLayout = computed(() => !route.meta.noLayout)
</script>

<template>
  <DefaultLayout v-if="useLayout">
    <RouterView />
  </DefaultLayout>
  <RouterView v-else />
</template>
