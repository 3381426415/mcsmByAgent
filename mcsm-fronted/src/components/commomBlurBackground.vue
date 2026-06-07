<template>
  <div 
    class="apple-card-wrapper" 
    :style="{ 
      '--width': width, 
      '--height': height, 
      '--radius': radius + 'px',
    }"
  >
    <div class="card-background"></div>
    
    <div class="card-border"></div>

    <div class="card-content" :style="{ padding: padding + 'px' }">
      <slot></slot>
    </div>
  </div>
</template>

<script setup>
defineProps({
  width: { type: String, default: '400px' },
  height: { type: String, default: 'auto' },
  radius: { type: Number, default: 22 }, // 苹果官网卡片 R 角通常在 18-24 之间，44 略大，可根据喜好调回
  padding: { type: Number, default: 40 }, // 官网风格通常留白更多
})
// 删除了不再需要的 opacity 属性，因为官网卡片是不透明的
</script>

<style scoped>
.apple-card-wrapper {
  position: relative;
  width: var(--width);
  height: var(--height);
  border-radius: var(--radius);
  display: flex;
  flex-direction: column;
  /* 苹果官网风格的核心：微妙的分层阴影 */
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.08), 
              0 1px 2px rgba(0, 0, 0, 0.04);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

/* 模拟官网的交互感：鼠标移入稍微浮起 */
.apple-card-wrapper:hover {
  transform: translateY(-2px);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.12);
}

.card-background {
  position: absolute;
  inset: 0;
  background: #ffffff; /* 纯白色实色 */
  border-radius: var(--radius);
  z-index: 1;
}

.card-border {  
  position: absolute;
  inset: 0;
  z-index: 2;
  border-radius: var(--radius);
  /* 苹果官网那种极细的浅灰边框 */
  border: 1px solid rgba(0, 0, 0, 0.1); 
  pointer-events: none;
}

.card-content {
  position: relative;
  z-index: 3;
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center; /* 内容默认居中，符合登录/注册布局 */
}
</style>