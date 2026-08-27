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
      {path:'seasons',name:'user-seasons',component:view('user/UserSeasons')},
      {path:'seasons/:id/rounds',name:'user-rounds',component:view('user/UserRounds')},
      {path:'seasons/:id/standings',name:'user-standings',component:view('user/UserStandings')},
      {path:'matches',name:'user-matches',component:view('user/UserMatches')},
      {path:'matches/:id',name:'user-match-detail',component:view('user/UserMatchDetail')},
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
      { path: 'profile', name: 'club-profile', component: view('club/ClubProfile') },
      { path: 'players', name: 'club-players', component: view('club/ClubPlayers') },
      { path: 'coaches', name: 'club-coaches', component: view('club/ClubCoaches') },
      { path: 'stats', name: 'club-stats', component: view('club/ClubStats') },
      { path: 'matches', name: 'club-matches', component: view('club/ClubMatches') },
      { path: 'matches/:id/tickets', name: 'club-match-tickets', component: view('club/ClubMatchTickets') },
      { path: 'statistics', name: 'club-statistics', component: view('club/ClubStatistics') },
    ],
  },
  {
    path:'/checker',component:ManagementLayout,meta:{requiresAuth:true,roles:['CHECKER']},children:[
      {path:'',redirect:'/checker/matches'},
      {path:'matches',name:'checker-matches',component:view('checker/CheckerMatches')},
      {path:'matches/:id/checkin',name:'checker-checkin',component:view('checker/CheckerCheckin')},
      {path:'checkins',name:'checker-checkins',component:view('checker/CheckerCheckins')},
    ],
  },
  {
    path: '/admin', component: ManagementLayout, meta: { requiresAuth: true, roles: ['ADMIN'] },
    children: [
      { path: '', redirect: '/admin/users' },
      { path: 'users', name: 'admin-users', component: view('admin/AdminUsers') },
      { path: 'clubs', name: 'admin-clubs', component: view('admin/AdminClubList') },
      { path: 'clubs/:id', name: 'admin-club-detail', component: view('admin/AdminClubDetail') },
      { path: 'seasons', name: 'admin-seasons', component: view('admin/AdminSeasons') },
      { path: 'seasons/:id', name: 'admin-season-detail', component: view('admin/AdminSeasonDetail') },
      { path: 'matches', name: 'admin-matches', component: view('admin/AdminMatches') },
      { path: 'matches/:id', name: 'admin-match-detail', component: view('admin/AdminMatchDetail') },
      { path: 'matches/:id/tickets', name: 'admin-match-tickets', component: view('admin/AdminMatchTickets') },
      { path: 'stadiums', name: 'admin-stadiums', component: view('admin/AdminStadiums') },
      { path: 'stadiums/:id', name: 'admin-stadium-detail', component: view('admin/AdminStadiumDetail') },
      { path: 'refunds', name: 'admin-refunds', component: view('admin/AdminRefunds') },
      { path: 'refunds/:id', name: 'admin-refund-detail', component: view('admin/AdminRefundDetail') },
      { path: 'checkins', name: 'admin-checkins', component: view('admin/AdminCheckins') },
      { path: 'statistics', name: 'admin-statistics', component: view('admin/AdminStatistics') },
      { path: 'statistics/matches', name: 'admin-statistics-matches', component: view('admin/AdminStatisticsMatches') },
    ],
  },
  { path: '/403', name: 'forbidden', component: view('Forbidden'), meta: { requiresAuth: true } },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: view('NotFound') },
]

const router = createRouter({ history: createWebHistory(), routes, scrollBehavior: () => ({ top: 0 }) })

router.beforeEach(async (to) => {
  const authStore = useAuthStore(pinia)
  if (to.meta.public) {
    if (to.name === 'login' && authStore.isAuthenticated) return authStore.homePath
    return true
  }
  if (to.meta.requiresAuth && !authStore.isAuthenticated) return { name: 'login', query: { redirect: to.fullPath } }
  if (authStore.isAuthenticated && !authStore.user?.permissions) {
    try {
      await authStore.fetchMe()
    } catch {
      authStore.logout()
      return { name: 'login' }
    }
  }
  if (to.meta.roles?.length && !to.meta.roles.includes(authStore.user?.roleCode)) return { name: 'forbidden' }
  return true
})

export default router
