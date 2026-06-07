<template>
    <showCard title="用户管理" subtitle="在这里管理web用户"  >

    
  <div class="management-container">
    <header class="management-header">
      <div class="nav-wrapper">

        
        <div class="dropdown">
          <button class="dropdown-trigger">
            {{ currentLabel }}
            <span class="arrow">↓</span>
          </button>
          
          <div class="dropdown-menu">
            <router-link to="/adminPanel/userM/accountM" class="menu-item">
              用户修改
            </router-link>
            <router-link to="/adminPanel/userM/roleM" class="menu-item">
              角色分配
            </router-link>
          </div>
        </div>
      </div>
    </header>

    <main class="management-content">
      <transition name="fade" mode="out-in">
        <router-view />
      </transition>
    </main>
  </div>
  
  </showCard>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import showCard from "@/components/commonShowPanel.vue"
const route = useRoute()

// 根据当前路由路径显示对应的标题
const currentLabel = computed(() => {
  if (route.path.includes('accountM')) return '用户修改'
  if (route.path.includes('roleM')) return '角色分配'
  return '选择功能'
})
</script>

<style scoped>
/* 容器设计：采用毛玻璃背景感 */
.management-container {
  min-height: 100vh;
  padding: 20px;
  border-radius: 12px; 
  background-color: #f5f5f7; /* Apple 典型的浅灰背景 */
}

.management-header {
  margin-bottom: 24px;
}

.nav-wrapper {
  display: flex;
  align-items: center;
  gap: 20px;
}

.title {
  font-size: 24px;
  font-weight: 600;
  color: #1d1d1f;
}

/* 下拉菜单样式 */
.dropdown {
  position: relative;
  display: inline-block;
}

.dropdown-trigger {
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px); /* 毛玻璃效果 */
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 12px; /* R角设计 */
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.dropdown-trigger:hover {
  background: #ffffff;
}

.dropdown-menu {
  position: absolute;
  top: 110%;
  left: 0;
  min-width: 140px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  opacity: 0;
  visibility: hidden;
  transform: translateY(-10px);
  transition: all 0.3s ease;
  z-index: 10;
  padding: 8px;
}

.dropdown:hover .dropdown-menu {
  opacity: 1;
  visibility: visible;
  transform: translateY(0);
}

.menu-item {
  display: block;
  padding: 10px 12px;
  color: #1d1d1f;
  text-decoration: none;
  font-size: 14px;
  border-radius: 8px;
  transition: background 0.2s;
}

.menu-item:hover {
  background: #0071e3; /* Apple 经典蓝色 */
  color: #fff;
}

/* 子页面切换动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.management-content {
  background: #ffffff;
  border-radius: 18px; /* 较大的圆角 */
  padding: 24px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.02);
  min-height: 400px;
}
</style>