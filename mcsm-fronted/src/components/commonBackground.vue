<template>
  <div class="login-bg-container">
    <div class="login-bg-image"></div>
    <div class="bg-overlay"></div>
  </div>
</template>

<script setup>
import defaultPg from '@/assets/images/login.png'

defineProps({
  imgSrc: {
    type: String,
    default: defaultPg
  }
})
</script>

<style scoped>
.login-bg-container {
  position: fixed;
  inset: 0;
  z-index: -1;
  overflow: hidden;
  background-color: #000; /* 防止图片加载前的白闪 */
}

.login-bg-image {
  position: absolute;
  inset: 0;
  /* 使用 v-bind 绑定图片 */
  background-image: v-bind('`url("${imgSrc}")`');
  background-repeat: no-repeat;
  background-position: center center;
  background-size: cover;

  filter: brightness(0.85); 
  animation: fadeIn 1.2s ease-out;

  will-change: transform, opacity;
  
  /* 魔法属性：强制开启 3D 渲染模式（即便我们只做 2D 缩放） */
  transform: translateZ(0);
  
  /* 优化模糊：如果你一定要保留模糊动画 */
  /* 这一行能让 GPU 更聪明地处理滤镜 */
  backface-visibility: hidden;
  perspective: 1000px;
}

.bg-overlay {
  position: absolute;
  inset: 0;
  /* 增加暗色渐变，中心稍亮，边缘稍暗，增加电影感 */
  background: radial-gradient(circle at center, rgba(0,0,0,0) 0%, rgba(0,0,0,0.3) 100%),
              linear-gradient(to bottom, rgba(0,0,0,0.2), transparent);
  z-index: 1;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: scale(1.05); /* 稍微从放大状态缩回，更有动感 */
    filter: blur(10px) brightness(0.5);
  }
  to {
    opacity: 1;
    transform: scale(1);
    filter: blur(0) brightness(0.85);
  }
}
</style>