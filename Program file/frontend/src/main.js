import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import pinia from './stores'
import './assets/tokens.css'
import './assets/element-plus.css'
import './assets/main.css'
import ActionToolbar from './components/ActionToolbar.vue'
import CardShell from './components/CardShell.vue'
import ConfirmDialog from './components/ConfirmDialog.vue'
import EmptyState from './components/EmptyState.vue'
import StatusTag from './components/StatusTag.vue'
import PageHeader from './components/PageHeader.vue'
import DataState from './components/DataState.vue'
import FilterBar from './components/FilterBar.vue'
import MetricStrip from './components/MetricStrip.vue'
import TableWrapper from './components/TableWrapper.vue'
import { statusLabel } from './constants/status'
import { formatDate, formatDateTime, formatMoney, formatRate } from './utils/format'

const app = createApp(App)
app.component('ActionToolbar', ActionToolbar)
app.component('CardShell', CardShell)
app.component('ConfirmDialog', ConfirmDialog)
app.component('EmptyState', EmptyState)
app.component('StatusTag', StatusTag)
app.component('PageHeader', PageHeader)
app.component('DataState', DataState)
app.component('FilterBar', FilterBar)
app.component('MetricStrip', MetricStrip)
app.component('TableWrapper', TableWrapper)
Object.assign(app.config.globalProperties, { $statusLabel: statusLabel, $formatDate: formatDate, $formatDateTime: formatDateTime, $formatMoney: formatMoney, $formatRate: formatRate })
app.use(pinia).use(router).use(ElementPlus).mount('#app')
