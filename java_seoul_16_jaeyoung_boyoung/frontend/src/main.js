import { createApp } from 'vue'
import { createPinia } from 'pinia'
import './style.css'
import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/auth'

const app = createApp(App)
app.use(createPinia())
app.use(router)

// 앱 시작 시 localStorage 토큰으로 인증 상태 복원 (새로고침해도 로그인 유지)
useAuthStore().restore()

app.mount('#app')
