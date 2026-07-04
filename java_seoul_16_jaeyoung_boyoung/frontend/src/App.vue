<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import DefaultLayout from './layouts/DefaultLayout.vue'
import ToastContainer from './components/ToastContainer.vue'

const route = useRoute()
// 로그인/회원가입 등 noLayout 라우트는 전체화면, 나머지는 공통 레이아웃
const useLayout = computed(() => !route.meta.noLayout)
</script>

<template>
  <DefaultLayout v-if="useLayout">
    <RouterView v-slot="{ Component }">
      <Transition name="fade" mode="out-in">
        <component :is="Component" />
      </Transition>
    </RouterView>
  </DefaultLayout>
  <RouterView v-else v-slot="{ Component }">
    <Transition name="fade" mode="out-in">
      <component :is="Component" />
    </Transition>
  </RouterView>

  <ToastContainer />
</template>
