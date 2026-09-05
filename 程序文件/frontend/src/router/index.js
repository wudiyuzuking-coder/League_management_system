import { createRouter, createWebHistory } from 'vue-router'
import ManagementLayout from '../layouts/ManagementLayout.vue'
import UserLayout from '../layouts/UserLayout.vue'
import pinia from '../stores'
import { useAuthStore } from '../stores/auth'

const pages = import.meta.glob('../views/**/*.vue')
const view = path => pages[`../views/${path}.vue`]

const routes = [
  { path: '/', redirect: () => useAuthStore(pinia).homePath },
  { path: '/login', name: 'login', component: view('auth/LoginView'), meta: { public: true } },
  { path: '/register', name: 'register', component: view('auth/RegisterView'), meta: { public: true } },
  {
    path:'/user',component:UserLayout,meta:{requiresAuth:true,roles:['USER']},children:[
      {path:'',redirect:'/user/seasons'},
      {path:'profile',name:'user-account-profile',component:view('shared/AccountProfile')},
      {path:'seasons',name:'user-seasons',component:view('user/UserSeasons')},
      {path:'seasons/:id/rounds',name:'user-rounds',component:view('user/UserRounds')},
      {path:'seasons/:id/standings',name:'user-standings',component:view('user/UserStandings')},
      {path:'matches',name:'user-matches',component:view('user/UserMatches')},
      {path:'matches/:id',name:'user-match-detail',component:view('user/UserMatchDetail')},
      {path:'clubs/:clubId',name:'user-club-detail',component:view('user/UserClubDetail')},
      {path:'orders',name:'user-orders',component:view('user/UserOrders')},
      {path:'orders/:id',name:'user-order-detail',component:view('user/UserOrderDetail')},
      {path:'tickets',name:'user-tickets',component:view('user/UserTickets')},
      {path:'tickets/:id',name:'user-ticket-detail',component:view('user/UserTicketDetail')},
      {path:'refunds',name:'user-refunds',component:view('user/UserRefunds')},
      {path:'refunds/:id',name:'user-refund-detail',component:view('user/UserRefundDetail')},
    ],
  },
  {
    path: '/club', component: ManagementLayout, meta: { requiresAuth: true, roles: ['CLUB'] },
    children: [
      { path: '', redirect: '/club/profile' },
      { path: 'account', name: 'club-account-profile', component: view('shared/AccountProfile') },
      { path: 'profile', name: 'club-profile', component: view('club/ClubProfile') },
      { path: 'players', name: 'club-players', component: view('club/ClubPlayers') },
      { path: 'coaches', name: 'club-coaches', component: view('club/ClubCoaches') },
      { path: 'enrollments', name: 'club-enrollments', component: view('club/ClubEnrollments') },
      { path: 'schedules', name: 'club-schedules', component: view('club/ClubSchedules') },
      { path: 'stats', name: 'club-stats', component: view('club/ClubStats') },
      { path: 'matches', name: 'club-matches', component: view('club/ClubMatches') },
      { path: 'matches/:id/tickets', name: 'club-match-tickets', component: view('club/ClubMatchTickets') },
      { path: 'statistics', name: 'club-statistics', component: view('club/ClubStatistics') },
    ],
  },
  {
    path: '/admin', component: ManagementLayout, meta: { requiresAuth: true, roles: ['ADMIN', 'EVENT_ADMIN'] },
    children: [
      { path: '', redirect: () => (useAuthStore(pinia).user?.roleCode === 'EVENT_ADMIN' ? '/admin/matches' : '/admin/users') },
      { path: 'account', name: 'admin-account-profile', component: view('shared/AccountProfile') },
      { path: 'users', name: 'admin-users', component: view('admin/AdminUsers'), meta: { roles: ['ADMIN'] } },
      { path: 'clubs', name: 'admin-clubs', component: view('admin/AdminClubList'), meta: { roles: ['ADMIN'] } },
      { path: 'clubs/:id', name: 'admin-club-detail', component: view('admin/AdminClubDetail'), meta: { roles: ['ADMIN'] } },
      { path: 'seasons', name: 'admin-seasons', component: view('admin/AdminSeasons'), meta: { roles: ['EVENT_ADMIN'] } },
      { path: 'seasons/:id', name: 'admin-season-detail', component: view('admin/AdminSeasonDetail'), meta: { roles: ['EVENT_ADMIN'] } },
      { path: 'enrollments', name: 'admin-enrollments', component: view('admin/AdminEnrollments'), meta: { roles: ['EVENT_ADMIN'] } },
      { path: 'schedules', name: 'admin-schedules', component: view('admin/AdminSchedules'), meta: { roles: ['EVENT_ADMIN'] } },
      { path: 'matches', name: 'admin-matches', component: view('admin/AdminMatches'), meta: { roles: ['EVENT_ADMIN'] } },
      { path: 'matches/result-reminders', name: 'admin-match-result-reminders', component: view('admin/AdminMatchResultReminders'), meta: { roles: ['EVENT_ADMIN'] } },
      { path: 'matches/:id', name: 'admin-match-detail', component: view('admin/AdminMatchDetail'), meta: { roles: ['EVENT_ADMIN'] } },
      { path: 'matches/:id/tickets', name: 'admin-match-tickets', component: view('admin/AdminMatchTickets'), meta: { roles: ['EVENT_ADMIN'] } },
      { path: 'stadiums', name: 'admin-stadiums', component: view('admin/AdminStadiums'), meta: { roles: ['EVENT_ADMIN'] } },
      { path: 'stadiums/:id', name: 'admin-stadium-detail', component: view('admin/AdminStadiumDetail'), meta: { roles: ['EVENT_ADMIN'] } },
      { path: 'refunds', name: 'admin-refunds', component: view('admin/AdminRefunds'), meta: { roles: ['EVENT_ADMIN'] } },
      { path: 'refunds/:id', name: 'admin-refund-detail', component: view('admin/AdminRefundDetail'), meta: { roles: ['EVENT_ADMIN'] } },
      { path: 'checkins', name: 'admin-checkins', component: view('admin/AdminCheckins'), meta: { roles: ['ADMIN'] } },
      { path: 'statistics', name: 'admin-statistics', component: view('admin/AdminStatistics'), meta: { roles: ['EVENT_ADMIN'] } },
      { path: 'statistics/matches', name: 'admin-statistics-matches', component: view('admin/AdminStatisticsMatches'), meta: { roles: ['EVENT_ADMIN'] } },
    ],
  },
  { path: '/403', name: 'forbidden', component: view('Forbidden'), meta: { requiresAuth: true } },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: view('NotFound') },
]

const router = createRouter({ history: createWebHistory(), routes, scrollBehavior: () => ({ top: 0 }) })

router.beforeEach(async (to) => {
  const authStore = useAuthStore(pinia)
  if (authStore.token && !authStore.hasValidIdentity) authStore.logout()
  if (authStore.isAuthenticated && !authStore.sessionValidated) {
    try {
      await authStore.fetchMe()
    } catch {
      authStore.logout()
      if (to.name === 'login') return true
      return { name: 'login', query: to.meta.public ? {} : { redirect: to.fullPath } }
    }
  }
  if (to.meta.public) {
    if (to.name === 'login' && authStore.isAuthenticated) return authStore.homePath
    return true
  }
  if (to.meta.requiresAuth && !authStore.isAuthenticated) return { name: 'login', query: { redirect: to.fullPath } }
  if (to.meta.roles?.length && !to.meta.roles.includes(authStore.user?.roleCode)) return { name: 'forbidden' }
  return true
})

export default router
