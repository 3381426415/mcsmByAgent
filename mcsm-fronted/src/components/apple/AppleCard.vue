<template>
  <div
    :class="[
      'apple-card',
      { 'apple-card--hoverable': hoverable },
      { 'apple-card--elevated': elevated },
      { 'apple-card--compact': compact },
      `apple-card--${variant}`
    ]"
    @click="$emit('click', $event)"
  >
    <div v-if="$slots.header || title" class="apple-card__header">
      <slot name="header">
        <div class="apple-card__header-content">
          <h3 v-if="title" class="apple-card__title">{{ title }}</h3>
          <p v-if="subtitle" class="apple-card__subtitle">{{ subtitle }}</p>
        </div>
      </slot>
    </div>
    <div class="apple-card__body">
      <slot></slot>
    </div>
    <div v-if="$slots.footer" class="apple-card__footer">
      <slot name="footer"></slot>
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: {
    type: String,
    default: ''
  },
  subtitle: {
    type: String,
    default: ''
  },
  variant: {
    type: String,
    default: 'default',
    validator: (value) => ['default', 'bordered', 'filled', 'elevated'].includes(value)
  },
  hoverable: {
    type: Boolean,
    default: false
  },
  elevated: {
    type: Boolean,
    default: false
  },
  compact: {
    type: Boolean,
    default: false
  }
})

defineEmits(['click'])
</script>

<style scoped>
.apple-card {
  position: relative;
  background: var(--apple-card-bg);
  border: var(--apple-card-border);
  border-radius: var(--apple-radius-lg);
  overflow: hidden;
  transition: all var(--apple-transition-normal);
}

/* 颜色变体 */
.apple-card--bordered {
  border: 1px solid var(--apple-border);
}

.apple-card--filled {
  background: var(--apple-bg-secondary);
  border: none;
}

.apple-card--elevated {
  box-shadow: var(--apple-shadow-lg);
  border: none;
}

/* 悬浮效果 */
.apple-card--hoverable {
  cursor: pointer;
}

.apple-card--hoverable:hover {
  transform: translateY(-2px);
  box-shadow: var(--apple-shadow-md);
}

.apple-card--hoverable:active {
  transform: translateY(0);
}

/* 提升效果 */
.apple-card.elevated {
  box-shadow: var(--apple-shadow-lg);
}

.apple-card.elevated:hover {
  box-shadow: var(--apple-shadow-xl);
}

/* 头部样式 */
.apple-card__header {
  padding: 20px 24px;
  border-bottom: 1px solid var(--apple-divider);
}

.apple-card__header-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.apple-card__title {
  font-family: var(--apple-font-display);
  font-size: var(--apple-text-lg);
  font-weight: var(--apple-font-semibold);
  color: var(--apple-text-primary);
  margin: 0;
  line-height: var(--apple-leading-tight);
  letter-spacing: var(--apple-tracking-tight);
}

.apple-card__subtitle {
  font-size: var(--apple-text-sm);
  color: var(--apple-text-secondary);
  margin: 0;
  line-height: var(--apple-leading-normal);
}

/* 内容区域 */
.apple-card__body {
  padding: 24px;
}

.apple-card--compact .apple-card__body {
  padding: 16px;
}

/* 底部样式 */
.apple-card__footer {
  padding: 16px 24px;
  border-top: 1px solid var(--apple-divider);
  background: rgba(0, 0, 0, 0.01);
}

/* 毛玻璃效果 */
.apple-card.glass {
  background: var(--apple-glass-bg);
  backdrop-filter: var(--apple-backdrop-blur);
  -webkit-backdrop-filter: var(--apple-backdrop-blur);
  border: var(--apple-glass-border);
}

/* 紧凑模式 */
.apple-card--compact .apple-card__header {
  padding: 16px 20px;
}

.apple-card--compact .apple-card__footer {
  padding: 12px 20px;
}

/* 交互状态 */
.apple-card--hoverable {
  transition: all var(--apple-transition-normal);
}

.apple-card--hoverable:hover {
  border-color: var(--apple-border-hover);
}
</style>
