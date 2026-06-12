<script setup>
import { useToast } from '../composables/useToast'
const { items, remove } = useToast()
</script>

<template>
  <div class="toasts">
    <TransitionGroup name="toast">
      <div
        v-for="t in items"
        :key="t.id"
        class="toast"
        :class="t.type"
        @click="remove(t.id)"
      >
        <span class="ic">{{ t.type === 'success' ? '✓' : t.type === 'error' ? '!' : 'ⓘ' }}</span>
        <span>{{ t.message }}</span>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.toasts {
  position: fixed;
  left: 0; right: 0; bottom: 84px;
  display: flex; flex-direction: column; align-items: center; gap: 8px;
  z-index: 100; pointer-events: none; padding: 0 16px;
}
.toast {
  pointer-events: auto;
  display: flex; align-items: center; gap: 8px;
  max-width: 360px; width: fit-content;
  background: #222; color: #fff;
  font-size: 13px; padding: 11px 16px; border-radius: 10px;
  box-shadow: 0 8px 24px rgba(0,0,0,.25); cursor: pointer;
}
.toast .ic {
  display: inline-flex; align-items: center; justify-content: center;
  width: 18px; height: 18px; border-radius: 50%; font-size: 12px; font-weight: 700;
}
.toast.success { background: var(--primary-deep); }
.toast.success .ic { background: rgba(255,255,255,.25); }
.toast.error { background: #dc2626; }
.toast.error .ic { background: rgba(255,255,255,.25); }
.toast.info .ic { background: rgba(255,255,255,.25); }

.toast-enter-active, .toast-leave-active { transition: all .25s ease; }
.toast-enter-from { opacity: 0; transform: translateY(12px); }
.toast-leave-to { opacity: 0; transform: translateY(8px); }
</style>
