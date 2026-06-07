<template>
  <span
    :class="[
      'apple-badge',
      `apple-badge--${variant}`,
      { 'apple-badge--dot': dot },
      { 'apple-badge--pulse': pulse },
      { 'apple-badge--outlined': outlined }
    ]"
  >
    <span v-if="dot" class="apple-badge__dot"></span>
    <slot>{{ text }}</slot>
  </span>
</template>

<script setup>
defineProps({
  variant: {
    type: String,
    default: 'default',
    validator: (value) => ['default', 'primary', 'success', 'warning', 'danger', 'info'].includes(value)
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
  },
  outlined: {
    type: Boolean,
    default: false
  }
})
</script>

<style scoped>
.apple-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border-radius: var(--apple-radius-full);
  font-family: var(--apple-font-body);
  font-size: var(--apple-text-xs);
  font-weight: var(--apple-font-medium);
  white-space: nowrap;
  line-height: 1.4;
  letter-spacing: var(--apple-tracking-wide);
}

/* 颜色变体 */
.apple-badge--default {
  background: var(--apple-bg-tertiary);
  color: var(--apple-text-secondary);
}

.apple-badge--primary {
  background: var(--apple-accent-light);
  color: var(--apple-accent);
}

.apple-badge--success {
  background: var(--apple-success-light);
  color: var(--apple-success);
}

.apple-badge--warning {
  background: var(--apple-warning-light);
  color: var(--apple-warning);
}

.apple-badge--danger {
  background: var(--apple-error-light);
  color: var(--apple-error);
}

.apple-badge--info {
  background: var(--apple-info-light);
  color: #007aff;
}

/* 描边变体 */
.apple-badge--outlined {
  background: transparent;
  border: 1px solid currentColor;
}

.apple-badge--outlined.apple-badge--default {
  border-color: var(--apple-border);
}

.apple-badge--outlined.apple-badge--primary {
  border-color: var(--apple-accent);
}

.apple-badge--outlined.apple-badge--success {
  border-color: var(--apple-success);
}

.apple-badge--outlined.apple-badge--warning {
  border-color: var(--apple-warning);
}

.apple-badge--outlined.apple-badge--danger {
  border-color: var(--apple-error);
}

.apple-badge--outlined.apple-badge--info {
  border-color: #007aff;
}

/* 圆点样式 */
.apple-badge__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

/* 脉冲动画 */
.apple-badge--pulse .apple-badge__dot {
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
.apple-badge:empty {
  padding: 0;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.apple-badge:empty .apple-badge__dot {
  width: 100%;
  height: 100%;
}
</style>
