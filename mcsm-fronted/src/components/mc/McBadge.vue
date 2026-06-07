<template>
  <span
    :class="[
      'mc-badge',
      `mc-badge--${variant}`,
      { 'mc-badge--dot': dot },
      { 'mc-badge--pulse': pulse }
    ]"
  >
    <span v-if="dot" class="mc-badge__dot"></span>
    <slot>{{ text }}</slot>
  </span>
</template>

<script setup>
defineProps({
  variant: {
    type: String,
    default: 'default',
    validator: (value) => ['default', 'success', 'warning', 'danger', 'info', 'rare', 'epic', 'legendary'].includes(value)
  },
  text: {
    type: [String, Number],
    default: ''
  },
  dot: {
    type: Boolean,
    default: false
  },
  pulse: {
    type: Boolean,
    default: false
  }
})
</script>

<style scoped>
.mc-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: var(--mc-radius-full);
  font-family: var(--mc-font-display);
  font-size: var(--mc-text-xs);
  font-weight: var(--mc-font-semibold);
  white-space: nowrap;
  line-height: 1;
}

/* 颜色变体 */
.mc-badge--default {
  background: var(--mc-bg-tertiary);
  color: var(--mc-text-light);
}

.mc-badge--success {
  background: rgba(23, 221, 98, 0.15);
  color: var(--mc-emerald-green);
  border: 1px solid rgba(23, 221, 98, 0.3);
}

.mc-badge--warning {
  background: rgba(252, 219, 5, 0.15);
  color: var(--mc-gold-dark);
  border: 1px solid rgba(252, 219, 5, 0.3);
}

.mc-badge--danger {
  background: rgba(255, 0, 0, 0.1);
  color: var(--mc-redstone-red);
  border: 1px solid rgba(255, 0, 0, 0.2);
}

.mc-badge--info {
  background: rgba(63, 118, 228, 0.15);
  color: var(--mc-water-blue);
  border: 1px solid rgba(63, 118, 228, 0.3);
}

/* MC 稀有度变体 */
.mc-badge--rare {
  background: linear-gradient(135deg, rgba(74, 237, 217, 0.2), rgba(74, 237, 217, 0.1));
  color: var(--mc-diamond-cyan);
  border: 1px solid rgba(74, 237, 217, 0.4);
  box-shadow: 0 0 8px rgba(74, 237, 217, 0.2);
}

.mc-badge--epic {
  background: linear-gradient(135deg, rgba(155, 89, 182, 0.2), rgba(155, 89, 182, 0.1));
  color: var(--mc-purple);
  border: 1px solid rgba(155, 89, 182, 0.4);
  box-shadow: 0 0 8px rgba(155, 89, 182, 0.2);
}

.mc-badge--legendary {
  background: linear-gradient(135deg, rgba(252, 219, 5, 0.2), rgba(252, 219, 5, 0.1));
  color: var(--mc-gold-dark);
  border: 1px solid rgba(252, 219, 5, 0.4);
  box-shadow: 0 0 8px rgba(252, 219, 5, 0.2);
  animation: legendaryGlow 2s ease-in-out infinite;
}

@keyframes legendaryGlow {
  0%, 100% {
    box-shadow: 0 0 8px rgba(252, 219, 5, 0.2);
  }
  50% {
    box-shadow: 0 0 16px rgba(252, 219, 5, 0.4);
  }
}

/* 圆点样式 */
.mc-badge__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

/* 脉冲动画 */
.mc-badge--pulse .mc-badge__dot {
  animation: pulseDot 2s ease-in-out infinite;
}

@keyframes pulseDot {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(1.5);
  }
}

/* 无文本时的紧凑样式 */
.mc-badge:empty {
  padding: 0;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.mc-badge:empty .mc-badge__dot {
  width: 100%;
  height: 100%;
}
</style>
