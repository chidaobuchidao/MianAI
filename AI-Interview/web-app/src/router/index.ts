import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomeView.vue')
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue')
    },
    {
      path: '/interview',
      name: 'interview',
      component: () => import('@/views/InterviewView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/interview/report',
      name: 'interview-report',
      component: () => import('@/views/InterviewReportView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/interview/history',
      name: 'interview-history',
      component: () => import('@/views/InterviewHistoryView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/resume/upload',
      name: 'resume-upload',
      component: () => import('@/views/ResumeUploadView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/resume/report',
      name: 'resume-report',
      component: () => import('@/views/ResumeReportView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/resume/history',
      name: 'resume-history',
      component: () => import('@/views/ResumeHistoryView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/questions',
      name: 'questions',
      component: () => import('@/views/QuestionsView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/questions/:id',
      name: 'question-detail',
      component: () => import('@/views/QuestionDetailView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/practice',
      name: 'practice',
      component: () => import('@/views/PracticeView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/practice/do',
      name: 'practice-do',
      component: () => import('@/views/PracticeDoView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/wrong-book',
      name: 'wrong-book',
      component: () => import('@/views/WrongBookView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('@/views/ProfileView.vue'),
      meta: { requiresAuth: true }
    }
  ],
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to, _from, next) => {
  if (to.path === '/login') {
    next()
    return
  }
  const userStore = useUserStore()
  // Auto-generate dev token if not logged in
  if (!userStore.token) {
    userStore.setUser({
      userId: 1,
      nickname: '开发者',
      avatarUrl: '',
      token: 'dev-token-' + Date.now()
    })
  }
  next()
})

export default router
