<template>
  <div
    :class="[
      'mc-card',
      { 'mc-card--hoverable': hoverable },
      { 'mc-card--elevated': elevated },
      `mc-card--${variant}`
    ]"
    @click="$emit('click', $event)"
  >
    <div v-if="$slots.header || title" class="mc-card__header">
      <slot name="header">
        <h3 v-if="title" class="mc-card__title">{{ title }}</h3>
      </slot>
    </div>
    <div class="mc-card__body">
      <slot></slot>
    </div>
    <div v-if="$slots.footer" class="mc-card__footer">
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
  variant: {
    type: String,
    default: 'default',
    validator: (value) => ['default', 'dirt', 'stone', 'wood', 'grass'].includes(value)
  },
  hoverable: {
    type: Boolean,
    default: false
  },
  elevated: {
    type: Boolean,
    default: false
  }
})

defineEmits(['click'])
</script>

<style scoped>
.mc-card {
  position: relative;
  background: var(--mc-bg-card);
  border: 3px solid var(--mc-border-dark);
  border-radius: var(--mc-radius-lg);
  padding: 0;
  overflow: hidden;
  transition: all var(--mc-transition-normal);
  box-shadow: var(--mc-shadow-md);
}

.mc-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 6px;
  background: linear-gradient(90deg,
    var(--mc-dirt-brown),
    var(--mc-wood-brown),
    var(--mc-dirt-brown)
  );
  z-index: 1;
}

/* 颜色变体 */
.mc-card--dirt {
  border-color: var(--mc-dirt-brown);
}

.mc-card--dirt::before {
  background: var(--mc-dirt-brown);
}

.mc-card--stone {
  border-color: var(--mc-stone-gray);
}

.mc-card--stone::before {
  background: var(--mc-stone-gray);
}

.mc-card--wood {
  border-color: var(--mc-wood-brown);
}

.mc-card--wood::before {
  background: var(--mc-wood-brown);
}

.mc-card--grass {
  border-color: var(--mc-emerald-green);
}

.mc-card--grass::before {
  background: linear-gradient(90deg,
    var(--mc-emerald-green),
    var(--mc-bg-secondary),
    var(--mc-emerald-green)
  );
}

/* 悬浮效果 */
.mc-card--hoverable {
  cursor: pointer;
}

.mc-card--hoverable:hover {
  transform: translateY(-4px);
  box-shadow: var(--mc-shadow-lg);
}

.mc-card--hoverable:active {
  transform: translateY(-2px);
}

/* 提升效果 */
.mc-card--elevated {
  box-shadow: var(--mc-shadow-lg);
}

.mc-card--elevated:hover {
  box-shadow: var(--mc-shadow-xl);
}

/* 头部样式 */
.mc-card__header {
  padding: 16px 20px;
  border-bottom: 2px solid var(--mc-border);
  background: rgba(0, 0, 0, 0.02);
}

.mc-card__title {
  font-family: var(--mc-font-display);
  font-size: var(--mc-text-lg);
  font-weight: var(--mc-font-semibold);
  color: var(--mc-text-primary);
  margin: 0;
  line-height: 1.4;
}

/* 内容区域 */
.mc-card__body {
  padding: 20px;
}

/* 底部样式 */
.mc-card__footer {
  padding: 16px 20px;
  border-top: 2px solid var(--mc-border);
  background: rgba(0, 0, 0, 0.02);
}

/* 特殊效果 */
.mc-card--hoverable::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg,
    rgba(255, 255, 255, 0.1) 0%,
    rgba(255, 255, 255, 0) 100%
  );
  pointer-events: none;
  opacity: 0;
  transition: opacity var(--mc-transition-normal);
}

.mc-card--hoverable:hover::after {
  opacity: 1;
}
</style>
