<template>
  <div class="mc-tooltip-wrapper" @mouseenter="show" @mouseleave="hide">
    <slot></slot>
    <Transition name="mc-tooltip">
      <div
        v-if="isVisible"
        :class="[
          'mc-tooltip',
          `mc-tooltip--${placement}`,
          `mc-tooltip--${variant}`
        ]"
        :style="tooltipStyle"
      >
        <div class="mc-tooltip__content">
          <slot name="content">{{ content }}</slot>
        </div>
        <div class="mc-tooltip__arrow"></div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  content: {
    type: String,
    default: ''
  },
  placement: {
    type: String,
    default: 'top',
    validator: (value) => ['top', 'bottom', 'left', 'right'].includes(value)
  },
  variant: {
    type: String,
    default: 'default',
    validator: (value) => ['default', 'dark', 'light', 'success', 'warning', 'danger'].includes(value)
  },
  delay: {
    type: Number,
    default: 200
  }
})

const isVisible = ref(false)
let showTimeout = null
let hideTimeout = null

const show = () => {
  clearTimeout(hideTimeout)
  showTimeout = setTimeout(() => {
    isVisible.value = true
  }, props.delay)
}

const hide = () => {
  clearTimeout(showTimeout)
  hideTimeout = setTimeout(() => {
    isVisible.value = false
  }, 100)
}

const tooltipStyle = computed(() => {
  const style = {}
  // 根据 placement 调整位置
  return style
})
</script>

<style scoped>
.mc-tooltip-wrapper {
  position: relative;
  display: inline-flex;
}

.mc-tooltip {
  position: absolute;
  z-index: var(--mc-z-tooltip);
  pointer-events: none;
}

.mc-tooltip--top {
  bottom: calc(100% + 8px);
  left: 50%;
  transform: translateX(-50%);
}

.mc-tooltip--bottom {
  top: calc(100% + 8px);
  left: 50%;
  transform: translateX(-50%);
}

.mc-tooltip--left {
  right: calc(100% + 8px);
  top: 50%;
  transform: translateY(-50%);
}

.mc-tooltip--right {
  left: calc(100% + 8px);
  top: 50%;
  transform: translateY(-50%);
}

.mc-tooltip__content {
  padding: 8px 12px;
  border-radius: var(--mc-radius-sm);
  font-family: var(--mc-font-body);
  font-size: var(--mc-text-sm);
  line-height: 1.4;
  white-space: nowrap;
  max-width: 250px;
  word-wrap: break-word;
}

.mc-tooltip__arrow {
  position: absolute;
  width: 8px;
  height: 8px;
  transform: rotate(45deg);
}

/* 箭头位置 */
.mc-tooltip--top .mc-tooltip__arrow {
  bottom: -4px;
  left: 50%;
  margin-left: -4px;
}

.mc-tooltip--bottom .mc-tooltip__arrow {
  top: -4px;
  left: 50%;
  margin-left: -4px;
}

.mc-tooltip--left .mc-tooltip__arrow {
  right: -4px;
  top: 50%;
  margin-top: -4px;
}

.mc-tooltip--right .mc-tooltip__arrow {
  left: -4px;
  top: 50%;
  margin-top: -4px;
}

/* 颜色变体 */
.mc-tooltip--default .mc-tooltip__content {
  background: var(--mc-text-dark);
  color: var(--mc-text-light);
}

.mc-tooltip--default .mc-tooltip__arrow {
  background: var(--mc-text-dark);
}

.mc-tooltip--dark .mc-tooltip__content {
  background: rgba(0, 0, 0, 0.9);
  color: var(--mc-text-light);
}

.mc-tooltip--dark .mc-tooltip__arrow {
  background: rgba(0, 0, 0, 0.9);
}

.mc-tooltip--light .mc-tooltip__content {
  background: var(--mc-bg-card);
  color: var(--mc-text-primary);
  border: 2px solid var(--mc-border);
  box-shadow: var(--mc-shadow-md);
}

.mc-tooltip--light .mc-tooltip__arrow {
  background: var(--mc-bg-card);
  border: 2px solid var(--mc-border);
}

.mc-tooltip--success .mc-tooltip__content {
  background: var(--mc-emerald-green);
  color: var(--mc-text-light);
}

.mc-tooltip--success .mc-tooltip__arrow {
  background: var(--mc-emerald-green);
}

.mc-tooltip--warning .mc-tooltip__content {
  background: var(--mc-gold-yellow);
  color: var(--mc-text-dark);
}

.mc-tooltip--warning .mc-tooltip__arrow {
  background: var(--mc-gold-yellow);
}

.mc-tooltip--danger .mc-tooltip__content {
  background: var(--mc-redstone-red);
  color: var(--mc-text-light);
}

.mc-tooltip--danger .mc-tooltip__arrow {
  background: var(--mc-redstone-red);
}

/* 过渡动画 */
.mc-tooltip-enter-active {
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.mc-tooltip-leave-active {
  transition: all 0.15s ease-in;
}

.mc-tooltip-enter-from {
  opacity: 0;
  transform: scale(0.9);
}

.mc-tooltip-leave-to {
  opacity: 0;
  transform: scale(0.95);
}

/* 特定方向的动画 */
.mc-tooltip--top.mc-tooltip-enter-from {
  transform: translateX(-50%) translateY(5px);
}

.mc-tooltip--bottom.mc-tooltip-enter-from {
  transform: translateX(-50%) translateY(-5px);
}

.mc-tooltip--left.mc-tooltip-enter-from {
  transform: translateY(-50%) translateX(5px);
}

.mc-tooltip--right.mc-tooltip-enter-from {
  transform: translateY(-50%) translateX(-5px);
}
</style>
