<script setup>
import {onMounted,reactive,ref} from 'vue'
import {ElMessage} from 'element-plus'
import {getAdminClubs,updateAdminClubLeaderStatus,updateAdminClubStatus} from '../../api/club'
import {approveClubUser,getClubApplications} from '../../api/user'
import {confirmAction} from '../../utils/confirmAction'
import {maskPhone} from '../../utils/privacy'

const rows=ref([]),pending=ref([]),total=ref(0),loading=ref(false),error=ref(''),actionKey=ref('')
const query=reactive({name:'',status:'',page:1,size:10})
const clubStatusOptions=Object.freeze([{value:'ACTIVE',label:'正常'},{value:'DISABLED',label:'已停用'}])
const load=async()=>{loading.value=true;error.value='';try{const [clubs,applications]=await Promise.all([getAdminClubs(query),getClubApplications({page:1,size:100})]);rows.value=clubs.data.records;total.value=clubs.data.total;pending.value=applications.data.records}catch(e){error.value=e?.message||'加载俱乐部失败，请稍后重试。'}finally{loading.value=false}}
const reset=()=>{Object.assign(query,{name:'',status:'',page:1});load()}
const toggleClub=async row=>{const enabling=row.clubStatus!=='ACTIVE';await confirmAction({title:enabling?'启用俱乐部':'停用俱乐部',message:`确定${enabling?'启用':'停用'}俱乐部“${row.clubName}”吗？`,impact:enabling?'启用后，俱乐部可恢复参与后续业务。':'停用后，俱乐部将无法参与新的业务；历史数据不会删除。',confirmButtonText:enabling?'确认启用':'确认停用',danger:!enabling});actionKey.value=`club-${row.clubId}`;try{await updateAdminClubStatus(row.clubId,enabling?'ACTIVE':'DISABLED');ElMessage.success('俱乐部状态已更新');await load()}finally{actionKey.value=''}}
const toggleLeader=async row=>{if(!row.leaderStatus)return;const enabling=row.leaderStatus!=='ENABLED';await confirmAction({title:enabling?'启用负责人账号':'停用负责人账号',message:`确定${enabling?'启用':'停用'}“${row.leaderName}”的负责人账号吗？`,impact:enabling?'启用后，负责人可以重新登录。':'停用后，负责人将无法登录；俱乐部及历史业务数据不会删除。',confirmButtonText:enabling?'确认启用':'确认停用',danger:!enabling});actionKey.value=`leader-${row.clubId}`;try{await updateAdminClubLeaderStatus(row.clubId,enabling?'ENABLED':'DISABLED');ElMessage.success('负责人账号状态已更新');await load()}finally{actionKey.value=''}}
const approve=async row=>{await confirmAction({title:'俱乐部注册审核',message:`确认通过俱乐部“${row.clubApplyName}”的注册申请吗？`,impact:'通过后将创建新俱乐部并绑定当前申请人为负责人。',confirmButtonText:'审核通过'});await approveClubUser(row.userId);ElMessage.success('审核通过并完成负责人绑定');await load()}
onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="俱乐部管理" subtitle="管理俱乐部、负责人账号与待审核的注册申请。" />
    <section class="app-surface">
      <FilterBar :model="query"><el-form-item label="名称"><el-input v-model="query.name" name="club-name-filter" autocomplete="off" clearable/></el-form-item><el-form-item label="状态"><el-select v-model="query.status" clearable><el-option v-for="option in clubStatusOptions" :key="option.value" :label="option.label" :value="option.value"/></el-select></el-form-item><template #actions><el-button @click="reset">重置</el-button><el-button type="primary" @click="query.page=1;load()">查询</el-button></template></FilterBar>
      <DataState :loading="loading" :error="error" :empty="!rows.length&&!pending.length" empty-title="暂无俱乐部" @retry="load">
        <el-table :data="rows" empty-text="暂无俱乐部"><el-table-column prop="clubName" label="名称" min-width="150"/><el-table-column label="负责人"><template #default="{row}">{{row.leaderName||'未绑定负责人'}}</template></el-table-column><el-table-column label="负责人手机号"><template #default="{row}">{{maskPhone(row.leaderPhone)}}</template></el-table-column><el-table-column label="负责人状态"><template #default="{row}"><StatusTag v-if="row.leaderStatus" :value="row.leaderStatus"/><span v-else>—</span></template></el-table-column><el-table-column label="俱乐部状态"><template #default="{row}"><StatusTag :value="row.clubStatus"/></template></el-table-column><el-table-column label="操作" min-width="260"><template #default="{row}"><RouterLink :to="`/admin/clubs/${row.clubId}`" class="el-button el-button--primary is-link">查看</RouterLink><el-button link :loading="actionKey===`club-${row.clubId}`" :type="row.clubStatus==='ACTIVE'?'danger':'success'" @click="toggleClub(row)">{{row.clubStatus==='ACTIVE'?'停用俱乐部':'启用俱乐部'}}</el-button><el-button v-if="row.leaderStatus" link :loading="actionKey===`leader-${row.clubId}`" :type="row.leaderStatus==='ENABLED'?'danger':'success'" @click="toggleLeader(row)">{{row.leaderStatus==='ENABLED'?'停用负责人':'启用负责人'}}</el-button></template></el-table-column></el-table>
        <el-pagination v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total,prev,pager,next" @current-change="load"/>
        <section class="pending-section"><div class="pending-section__heading"><div><h2>待审核俱乐部注册</h2><p>审核通过后将创建新俱乐部并绑定负责人。</p></div><StatusTag v-if="pending.length" value="PENDING"/></div><el-table :data="pending" empty-text="暂无待审核俱乐部注册"><el-table-column prop="clubApplyName" label="申请俱乐部"/><el-table-column prop="realName" label="负责人"/><el-table-column label="手机号"><template #default="{row}">{{maskPhone(row.phone)}}</template></el-table-column><el-table-column label="状态"><template #default="{row}"><StatusTag :value="row.userStatus"/></template></el-table-column><el-table-column label="操作"><template #default="{row}"><el-button link type="success" @click="approve(row)">审核通过</el-button></template></el-table-column></el-table></section>
      </DataState>
    </section>
  </div>
</template>

<style scoped>.pending-section{margin-top:var(--space-xl);padding-top:var(--space-lg);border-top:1px solid var(--border-color)}.pending-section__heading{display:flex;align-items:center;justify-content:space-between;margin-bottom:var(--space-md)}.pending-section h2{margin:0;font-size:19px}.pending-section p{margin:5px 0 0;color:var(--text-secondary)}</style>
