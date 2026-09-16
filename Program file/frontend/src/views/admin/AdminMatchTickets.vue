<script setup>
import {computed,onMounted,reactive,ref} from 'vue'
import {useRoute} from 'vue-router'
import {ElMessage,ElMessageBox} from 'element-plus'
import {getMatch} from '../../api/match'
import {getZones} from '../../api/stadium'
import {createTicketZone,debugSeatAllocation,generateInventory,getAdminTicketZones,getInventoryLayout,initializeStandardTicketing,updateInventoryStatus,updateTicketZone,updateTicketZoneStatus} from '../../api/ticket'

const route=useRoute(),matchId=Number(route.params.id)
const match=ref({}),staticZones=ref([]),zones=ref([]),selected=ref(null),layout=ref([]),loading=ref(false),error=ref(''),saving=ref(false),operating=ref('')
const visible=ref(false),editingId=ref(null),formRef=ref(),debugCount=ref(2),debugResult=ref(null),debugging=ref(false),standardVisible=ref(false)
const standardStadium=computed(()=>staticZones.value.length===8&&staticZones.value.every(z=>z.zoneDirection&&z.ticketType))
const metrics=computed(()=>[
  {label:'总座位',value:zones.value.reduce((sum,z)=>sum+Number(z.totalSeatCount||0),0),meta:'服务端库存'},
  {label:'已售',value:zones.value.reduce((sum,z)=>sum+Number(z.soldSeatCount||0),0)},
  {label:'剩余可售',value:zones.value.reduce((sum,z)=>sum+Number(z.availableSeatCount||0),0)},
  {label:'锁定中',value:zones.value.reduce((sum,z)=>sum+Number(z.lockedSeatCount||0),0)},
])
const blank=()=>({stadiumZoneId:null,price:0}),form=reactive(blank()),automaticSaleStart=ref('')
const rules={stadiumZoneId:[{required:true,message:'请选择静态票区'}],price:[{required:true,type:'number',min:0,message:'票价不能小于0'}]}
const zoneStatusLabel=value=>({DRAFT:'未启用销售',ON_SALE:'已启用自动销售',PAUSED:'暂停销售',CLOSED:'已关闭'}[value]||'未知状态')
const saleStatusLabel=value=>({MATCH_UNAVAILABLE:'比赛不可售',NOT_ENABLED:'未启用',PAUSED:'暂停销售',CLOSED:'已关闭',NOT_STARTED:'尚未开售',ENDED:'已停售',SOLD_OUT:'已售罄',AVAILABLE:'销售中'}[value]||'未知状态')
const ticketTypeLabel=value=>({VIP:'VIP',NORMAL:'普通票'}[value]||'未知类型')
const directionLabel=value=>({EAST:'东',WEST:'西',SOUTH:'南',NORTH:'北'}[value]||'未知方向')
const nextActions=z=>({DRAFT:[['ON_SALE','开售'],['CLOSED','关闭']],ON_SALE:[['PAUSED','暂停'],['CLOSED','关闭']],PAUSED:[['ON_SALE','恢复'],['CLOSED','关闭']]}[z.zoneStatus]||[])
const moreActions=z=>[...(z.zoneStatus==='DRAFT'&&z.totalSeatCount===0?[{key:'generate',label:'生成库存'}]:[]),...nextActions(z).map(action=>({key:`status:${action[0]}`,label:action[1],danger:action[0]==='CLOSED'}))]
const load=async()=>{loading.value=true;error.value='';try{const [m,z]=await Promise.all([getMatch(matchId),getAdminTicketZones(matchId)]);match.value=m.data;zones.value=z.data;staticZones.value=(await getZones(match.value.stadiumId)).data;if(selected.value){selected.value=zones.value.find(v=>v.matchZoneId===selected.value.matchZoneId)||null;if(selected.value)await loadLayout()}}catch(e){error.value=e?.message||'加载比赛票务失败，请稍后重试。'}finally{loading.value=false}}
const open=row=>{editingId.value=row?.matchZoneId||null;Object.assign(form,blank(),row?{stadiumZoneId:row.stadiumZoneId,price:Number(row.price)}:{});automaticSaleStart.value=row?.saleStartTime||match.value.saleStartTime;visible.value=true}
const save=async()=>{await formRef.value.validate();saving.value=true;try{editingId.value?await updateTicketZone(editingId.value,form):await createTicketZone(matchId,form);visible.value=false;ElMessage.success('比赛票区已保存');await load()}finally{saving.value=false}}
const transition=async(z,status,label)=>{await ElMessageBox.confirm(`确认${label}“${z.zoneName}”？`,'销售状态');operating.value=`status-${z.matchZoneId}`;try{await updateTicketZoneStatus(z.matchZoneId,status);ElMessage.success('销售状态已更新');await load()}finally{operating.value=''}}
const generate=async z=>{await ElMessageBox.confirm('库存将从当前启用的物理座位生成，生成后不可重复生成。','生成库存');operating.value=`inventory-${z.matchZoneId}`;try{const response=await generateInventory(z.matchZoneId);ElMessage.success(`已生成 ${response.data} 个比赛座位库存`);await load()}finally{operating.value=''}}
const handleMore=(command,z)=>{if(command==='generate')return generate(z);const status=command.replace('status:','');const action=nextActions(z).find(item=>item[0]===status);if(action)return transition(z,action[0],action[1])}
const selectZone=async z=>{selected.value=z;debugResult.value=null;await loadLayout()}
const loadLayout=async()=>{layout.value=(await getInventoryLayout(selected.value.matchZoneId)).data}
const toggleSeat=async seat=>{if(!['AVAILABLE','DISABLED'].includes(seat.inventoryStatus))return;await updateInventoryStatus(seat.inventoryId,seat.inventoryStatus==='AVAILABLE'?'DISABLED':'AVAILABLE');await Promise.all([loadLayout(),load()]);debugResult.value=null;ElMessage.success('比赛座位状态已更新')}
const seatLetter=seat=>({AVAILABLE:'O',LOCKED:'L',SOLD:'S',DISABLED:'X'}[seat.inventoryStatus]||'?')
const seatType=seat=>({AVAILABLE:'primary',LOCKED:'warning',SOLD:'success',DISABLED:'info'}[seat.inventoryStatus]||'info')
const runDebug=async()=>{debugging.value=true;try{debugResult.value=(await debugSeatAllocation(selected.value.matchZoneId,debugCount.value)).data}finally{debugging.value=false}}
const initializeStandard=async()=>{saving.value=true;try{await initializeStandardTicketing(matchId);standardVisible.value=false;ElMessage.success('已按默认价格和自动售票时间生成8个比赛票区及库存');await load()}finally{saving.value=false}}
onMounted(load)
</script>

