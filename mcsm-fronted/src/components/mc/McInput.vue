<template>
  <div
    :class="[
      'mc-input-wrapper',
      { 'mc-input-wrapper--focused': isFocused },
      { 'mc-input-wrapper--error': error },
      { 'mc-input-wrapper--disabled': disabled },
      `mc-input-wrapper--${size}`
    ]"
  >
    <label v-if="label" class="mc-input__label" :for="inputId">
      {{ label }}
      <span v-if="required" class="mc-input__required">*</span>
    </label>
    <div class="mc-input__container">
      <span v-if="$slots.prefix" class="mc-input__prefix">
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
        class="mc-input__field"
        @input="$emit('update:modelValue', $event.target.value)"
        @focus="isFocused = true"
        @blur="isFocused = false"
        @keyup.enter="$emit('enter', $event)"
      />
      <span v-if="$slots.suffix" class="mc-input__suffix">
        <slot name="suffix"></slot>
      </span>
      <span v-if="clearable && modelValue" class="mc-input__clear" @click="handleClear">
        ✕
      </span>
    </div>
    <div v-if="error || hint" class="mc-input__message">
      <span v-if="error" class="mc-input__error">{{ error }}</span>
      <span v-else-if="hint" class="mc-input__hint">{{ hint }}</span>
    </div>
    <div v-if="maxlength" class="mc-input__counter">
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

const inputId = computed(() => `mc-input-${Math.random().toString(36).substr(2, 9)}`)

const handleClear = () => {
  emit('update:modelValue', '')
  emit('clear')
  inputRef.value?.focus()
}
</script>

<style scoped>
.mc-input-wrapper {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}

.mc-input__label {
  font-family: var(--mc-font-display);
  font-size: var(--mc-text-sm);
  font-weight: var(--mc-font-medium);
  color: var(--mc-text-primary);
  display: flex;
  align-items: center;
  gap: 4px;
}

.mc-input__required {
  color: var(--mc-redstone-red);
  font-weight: var(--mc-font-bold);
}

.mc-input__container {
  position: relative;
  display: flex;
  align-items: center;
  background: var(--mc-bg-card);
  border: 2px solid var(--mc-border);
  border-radius: var(--mc-radius-md);
  transition: all var(--mc-transition-fast);
  overflow: hidden;
}

.mc-input__container:hover {
  border-color: var(--mc-border-dark);
}

.mc-input-wrapper--focused .mc-input__container {
  border-color: var(--mc-emerald-green);
  box-shadow: 0 0 0 3px rgba(23, 221, 98, 0.2);
}

.mc-input-wrapper--error .mc-input__container {
  border-color: var(--mc-redstone-red);
  box-shadow: 0 0 0 3px rgba(255, 0, 0, 0.1);
}

.mc-input-wrapper--disabled .mc-input__container {
  opacity: 0.6;
  cursor: not-allowed;
  background: rgba(0, 0, 0, 0.05);
}

/* 尺寸变体 */
.mc-input-wrapper--small .mc-input__container {
  height: 36px;
}

.mc-input-wrapper--medium .mc-input__container {
  height: 44px;
}

.mc-input-wrapper--large .mc-input__container {
  height: 52px;
}

.mc-input-wrapper--small .mc-input__field {
  padding: 8px 12px;
  font-size: var(--mc-text-sm);
}

.mc-input-wrapper--medium .mc-input__field {
  padding: 10px 14px;
  font-size: var(--mc-text-base);
}

.mc-input-wrapper--large .mc-input__field {
  padding: 14px 16px;
  font-size: var(--mc-text-lg);
}

/* 输入框 */
.mc-input__field {
  flex: 1;
  width: 100%;
  height: 100%;
  border: none;
  background: transparent;
  color: var(--mc-text-primary);
  font-family: var(--mc-font-body);
  outline: none;
}

.mc-input__field::placeholder {
  color: var(--mc-text-muted);
}

.mc-input__field:disabled {
  cursor: not-allowed;
}

/* 前缀后缀 */
.mc-input__prefix,
.mc-input__suffix {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 12px;
  color: var(--mc-text-secondary);
  font-size: var(--mc-text-base);
}

.mc-input__prefix {
  border-right: 2px solid var(--mc-border);
}

.mc-input__suffix {
  border-left: 2px solid var(--mc-border);
}

/* 清除按钮 */
.mc-input__clear {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  margin-right: 8px;
  border-radius: 50%;
  background: var(--mc-text-muted);
  color: white;
  font-size: 12px;
  cursor: pointer;
  transition: all var(--mc-transition-fast);
}

.mc-input__clear:hover {
  background: var(--mc-text-secondary);
}

/* 消息提示 */
.mc-input__message {
  font-size: var(--mc-text-xs);
  line-height: 1.4;
  min-height: 18px;
}

.mc-input__error {
  color: var(--mc-redstone-red);
}

.mc-input__hint {
  color: var(--mc-text-muted);
}

/* 字数统计 */
.mc-input__counter {
  font-size: var(--mc-text-xs);
  color: var(--mc-text-muted);
  text-align: right;
  margin-top: 2px;
}
</style>
