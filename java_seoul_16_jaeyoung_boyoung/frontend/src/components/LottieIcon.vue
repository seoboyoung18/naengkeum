<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import lottie from 'lottie-web'

const props = defineProps({
  data: { type: Object, required: true },     // 가져온 Lottie JSON
  size: { type: Number, default: 28 },         // 표시 박스 크기(px)
  zoom: { type: Number, default: 1 },          // >1이면 확대 후 박스에 맞춰 여백 crop
  loop: { type: Boolean, default: true },
  autoplay: { type: Boolean, default: true },
})

const el = ref(null)
let anim = null

onMounted(() => {
  anim = lottie.loadAnimation({
    container: el.value,
    renderer: 'svg',
    loop: props.loop,
    autoplay: props.autoplay,
    animationData: props.data,
  })
})
onBeforeUnmount(() => { if (anim) anim.destroy() })
</script>

<template>
  <span class="lottie" :style="{ width: size + 'px', height: size + 'px' }">
    <span ref="el" class="inner" :style="{ width: size * zoom + 'px', height: size * zoom + 'px' }"></span>
  </span>
</template>

<style scoped>
.lottie {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;          /* 확대된 내부를 박스에 맞춰 잘라냄(crop) */
  vertical-align: middle;
  flex: 0 0 auto;
}
.inner { flex: 0 0 auto; }
</style>