<template>
  <div class="operations-page">
    <PageHeader :breadcrumb="[{label:'比赛管理',to:'/admin/matches'},{label:'比赛运营',to:`/admin/matches/${matchId}`},{label:'票务配置'}]" title="比赛票务配置" :subtitle="match.homeClubName&&match.awayClubName?`${match.homeClubName} 对阵 ${match.awayClubName}`:'管理比赛票区与服务端库存快照。'"><template #status><StatusTag v-if="match.matchStatus" :value="match.matchStatus" /></template><template #actions><el-button v-if="standardStadium&&!zones.length" :disabled="!['DRAFT','PUBLISHED'].includes(match.matchStatus)" @click="standardVisible=true">初始化标准8票区</el-button><el-button type="primary" :disabled="!['DRAFT','PUBLISHED'].includes(match.matchStatus)" @click="open()">新增比赛票区</el-button></template></PageHeader>
    <DataState :loading="loading" :error="error" :empty="!zones.length" empty-title="暂无比赛票区" empty-description="可从页面主操作新增票区；标准主场也可按既有规则初始化8个票区。" @retry="load">
      <MetricStrip :items="metrics" label="比赛票务库存摘要" />
      <el-alert class="notice" title="比赛库存是物理座位在本场比赛中的独立快照；连坐预览和算法调试均不会修改库存。" type="info" :closable="false" />
      <TableWrapper title="比赛票区" description="点击一行可查看该票区的库存快照与连坐算法测试。" label="比赛票区表格">
        <el-table :data="zones" highlight-current-row @current-change="selectZone"><el-table-column label="票区" min-width="180"><template #default="{row}"><strong>{{row.zoneName}}</strong><span class="zone-code">{{row.zoneCode}}</span></template></el-table-column><el-table-column label="类型 / 方向" min-width="130"><template #default="{row}">{{ticketTypeLabel(row.ticketType)}}<span v-if="row.zoneDirection"> · {{directionLabel(row.zoneDirection)}}</span></template></el-table-column><el-table-column label="票价" width="110" align="right"><template #default="{row}"><span class="score-nums">{{$formatMoney(row.price)}}</span></template></el-table-column><el-table-column label="运营状态" min-width="150"><template #default="{row}"><StatusTag :value="row.zoneStatus" :label="zoneStatusLabel(row.zoneStatus)" /></template></el-table-column><el-table-column label="当前销售" min-width="120"><template #default="{row}"><StatusTag :value="row.saleState" :label="saleStatusLabel(row.saleState)" /></template></el-table-column><el-table-column label="库存" min-width="210"><template #default="{row}"><span class="inventory-counts score-nums">总 {{row.totalSeatCount}} · 可售 {{row.availableSeatCount}} · 已售 {{row.soldSeatCount}}</span></template></el-table-column><el-table-column label="操作" width="170" fixed="right"><template #default="{row}"><el-button v-if="row.zoneStatus==='DRAFT'" link type="primary" @click.stop="open(row)">编辑</el-button><el-dropdown v-if="moreActions(row).length" trigger="click" :disabled="Boolean(operating)" @command="command=>handleMore(command,row)"><el-button link :loading="operating.endsWith(String(row.matchZoneId))" @click.stop aria-label="打开票区操作菜单">更多</el-button><template #dropdown><el-dropdown-menu><el-dropdown-item v-for="item in moreActions(row)" :key="item.key" :command="item.key" :class="{'danger-item':item.danger}">{{item.label}}</el-dropdown-item></el-dropdown-menu></template></el-dropdown></template></el-table-column></el-table>
      </TableWrapper>
      <CardShell v-if="selected" class="section-card" :title="`${selected.zoneName} · 比赛库存预览`" subtitle="O 可售，L 锁定，S 已售，X 停用。">
        <EmptyState v-if="!layout.length" title="尚未生成库存" description="生成该票区库存后，座位快照会显示在这里。" />
        <div v-else v-for="row in layout" :key="row.rowNo" class="seat-row"><b>{{row.rowLabel}}</b><el-button v-for="seat in row.seats" :key="seat.inventoryId" size="small" :type="seatType(seat)" :plain="seat.inventoryStatus==='AVAILABLE'" :disabled="['LOCKED','SOLD'].includes(seat.inventoryStatus)" @click="toggleSeat(seat)">{{seatLetter(seat)}} {{seat.seatLabel}}</el-button></div>
      </CardShell>
      <CardShell v-if="selected" class="section-card" title="连坐算法测试" subtitle="只读取候选结果，不修改库存。">
        <ActionToolbar title="测试条件" description="选择购票张数后查看服务端返回的候选座位。"><el-select v-model="debugCount" aria-label="购票张数"><el-option v-for="count in 4" :key="count" :label="`${count}张`" :value="count" /></el-select><template #actions><el-button type="primary" :loading="debugging" @click="runDebug">计算候选</el-button></template></ActionToolbar>
        <el-alert v-if="debugResult?.best" class="debug-result" type="success" :closable="false" :title="`最优：${debugResult.best.rowLabel}，${debugResult.best.seatLabels.join('、')}`" /><el-alert v-else-if="debugResult" class="debug-result" type="warning" :closable="false" :title="`无可用候选，当前最大连续数 ${debugResult.maxContinuousCount}`" />
        <TableWrapper v-if="debugResult" class="debug-table" compact label="连坐候选表格"><el-table :data="debugResult.candidates" size="small"><el-table-column type="index" label="排名" width="65" /><el-table-column prop="rowLabel" label="排" /><el-table-column label="座位"><template #default="{row}">{{row.startSeatNo}} - {{row.endSeatNo}}</template></el-table-column><el-table-column prop="centerDistance" label="中线距离" /><el-table-column prop="remainingFragmentCount" label="剩余碎片" /><el-table-column prop="maxRemainingContinuousLength" label="最大剩余连续" /></el-table></TableWrapper>
      </CardShell>
    </DataState>
    <el-dialog v-model="visible" :title="editingId?'编辑比赛票区':'新增比赛票区'" width="560px"><el-alert title="每场比赛开始日期前14天晚上8点开售，停售时间固定为赛前1小时。" type="info" :closable="false" /><el-form ref="formRef" :model="form" :rules="rules" label-width="120px" class="zone-form"><el-form-item label="静态票区" prop="stadiumZoneId"><el-select v-model="form.stadiumZoneId" :disabled="editingId&&zones.find(value=>value.matchZoneId===editingId)?.totalSeatCount>0" aria-label="静态票区"><el-option v-for="zone in staticZones" :key="zone.stadiumZoneId" :label="`${zone.zoneName}（${zone.zoneCode}）`" :value="zone.stadiumZoneId" :disabled="zone.zoneStatus!=='ACTIVE'" /></el-select></el-form-item><el-form-item label="票价" prop="price"><el-input-number v-model="form.price" :min="0" :precision="2" aria-label="票价" /></el-form-item><el-form-item label="自动开售时间"><el-input :model-value="automaticSaleStart" readonly /></el-form-item><el-form-item label="自动停售时间"><el-input :model-value="match.saleEndTime" readonly /></el-form-item></el-form><template #footer><el-button :disabled="saving" @click="visible=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存票区</el-button></template></el-dialog>
    <el-dialog v-model="standardVisible" title="初始化标准8票区" width="500px"><el-alert title="系统将使用主场默认价格和赛季自动售票时间，一次生成8个比赛票区及库存。" type="info" :closable="false" /><template #footer><el-button :disabled="saving" @click="standardVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="initializeStandard">确认初始化</el-button></template></el-dialog>
  </div>
</template>

<style scoped>
.notice,.section-card{margin-top:var(--space-4)}.zone-code{display:block;margin-top:var(--space-1);color:var(--color-text-muted);font-size:var(--font-size-xs)}.inventory-counts{color:var(--color-text-secondary);font-size:var(--font-size-xs)}.seat-row{display:flex;align-items:center;flex-wrap:wrap;gap:var(--space-2);margin:var(--space-3) 0}.seat-row>b{width:55px}.debug-result,.debug-table{margin-top:var(--space-4)}.zone-form{margin-top:var(--space-4)}.zone-form :deep(.el-select){width:100%}.danger-item{color:var(--color-danger)}
</style>
