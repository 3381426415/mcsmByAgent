<template>
  <el-row :gutter="16" class="dashboard-row">
    <el-col :xs="12" :sm="6">
      <div class="dash-card">
        <div class="dash-label">CPU</div>
        <el-progress type="dashboard" :percentage="metrics.cpuUsage" :color="cpuColor" :stroke-width="8" size="100" />
        <div class="dash-desc">{{ metrics.cpuCores || 8 }} 核</div>
      </div>
    </el-col>
    <el-col :xs="12" :sm="6">
      <div class="dash-card">
        <div class="dash-label">内存</div>
        <el-progress type="dashboard" :percentage="metrics.memUsage" :status="metrics.memUsage > 80 ? 'exception' : ''" :stroke-width="8" size="100" />
        <div class="dash-desc">{{ metrics.memUsed }} / {{ metrics.memTotal }}</div>
      </div>
    </el-col>
    <el-col :xs="12" :sm="6">
      <div class="dash-card">
        <div class="dash-label">下载</div>
        <div class="dash-value">{{ metrics.netIn }} KB/s</div>
      </div>
    </el-col>
    <el-col :xs="12" :sm="6">
      <div class="dash-card">
        <div class="dash-label">上传</div>
        <div class="dash-value">{{ metrics.netOut }} KB/s</div>
      </div>
    </el-col>
  </el-row>
</template>

<script setup>
import { ref, onMounted, onUnmounted, onActivated, onDeactivated, reactive } from 'vue';
import request from '@/utils/request';

const metrics = reactive({
  cpuUsage: 0, cpuCores: 8,
  memUsage: 0, memTotal: '0 GB', memUsed: '0 GB',
  netIn: 0, netOut: 0
});

const cpuColor = [
  { color: '#67c23a', percentage: 60 },
  { color: '#e6a23c', percentage: 80 },
  { color: '#f56c6c', percentage: 100 }
];

let timer = null;

const fetchMetrics = async () => {
  try {
    const res = await request.get('/api/server/metrics', { silent: true, params: { t: Date.now() } });
    const data = res.data || res;
    if (data) Object.assign(metrics, data);
  } catch (e) { /* silent */ }
};

onMounted(() => {
  fetchMetrics();
  timer = setInterval(fetchMetrics, 3000);
});

onActivated(() => {
  if (!timer) {
    fetchMetrics();
    timer = setInterval(fetchMetrics, 3000);
  }
});

onDeactivated(() => {
  if (timer) { clearInterval(timer); timer = null; }
});

onUnmounted(() => {
  if (timer) clearInterval(timer);
});
</script>

<style scoped>
.dashboard-row { margin-bottom: 20px; text-align: center; }

.dash-card {
  background: #fff; border-radius: 16px; padding: 16px 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.04); margin-bottom: 16px;
  display: flex; flex-direction: column; align-items: center; gap: 8px;
}

.dash-label { font-weight: 600; font-size: 13px; color: #606266; }

.dash-value { font-size: 20px; font-weight: 700; color: #303133; }

.dash-desc { font-size: 12px; color: #909399; margin-top: 4px; }
</style>
