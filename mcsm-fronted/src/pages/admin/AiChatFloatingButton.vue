<template>
  <div class="ai-chat-floating">
    <el-button
      class="floating-btn"
      type="primary"
      circle
      @click="drawerVisible = true"
      v-show="!drawerVisible"
    >
      <span class="ai-text">AI</span>
    </el-button>

    <el-drawer
      v-model="drawerVisible"
      title="AI 助手"
      direction="rtl"
      size="450px"
      :close-on-click-modal="false"
    >
      <AIchat />
    </el-drawer>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import AIchat from '@/pages/admin/admin-menu/serverM/agentChat.vue';

const drawerVisible = ref(false);
</script>

<style scoped>
.ai-chat-floating {
  position: fixed;
  top: 16px;
  right: 32px;
  z-index: 1100;
}

.floating-btn {
  width: 56px;
  height: 56px;
  box-shadow: 0 4px 16px rgba(0, 113, 227, 0.35);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.floating-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 24px rgba(0, 113, 227, 0.5);
}

.ai-text {
  font-weight: 700;
  font-size: 20px;
  letter-spacing: -0.5px;
}
</style>
