<template>
  <button
    :class="[
      'mc-button',
      `mc-button--${variant}`,
      `mc-button--${size}`,
      { 'mc-button--block': block },
      { 'mc-button--loading': loading },
      { 'mc-button--disabled': disabled }
    ]"
    :disabled="disabled || loading"
    @click="$emit('click', $event)"
  >
    <span v-if="loading" class="mc-button__spinner">
      <span class="mc-spinner"></span>
    </span>
    <span v-if="$slots.icon && !loading" class="mc-button__icon">
      <slot name="icon"></slot>
    </span>
    <span class="mc-button__text">
      <slot></slot>
    </span>
  </button>
</template>

<script setup>
defineProps({
  variant: {
    type: String,
    default: 'primary',
    validator: (value) => ['primary', 'secondary', 'success', 'danger', 'warning', 'ghost'].includes(value)
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
  }
})

defineEmits(['click'])
</script>

<style scoped>
.mc-button {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-family: var(--mc-font-display);
  font-weight: var(--mc-font-semibold);
  cursor: pointer;
  transition: all var(--mc-transition-fast);
  border: 3px solid transparent;
  border-radius: var(--mc-radius-md);
  white-space: nowrap;
  user-select: none;
}

.mc-button:focus {
  outline: 2px solid var(--mc-emerald-green);
  outline-offset: 2px;
}

.mc-button:active {
  transform: translateY(2px);
}

/* 尺寸变体 */
.mc-button--small {
  padding: 8px 16px;
  font-size: var(--mc-text-sm);
  height: 36px;
}

.mc-button--medium {
  padding: 10px 20px;
  font-size: var(--mc-text-base);
  height: 44px;
}

.mc-button--large {
  padding: 14px 28px;
  font-size: var(--mc-text-lg);
  height: 52px;
}

/* 颜色变体 */
.mc-button--primary {
  background: linear-gradient(180deg, var(--mc-emerald-green) 0%, var(--mc-emerald-dark) 100%);
  border-color: #0d7a3a;
  color: var(--mc-text-light);
  box-shadow: 0 4px 0 #0d7a3a;
}

.mc-button--primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 0 #0d7a3a;
}

.mc-button--primary:active:not(:disabled) {
  transform: translateY(2px);
  box-shadow: 0 2px 0 #0d7a3a;
}

.mc-button--secondary {
  background: linear-gradient(180deg, var(--mc-stone-gray) 0%, var(--mc-stone-light) 100%);
  border-color: #666666;
  color: var(--mc-text-light);
  box-shadow: 0 4px 0 #666666;
}

.mc-button--secondary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 0 #666666;
}

.mc-button--secondary:active:not(:disabled) {
  transform: translateY(2px);
  box-shadow: 0 2px 0 #666666;
}

.mc-button--success {
  background: linear-gradient(180deg, var(--mc-gold-yellow) 0%, var(--mc-gold-dark) 100%);
  border-color: #c9a800;
  color: var(--mc-text-dark);
  box-shadow: 0 4px 0 #c9a800;
}

.mc-button--success:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 0 #c9a800;
}

.mc-button--success:active:not(:disabled) {
  transform: translateY(2px);
  box-shadow: 0 2px 0 #c9a800;
}

.mc-button--danger {
  background: linear-gradient(180deg, var(--mc-redstone-red) 0%, var(--mc-redstone-dark) 100%);
  border-color: #990000;
  color: var(--mc-text-light);
  box-shadow: 0 4px 0 #990000;
}

.mc-button--danger:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 0 #990000;
}

.mc-button--danger:active:not(:disabled) {
  transform: translateY(2px);
  box-shadow: 0 2px 0 #990000;
}

.mc-button--warning {
  background: linear-gradient(180deg, var(--mc-lava-orange) 0%, var(--mc-lava-red) 100%);
  border-color: #cc5200;
  color: var(--mc-text-light);
  box-shadow: 0 4px 0 #cc5200;
}

.mc-button--warning:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 0 #cc5200;
}

.mc-button--warning:active:not(:disabled) {
  transform: translateY(2px);
  box-shadow: 0 2px 0 #cc5200;
}

.mc-button--ghost {
  background: transparent;
  border-color: var(--mc-border-dark);
  color: var(--mc-text-primary);
  box-shadow: none;
}

.mc-button--ghost:hover:not(:disabled) {
  background: rgba(0, 0, 0, 0.05);
  border-color: var(--mc-text-secondary);
}

.mc-button--ghost:active:not(:disabled) {
  background: rgba(0, 0, 0, 0.1);
}

/* 块级按钮 */
.mc-button--block {
  width: 100%;
}

/* 加载状态 */
.mc-button--loading {
  opacity: 0.8;
  cursor: not-allowed;
}

.mc-button__spinner {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.mc-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: currentColor;
  border-radius: 50%;
  animation: mcRotate 1s linear infinite;
}

@keyframes mcRotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 禁用状态 */
.mc-button--disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none !important;
  box-shadow: none !important;
}

/* 图标样式 */
.mc-button__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1em;
}

.mc-button__text {
  display: inline-flex;
  align-items: center;
}
</style>
