<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import lottie from 'lottie-web'
import animationData from '../assets/fridge.json'

const props = defineProps({
  size: { type: Number, default: 28 },
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
    animationData,
  })
})
onBeforeUnmount(() => { if (anim) anim.destroy() })
</script>

<template>
  <span ref="el" class="lottie" :style="{ width: size + 'px', height: size + 'px' }"></span>
</template>

<style scoped>
.lottie { display: inline-block; vertical-align: middle; flex: 0 0 auto; }
</style>
