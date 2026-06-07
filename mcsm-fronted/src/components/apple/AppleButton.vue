<template>
  <button
    :class="[
      'apple-button',
      `apple-button--${variant}`,
      `apple-button--${size}`,
      { 'apple-button--block': block },
      { 'apple-button--loading': loading },
      { 'apple-button--disabled': disabled },
      { 'apple-button--rounded': rounded }
    ]"
    :disabled="disabled || loading"
    @click="$emit('click', $event)"
  >
    <span v-if="loading" class="apple-button__spinner">
      <span class="apple-spinner"></span>
    </span>
    <span v-if="$slots.icon && !loading" class="apple-button__icon">
      <slot name="icon"></slot>
    </span>
    <span class="apple-button__text">
      <slot></slot>
    </span>
  </button>
</template>

<script setup>
defineProps({
  variant: {
    type: String,
    default: 'primary',
    validator: (value) => ['primary', 'secondary', 'tertiary', 'danger', 'success', 'ghost'].includes(value)
  },
  size: {
    type: String,
    default: 'medium',
    validator: (value) => ['small', 'medium', 'large'].includes(value)
  },
  block: {
    type: Boolean,
    default: false
  },
  loading: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  },
  rounded: {
    type: Boolean,
    default: false
  }
})

defineEmits(['click'])
</script>

<style scoped>
.apple-button {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-family: var(--apple-font-body);
  font-weight: var(--apple-font-medium);
  cursor: pointer;
  transition: all var(--apple-transition-fast);
  border: none;
  border-radius: var(--apple-radius-md);
  white-space: nowrap;
  user-select: none;
  -webkit-font-smoothing: antialiased;
  letter-spacing: var(--apple-tracking-normal);
}

.apple-button:focus {
  outline: none;
}

.apple-button:focus-visible {
  box-shadow: var(--apple-shadow-focus);
}

.apple-button:active {
  transform: scale(0.98);
}

/* 尺寸变体 */
.apple-button--small {
  padding: 6px 14px;
  font-size: var(--apple-text-sm);
  height: var(--apple-btn-height-sm);
}

.apple-button--medium {
  padding: 8px 18px;
  font-size: var(--apple-text-base);
  height: var(--apple-btn-height-md);
}

.apple-button--large {
  padding: 12px 24px;
  font-size: var(--apple-text-lg);
  height: var(--apple-btn-height-lg);
}

/* 圆角变体 */
.apple-button--rounded {
  border-radius: var(--apple-radius-full);
}

/* 颜色变体 */
.apple-button--primary {
  background: var(--apple-accent);
  color: var(--apple-text-inverse);
}

.apple-button--primary:hover:not(:disabled) {
  background: var(--apple-accent-hover);
}

.apple-button--primary:active:not(:disabled) {
  background: var(--apple-accent-active);
}

.apple-button--secondary {
  background: var(--apple-bg-secondary);
  color: var(--apple-text-primary);
}

.apple-button--secondary:hover:not(:disabled) {
  background: var(--apple-bg-tertiary);
}

.apple-button--secondary:active:not(:disabled) {
  background: rgba(0, 0, 0, 0.1);
}

.apple-button--tertiary {
  background: transparent;
  color: var(--apple-accent);
}

.apple-button--tertiary:hover:not(:disabled) {
  background: var(--apple-accent-subtle);
}

.apple-button--tertiary:active:not(:disabled) {
  background: var(--apple-accent-light);
}

.apple-button--danger {
  background: var(--apple-error);
  color: var(--apple-text-inverse);
}

.apple-button--danger:hover:not(:disabled) {
  background: #e63329;
}

.apple-button--danger:active:not(:disabled) {
  background: #cc2d24;
}

.apple-button--success {
  background: var(--apple-success);
  color: var(--apple-text-inverse);
}

.apple-button--success:hover:not(:disabled) {
  background: #2db84e;
}

.apple-button--success:active:not(:disabled) {
  background: #28a344;
}

.apple-button--ghost {
  background: transparent;
  color: var(--apple-accent);
  border: 1px solid var(--apple-border);
}

.apple-button--ghost:hover:not(:disabled) {
  background: var(--apple-accent-subtle);
  border-color: var(--apple-accent);
}

.apple-button--ghost:active:not(:disabled) {
  background: var(--apple-accent-light);
}

/* 块级按钮 */
.apple-button--block {
  width: 100%;
}

/* 加载状态 */
.apple-button--loading {
  opacity: 0.7;
  cursor: not-allowed;
}

.apple-button__spinner {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.apple-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: currentColor;
  border-radius: 50%;
  animation: appleSpin 0.8s linear infinite;
}

@keyframes appleSpin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.apple-button--secondary .apple-spinner,
.apple-button--tertiary .apple-spinner,
.apple-button--ghost .apple-spinner {
  border-color: rgba(0, 0, 0, 0.1);
  border-top-color: var(--apple-accent);
}

/* 禁用状态 */
.apple-button--disabled {
  opacity: 0.4;
  cursor: not-allowed;
  transform: none !important;
}

/* 图标样式 */
.apple-button__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1em;
}

.apple-button__text {
  display: inline-flex;
  align-items: center;
}
</style>
