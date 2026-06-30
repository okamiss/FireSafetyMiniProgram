<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Refresh } from '@element-plus/icons-vue'
import { PieChart, type PieSeriesOption } from 'echarts/charts'
import { LegendComponent, TooltipComponent, type LegendComponentOption, type TooltipComponentOption } from 'echarts/components'
import { init, use, type ComposeOption, type ECharts } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { getDashboardSummary, type DashboardSummary } from '../api/dashboard'
import { exportRepairs, exportTrainingRecords } from '../api/exports'
import type { RepairStatus } from '../api/repairs'

use([PieChart, LegendComponent, TooltipComponent, CanvasRenderer])
type ChartOption = ComposeOption<PieSeriesOption | LegendComponentOption | TooltipComponentOption>

const loading = ref(false)
const exporting = ref<'repairs' | 'training' | ''>('')
const summary = ref<DashboardSummary | null>(null)
const chartElement = ref<HTMLDivElement | null>(null)
let chart: ECharts | null = null
let resizeObserver: ResizeObserver | null = null

const statusMeta: Array<{ key: RepairStatus; label: string; color: string }> = [
  { key: 'PENDING_ACCEPTANCE', label: '待受理', color: '#d97706' },
  { key: 'PROCESSING', label: '处理中', color: '#2563eb' },
  { key: 'COMPLETED', label: '已完成', color: '#059669' },
  { key: 'CLOSED', label: '已关闭', color: '#64748b' },
]
const activeRepairs = computed(() => (summary.value?.repairStatusCounts.PENDING_ACCEPTANCE || 0)
  + (summary.value?.repairStatusCounts.PROCESSING || 0))
const metrics = computed(() => [
  { label: '报修总量', value: String(summary.value?.repairTotal || 0), note: '当前全部报修工单' },
  { label: '待处理工单', value: String(activeRepairs.value), note: '待受理与处理中' },
  { label: '培训完成率', value: `${formatRate(summary.value?.trainingCompletionRate)}%`, note: '已作答 / 已分配' },
  { label: '培训通过率', value: `${formatRate(summary.value?.trainingPassRate)}%`, note: '已通过 / 已作答' },
])

async function load() {
  loading.value = true
  try {
    summary.value = await getDashboardSummary()
    await nextTick()
    renderChart()
  } catch { ElMessage.error('看板数据加载失败') }
  finally { loading.value = false }
}

function renderChart() {
  if (!chartElement.value || !summary.value?.repairTotal) {
    chart?.dispose()
    chart = null
    return
  }
  chart ||= init(chartElement.value)
  const option: ChartOption = {
    tooltip: { trigger: 'item', formatter: '{b}<br/>{c} 单（{d}%）' },
    legend: { orient: 'vertical', right: 16, top: 'middle' },
    series: [{
      type: 'pie', radius: ['48%', '72%'], center: ['36%', '50%'], avoidLabelOverlap: true,
      label: { formatter: '{b}\n{c} 单' },
      data: statusMeta.map(item => ({
        name: item.label, value: summary.value?.repairStatusCounts[item.key] || 0,
        itemStyle: { color: item.color },
      })),
    }],
  }
  chart.setOption(option, true)
}

async function runExport(type: 'repairs' | 'training') {
  exporting.value = type
  try {
    if (type === 'repairs') await exportRepairs()
    else await exportTrainingRecords()
    ElMessage.success('导出文件已生成')
  } catch { ElMessage.error('数据导出失败') }
  finally { exporting.value = '' }
}

function formatRate(value?: number) { return Number(value || 0).toFixed(2).replace(/\.00$/, '') }

onMounted(() => {
  load()
  if (chartElement.value) {
    resizeObserver = new ResizeObserver(() => chart?.resize())
    resizeObserver.observe(chartElement.value)
  }
})
onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  chart?.dispose()
})
</script>

<template>
  <div class="dashboard-page">
    <div class="section-toolbar dashboard-toolbar">
      <div><h2>业务概览</h2><p>报修与培训的当前统计口径</p></div>
      <div>
        <el-button :icon="Download" :loading="exporting === 'repairs'" @click="runExport('repairs')">导出报修</el-button>
        <el-button :icon="Download" :loading="exporting === 'training'" @click="runExport('training')">导出培训</el-button>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <section class="dashboard-metrics" aria-label="核心指标">
      <div v-for="item in metrics" :key="item.label" class="metric-item">
        <span>{{ item.label }}</span><strong>{{ item.value }}</strong><small>{{ item.note }}</small>
      </div>
    </section>

    <section class="work-section dashboard-chart-section">
      <div class="section-toolbar"><div><h2>报修状态分布</h2><p>按当前工单状态统计</p></div></div>
      <div v-if="!loading && !summary?.repairTotal" class="empty-chart">暂无报修数据</div>
      <div v-show="summary?.repairTotal" ref="chartElement" class="repair-chart" />
    </section>
  </div>
</template>

<style scoped>
.dashboard-page { display: grid; gap: 20px; }
.dashboard-toolbar { padding: 0 2px; }
.dashboard-toolbar > div:last-child { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 10px; }
.dashboard-metrics { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); border: 1px solid #dce3eb; background: #fff; }
.metric-item { min-width: 0; padding: 24px; border-right: 1px solid #dce3eb; }
.metric-item:last-child { border-right: 0; }
.metric-item span, .metric-item small { display: block; color: #667085; }
.metric-item strong { display: block; margin: 12px 0 8px; color: #17212b; font-size: 30px; letter-spacing: 0; }
.metric-item small { font-size: 13px; }
.dashboard-chart-section { min-height: 390px; }
.repair-chart, .empty-chart { width: 100%; height: 300px; }
.empty-chart { display: grid; place-items: center; color: #98a2b3; }
@media (max-width: 1000px) { .dashboard-metrics { grid-template-columns: repeat(2, minmax(0, 1fr)); }.metric-item:nth-child(2) { border-right: 0; }.metric-item:nth-child(-n+2) { border-bottom: 1px solid #dce3eb; } }
</style>
