<template>
  <el-config-provider>
    <el-container class="app-layout">
      <!-- Sidebar navigation -->
      <el-aside width="220px" class="sidebar">
        <div class="logo">
          <el-icon size="28" color="#409EFF"><Monitor /></el-icon>
          <span class="logo-text">Arthas Manager</span>
        </div>

        <el-menu
          :default-active="activeRoute"
          router
          background-color="#1e2a3a"
          text-color="#c0c4cc"
          active-text-color="#409EFF"
        >
          <el-menu-item index="/">
            <el-icon><House /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <el-menu-item index="/cluster">
            <el-icon><Grid /></el-icon>
            <span>集群管理</span>
          </el-menu-item>
          <el-menu-item index="/diagnosis">
            <el-icon><Cpu /></el-icon>
            <span>诊断中心</span>
          </el-menu-item>
          <el-menu-item index="/tools">
            <el-icon><Box /></el-icon>
            <span>工具管理</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-container>
        <!-- Top bar -->
        <el-header class="header">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentPageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
          <div class="header-right">
            <el-tag type="success" size="small">在线</el-tag>
          </div>
        </el-header>

        <!-- Page content -->
        <el-main class="main-content">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </el-config-provider>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const activeRoute = computed(() => route.path)
const currentPageTitle = computed(() => route.meta?.title ?? '')
</script>

<style>
* { box-sizing: border-box; margin: 0; padding: 0; }

body {
  font-family: 'PingFang SC', 'Helvetica Neue', Arial, sans-serif;
  background: #f0f2f5;
}

.app-layout { height: 100vh; }

.sidebar {
  background: #1e2a3a;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 20px;
  border-bottom: 1px solid #2d3d52;
}

.logo-text {
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.el-menu { border-right: none !important; }

.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-bottom: 1px solid #e8eaec;
  box-shadow: 0 1px 4px rgba(0,0,0,.08);
}

.header-right { display: flex; align-items: center; gap: 12px; }

.main-content {
  padding: 24px;
  overflow-y: auto;
}
</style>
