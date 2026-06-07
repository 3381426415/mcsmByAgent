<template>
  <div class="apple-card-wrapper">
    <div class="apple-card">
      <div v-if="imgSrc" class="card-image-section">
        <img :src="imgSrc" :alt="imgAlt || 'Card image'" />
      </div>

      <div class="card-content-section">
        <div v-if="title || subtitle" class="card-header-text">
          <h3 v-if="title" class="card-title">{{ title }}</h3>
          <p v-if="subtitle" class="card-subtitle">{{ subtitle }}</p>
        </div>
        
        <slot></slot>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: String,
  subtitle: String,
  imgSrc: String,
  imgAlt: String
})
</script>

<style scoped>
.apple-card-wrapper {
  width: 100%;
  padding: 10px; /* 增加一个外边距，防止投影被切断 */
}

.apple-card {
  background-color: #ffffff;
  border-radius: 20px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.3s ease;
  width: 100%;
}

.apple-card:hover {
  /* 如果你的卡片包裹的是整个页面布局，请务必删除下面这一行 transform */
  /* transform: translateY(-2px); */ 
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
}

.card-image-section img {
  width: 100%;
  height: auto;
  display: block;
  object-fit: cover;
}

.card-content-section {
  padding: 24px; /* 适中的内边距 */
}

.card-header-text {
  margin-bottom: 20px;
}

.card-title {
  font-size: 20px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0 0 8px 0;
}

.card-subtitle {
  font-size: 14px;
  color: #6e6e73;
}
</style>