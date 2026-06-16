import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  { path: '/', redirect: '/home' },

  // 공개(레이아웃 없음)
  { path: '/login', name: 'login', component: () => import('../views/LoginView.vue'), meta: { public: true, noLayout: true } },
  { path: '/register', name: 'register', component: () => import('../views/RegisterView.vue'), meta: { public: true, noLayout: true } },

  // 인증 필요(공통 레이아웃)
  { path: '/home', name: 'home', component: () => import('../views/HomeView.vue') },
  { path: '/fridge', name: 'fridge', component: () => import('../views/FridgeView.vue') },
  { path: '/recipe', name: 'recipe', component: () => import('../views/RecipeView.vue') },
  { path: '/recipe/publish', name: 'recipe-publish', component: () => import('../views/MyRecipePublishView.vue') },
  { path: '/recipe/:recipeId', name: 'recipe-detail', component: () => import('../views/RecipeDetailView.vue'), props: true },
  { path: '/ai-recommend', name: 'ai-recommend', component: () => import('../views/AiRecommendView.vue') },
  { path: '/challenge', name: 'challenge', component: () => import('../views/ChallengeView.vue') },
  { path: '/challenge/:challengeId', name: 'challenge-detail', component: () => import('../views/ChallengeDetailView.vue'), props: true },
  { path: '/follow', name: 'follow-list', component: () => import('../views/FollowListView.vue') },
  { path: '/user/:userId', name: 'user-profile', component: () => import('../views/UserProfileView.vue'), props: true },
  { path: '/mypage', name: 'mypage', component: () => import('../views/MyPageView.vue') },

  // 관리자 전용
  { path: '/admin', name: 'admin', component: () => import('../views/AdminView.vue'), meta: { admin: true } },

  { path: '/:pathMatch(.*)*', redirect: '/home' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 인증 가드: 비공개 페이지는 로그인 필요, 로그인 상태로 /login 접근 시 홈으로
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && auth.isAuthenticated) {
    return { name: 'home' }
  }
  // 관리자 전용 페이지는 ADMIN만
  if (to.meta.admin && !auth.isAdmin) {
    return { name: 'home' }
  }
})

export default router
