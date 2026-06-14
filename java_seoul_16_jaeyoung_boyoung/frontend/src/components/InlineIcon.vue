<script setup>
defineProps({
  svg: { type: String, required: true },   // ?raw 로 가져온 SVG 문자열
  size: { type: Number, default: 16 },
})
</script>

<template>
  <span class="ii" :style="{ width: size + 'px', height: size + 'px' }" v-html="svg"></span>
</template>

<style scoped>
.ii { display: inline-flex; align-items: center; justify-content: center; vertical-align: middle; line-height: 0; }
.ii :deep(svg) { width: 100%; height: 100%; display: block; }
</style>
