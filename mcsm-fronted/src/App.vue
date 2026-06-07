<template>
  

  <div v-if="isMobile" class="mobile-only">
    <div class="message-box">
      <h2>📱 请在电脑上访问</h2>
      <p>本网站暂不支持移动设备，请使用桌面浏览器打开。</p>
      <p>推荐使用 Chrome、Edge 或 Safari（Mac）访问。</p>
    </div>
  </div>
  <div v-else>
<nav>
    <router-link to="/"></router-link>
   
  </nav>
   <router-view />
  </div>
  
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue';

function isMobileDevice() {
 if ('userAgentData' in navigator) {
    return (navigator as any).userAgentData.mobile;
  }

  // 2. 针对 iOS / Safari 的特性检测
  // 尤其是 iPadOS，现在默认 UA 是 Mac，必须通过多点触控检测
  const isTouch = navigator.maxTouchPoints > 0 || 'ontouchstart' in window;

  // 3. 经典正则保底 (处理 Firefox 和旧版浏览器)
  const ua = navigator.userAgent;
  const isMobileUA = /Mobi|Android|iPhone|iPad|iPod/i.test(ua);

  // 综合判断：是移动端 UA，或者是支持触摸的小屏幕设备
  return isMobileUA || (isTouch && window.innerWidth <= 1024);
}

const isMobile = ref(false);

onMounted(() => {
  // 在 mounted 中检测（确保 window 对象可用）
  isMobile.value = isMobileDevice();
});
</script>
<style scoped>


.message-box {
  text-align: center;
  max-width: 80%;
  padding: 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.message-box h2 {
  color: #333;
  margin-bottom: 16px;
}

.message-box p {
  color: #666;
  line-height: 1.6;
  margin: 8px 0;
}

.mobile-only {
  width: 100vw;
  height: 100vh;
  background: #f8f9fa;
  display: flex;
  justify-content: center;
  align-items: center;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}


</style>
