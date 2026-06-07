<template>
  <div class="apple-tooltip-wrapper" @mouseenter="show" @mouseleave="hide">
    <slot></slot>
    <Transition name="apple-tooltip">
      <div
        v-if="isVisible"
        :class="[
          'apple-tooltip',
          `apple-tooltip--${placement}`,
          `apple-tooltip--${variant}`
        ]"
        :style="tooltipStyle"
      >
        <div class="apple-tooltip__content">
          <slot name="content">{{ content }}</slot>
        </div>
        <div class="apple-tooltip__arrow"></div>
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
    validator: (value) => ['default', 'dark', 'light'].includes(value)
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
.apple-tooltip-wrapper {
  position: relative;
  display: inline-flex;
}

.apple-tooltip {
  position: absolute;
  z-index: var(--apple-z-tooltip);
  pointer-events: none;
}

.apple-tooltip--top {
  bottom: calc(100% + 8px);
  left: 50%;
  transform: translateX(-50%);
}

.apple-tooltip--bottom {
  top: calc(100% + 8px);
  left: 50%;
  transform: translateX(-50%);
}

.apple-tooltip--left {
  right: calc(100% + 8px);
  top: 50%;
  transform: translateY(-50%);
}

.apple-tooltip--right {
  left: calc(100% + 8px);
  top: 50%;
  transform: translateY(-50%);
}

.apple-tooltip__content {
  padding: 8px 12px;
  border-radius: var(--apple-radius-md);
  font-family: var(--apple-font-body);
  font-size: var(--apple-text-sm);
  line-height: 1.4;
  white-space: nowrap;
  max-width: 250px;
  word-wrap: break-word;
  backdrop-filter: var(--apple-backdrop-blur);
  -webkit-backdrop-filter: var(--apple-backdrop-blur);
}

.apple-tooltip__arrow {
  position: absolute;
  width: 8px;
  height: 8px;
  transform: rotate(45deg);
}

/* 箭头位置 */
.apple-tooltip--top .apple-tooltip__arrow {
  bottom: -4px;
  left: 50%;
  margin-left: -4px;
}

.apple-tooltip--bottom .apple-tooltip__arrow {
  top: -4px;
  left: 50%;
  margin-left: -4px;
}

.apple-tooltip--left .apple-tooltip__arrow {
  right: -4px;
  top: 50%;
  margin-top: -4px;
}

.apple-tooltip--right .apple-tooltip__arrow {
  left: -4px;
  top: 50%;
  margin-top: -4px;
}

/* 颜色变体 */
.apple-tooltip--default .apple-tooltip__content {
  background: rgba(0, 0, 0, 0.8);
  color: var(--apple-text-inverse);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.apple-tooltip--default .apple-tooltip__arrow {
  background: rgba(0, 0, 0, 0.8);
}

.apple-tooltip--dark .apple-tooltip__content {
  background: rgba(0, 0, 0, 0.9);
  color: var(--apple-text-inverse);
}

.apple-tooltip--dark .apple-tooltip__arrow {
  background: rgba(0, 0, 0, 0.9);
}

.apple-tooltip--light .apple-tooltip__content {
  background: var(--apple-glass-bg);
  color: var(--apple-text-primary);
  border: var(--apple-glass-border);
  box-shadow: var(--apple-shadow-lg);
  backdrop-filter: var(--apple-backdrop-blur);
  -webkit-backdrop-filter: var(--apple-backdrop-blur);
}

.apple-tooltip--light .apple-tooltip__arrow {
  background: var(--apple-glass-bg);
  border: var(--apple-glass-border);
}

/* 过渡动画 */
.apple-tooltip-enter-active {
  transition: all 0.2s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.apple-tooltip-leave-active {
  transition: all 0.15s ease-in;
}

.apple-tooltip-enter-from {
  opacity: 0;
  transform: scale(0.95);
}

.apple-tooltip-leave-to {
  opacity: 0;
  transform: scale(0.98);
}

/* 特定方向的动画 */
.apple-tooltip--top.apple-tooltip-enter-from {
  transform: translateX(-50%) translateY(4px);
}

.apple-tooltip--bottom.apple-tooltip-enter-from {
  transform: translateX(-50%) translateY(-4px);
}

.apple-tooltip--left.apple-tooltip-enter-from {
  transform: translateY(-50%) translateX(4px);
}

.apple-tooltip--right.apple-tooltip-enter-from {
  transform: translateY(-50%) translateX(-4px);
}
</style>
