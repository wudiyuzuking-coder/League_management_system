import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import pinia from './stores'
import './assets/main.css'
import StatusTag from './components/StatusTag.vue'
import PageHeader from './components/PageHeader.vue'
import DataState from './components/DataState.vue'
import FilterBar from './components/FilterBar.vue'
import { statusLabel } from './constants/status'
import { formatDate, formatDateTime, formatMoney, formatRate } from './utils/format'

const app = createApp(App)
app.component('StatusTag', StatusTag)
app.component('PageHeader', PageHeader)
app.component('DataState', DataState)
app.component('FilterBar', FilterBar)
Object.assign(app.config.globalProperties, { $statusLabel: statusLabel, $formatDate: formatDate, $formatDateTime: formatDateTime, $formatMoney: formatMoney, $formatRate: formatRate })
app.use(pinia).use(router).use(ElementPlus).mount('#app')
