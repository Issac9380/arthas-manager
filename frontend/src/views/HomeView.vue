<template>
  <div class="home">
    <el-row :gutter="24" class="stat-row">
      <el-col :span="6" v-for="stat in stats" :key="stat.label">
        <el-card class="stat-card" shadow="never">
          <div class="stat-inner">
            <el-icon :size="40" :color="stat.color"><component :is="stat.icon" /></el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stat.value }}</div>
              <div class="stat-label">{{ stat.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="24" class="intro-row">
      <el-col :span="16">
        <el-card shadow="never">
          <template #header>
            <span class="card-title">快速开始</span>
          </template>
          <el-steps direction="vertical" :active="0" finish-status="success">
            <el-step title="连接集群" description="在「集群管理」页面选择目标 Namespace 和 Pod" />
            <el-step title="部署工具" description="一键将 Arthas（及可选 JDK）上传到容器" />
            <el-step title="附加进程" description="选择容器中的 Java 进程并点击「连接」" />
            <el-step title="可视化诊断" description="通过表单界面执行 JVM 分析、方法追踪、堆转储等操作" />
          </el-steps>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <template #header><span class="card-title">支持的诊断命令</span></template>
          <el-tag
            v-for="cmd in commands"
            :key="cmd"
            class="cmd-tag"
            type="info"
            effect="plain"
          >{{ cmd }}</el-tag>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
const stats = [
  { label: '支持命令', value: '11', icon: 'Operation',   color: '#409EFF' },
  { label: '部署方式', value: 'K8s', icon: 'Grid',        color: '#67C23A' },
  { label: '通信协议', value: 'HTTP', icon: 'Connection',  color: '#E6A23C' },
  { label: '实时推送', value: 'WS',   icon: 'Monitor',     color: '#F56C6C' }
]

const commands = [
  'Dashboard', 'JVM Info', 'Thread', 'Watch',
  'Trace', 'Monitor', 'Stack', 'Heap Dump',
  'JAD 反编译', 'SC 查找类', 'SM 查找方法',
  'OGNL 表达式', 'Classloader'
]
</script>

<style scoped>
.home { display: flex; flex-direction: column; gap: 24px; }
.stat-row { margin-bottom: 0; }
.stat-card { border-radius: 8px; }
.stat-inner { display: flex; align-items: center; gap: 16px; padding: 8px 0; }
.stat-info { display: flex; flex-direction: column; gap: 4px; }
.stat-value { font-size: 28px; font-weight: 700; color: #303133; }
.stat-label { font-size: 13px; color: #909399; }
.card-title { font-weight: 600; font-size: 15px; }
.cmd-tag { margin: 4px; }
</style>
