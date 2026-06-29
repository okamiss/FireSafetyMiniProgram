<script setup lang="ts">
const metrics = [
  { label: '报修总量', value: '0', note: '报修模块接入后显示' },
  { label: '处理中工单', value: '0', note: '按企业数据范围统计' },
  { label: '培训完成率', value: '0%', note: '基于已发布培训任务' },
  { label: '培训通过率', value: '0%', note: '默认 60 分及格' },
]
</script>
<template><section class="page-grid"><el-card v-for="item in metrics" :key="item.label" shadow="never"><span class="metric-label">{{ item.label }}</span><strong class="metric-value">{{ item.value }}</strong><p>{{ item.note }}</p></el-card></section></template>
