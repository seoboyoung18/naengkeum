import { reactive } from 'vue'

// 전역 토스트 큐 (모듈 싱글톤)
const items = reactive([])
let seq = 0

function push(message, type, duration) {
  const id = ++seq
  items.push({ id, message, type })
  setTimeout(() => remove(id), duration)
}

function remove(id) {
  const i = items.findIndex((t) => t.id === id)
  if (i >= 0) items.splice(i, 1)
}

export function useToast() {
  return {
    items,
    success: (m, d = 2200) => push(m, 'success', d),
    error: (m, d = 2800) => push(m, 'error', d),
    info: (m, d = 2200) => push(m, 'info', d),
    remove,
  }
}
