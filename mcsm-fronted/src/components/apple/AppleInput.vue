<template>
  <div
    :class="[
      'apple-input-wrapper',
      { 'apple-input-wrapper--focused': isFocused },
      { 'apple-input-wrapper--error': error },
      { 'apple-input-wrapper--disabled': disabled },
      `apple-input-wrapper--${size}`
    ]"
  >
    <label v-if="label" class="apple-input__label" :for="inputId">
      {{ label }}
      <span v-if="required" class="apple-input__required">*</span>
    </label>
    <div class="apple-input__container">
      <span v-if="$slots.prefix" class="apple-input__prefix">
        <slot name="prefix"></slot>
      </span>
      <input
        :id="inputId"
        ref="inputRef"
        :type="type"
        :value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled"
        :readonly="readonly"
        :maxlength="maxlength"
        class="apple-input__field"
        @input="$emit('update:modelValue', $event.target.value)"
        @focus="isFocused = true"
        @blur="isFocused = false"
        @keyup.enter="$emit('enter', $event)"
      />
      <span v-if="$slots.suffix" class="apple-input__suffix">
        <slot name="suffix"></slot>
      </span>
      <span v-if="clearable && modelValue" class="apple-input__clear" @click="handleClear">
        <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
          <path d="M10.5 3.5L3.5 10.5M3.5 3.5L10.5 10.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
        </svg>
      </span>
    </div>
    <div v-if="error || hint" class="apple-input__message">
      <span v-if="error" class="apple-input__error">{{ error }}</span>
      <span v-else-if="hint" class="apple-input__hint">{{ hint }}</span>
    </div>
    <div v-if="maxlength" class="apple-input__counter">
      {{ modelValue?.length || 0 }}/{{ maxlength }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: ''
  },
  type: {
    type: String,
    default: 'text'
  },
  label: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: ''
  },
  size: {
    type: String,
    default: 'medium',
    validator: (value) => ['small', 'medium', 'large'].includes(value)
  },
  error: {
    type: String,
    default: ''
  },
  hint: {
    type: String,
    default: ''
  },
  disabled: {
    type: Boolean,
    default: false
  },
  readonly: {
    type: Boolean,
    default: false
  },
  required: {
    type: Boolean,
    default: false
  },
  clearable: {
    type: Boolean,
    default: false
  },
  maxlength: {
    type: [String, Number],
    default: null
  }
})

const emit = defineEmits(['update:modelValue', 'enter', 'clear'])

const inputRef = ref(null)
const isFocused = ref(false)

const inputId = computed(() => `apple-input-${Math.random().toString(36).substr(2, 9)}`)

const handleClear = () => {
  emit('update:modelValue', '')
  emit('clear')
  inputRef.value?.focus()
}
</script>

<style scoped>
.apple-input-wrapper {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}

.apple-input__label {
  font-family: var(--apple-font-body);
  font-size: var(--apple-text-sm);
  font-weight: var(--apple-font-medium);
  color: var(--apple-text-primary);
  display: flex;
  align-items: center;
  gap: 4px;
}

.apple-input__required {
  color: var(--apple-error);
  font-weight: var(--apple-font-bold);
}

.apple-input__container {
  position: relative;
  display: flex;
  align-items: center;
  background: var(--apple-bg-primary);
  border: 1px solid var(--apple-border);
  border-radius: var(--apple-radius-md);
  transition: all var(--apple-transition-fast);
  overflow: hidden;
}

.apple-input__container:hover {
  border-color: var(--apple-border-hover);
}

.apple-input-wrapper--focused .apple-input__container {
  border-color: var(--apple-accent);
  box-shadow: 0 0 0 3px var(--apple-accent-light);
}

.apple-input-wrapper--error .apple-input__container {
  border-color: var(--apple-error);
  box-shadow: 0 0 0 3px var(--apple-error-light);
}

.apple-input-wrapper--disabled .apple-input__container {
  opacity: 0.6;
  cursor: not-allowed;
  background: var(--apple-bg-secondary);
}

/* 尺寸变体 */
.apple-input-wrapper--small .apple-input__container {
  height: var(--apple-input-height-sm);
}

.apple-input-wrapper--medium .apple-input__container {
  height: var(--apple-input-height-md);
}

.apple-input-wrapper--large .apple-input__container {
  height: var(--apple-input-height-lg);
}

.apple-input-wrapper--small .apple-input__field {
  padding: 6px 12px;
  font-size: var(--apple-text-sm);
}

.apple-input-wrapper--medium .apple-input__field {
  padding: 8px 14px;
  font-size: var(--apple-text-base);
}

.apple-input-wrapper--large .apple-input__field {
  padding: 12px 16px;
  font-size: var(--apple-text-lg);
}

/* 输入框 */
.apple-input__field {
  flex: 1;
  width: 100%;
  height: 100%;
  border: none;
  background: transparent;
  color: var(--apple-text-primary);
  font-family: var(--apple-font-body);
  outline: none;
  letter-spacing: var(--apple-tracking-normal);
}

.apple-input__field::placeholder {
  color: var(--apple-text-muted);
}

.apple-input__field:disabled {
  cursor: not-allowed;
}

/* 前缀后缀 */
.apple-input__prefix,
.apple-input__suffix {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  color: var(--apple-text-secondary);
  font-size: var(--apple-text-base);
}

.apple-input__prefix {
  border-right: 1px solid var(--apple-border);
}

.apple-input__suffix {
  border-left: 1px solid var(--apple-border);
}

/* 清除按钮 */
.apple-input__clear {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  margin-right: 8px;
  border-radius: 50%;
  background: var(--apple-bg-tertiary);
  color: var(--apple-text-secondary);
  cursor: pointer;
  transition: all var(--apple-transition-fast);
}

.apple-input__clear:hover {
  background: var(--apple-text-muted);
  color: var(--apple-text-primary);
}

/* 消息提示 */
.apple-input__message {
  font-size: var(--apple-text-xs);
  line-height: 1.4;
  min-height: 18px;
}

.apple-input__error {
  color: var(--apple-error);
}

.apple-input__hint {
  color: var(--apple-text-muted);
}

/* 字数统计 */
.apple-input__counter {
  font-size: var(--apple-text-xs);
  color: var(--apple-text-muted);
  text-align: right;
  margin-top: 2px;
}
</style>
