<template>
  <div class="input-container" :style="{ width: width }">
    <label v-if="label" class="input-label">{{ label }}</label>
    
    <div class="input-wrapper">
      <span v-if="$slots.prefix" class="prefix-icon">
        <slot name="prefix"></slot>
      </span>

      <input
        :type="type"
        :value="modelValue"
        @input="$emit('update:modelValue', $event.target.value)"
        :placeholder="placeholder"
        class="base-input"
       :class="{ 'has-prefix': $slots.prefix }"
       v-bind="$attrs"
      />
    </div>
  </div>
</template>

<script setup>
defineOptions({ inheritAttrs: false })

defineProps({
  modelValue: String,        // 接收 v-model 的值
  label: String,             // 输入框上方的标题
  placeholder: String,       // 占位提示词
  type: { type: String, default: 'text' }, // 输入类型：text, password, number
  width: { type: String, default: '100%' }  // 宽度
})

defineEmits(['update:modelValue']) // 声明事件，用于双向绑定
</script>




<style scoped>
.input-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 24px; /* 增加一点间距，更有呼吸感 */
}

.input-label {
  font-size: 14px;
  font-weight: 500;
  color: #1d1d1f; /* 苹果标准黑 */
  text-align: left;
  padding-left: 2px;
  text-shadow: none; /* 去除投影 */
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.base-input {
  width: 100%;
  padding: 14px 16px; /* 稍微增加高度，更显大气 */
  
  /* 苹果官网风格：极浅灰背景 + 细边框 */
  background: #f5f5f7; 
  border: 1px solid #d2d2d7;
  border-radius: 12px; /* 配合你整体的 R 角 */
  
  color: #1d1d1f; /* 文字改为深色 */
  font-size: 17px; /* 苹果偏好的字体大小 */
  outline: none;
  transition: all 0.2s ease-in-out;
}

/* 焦点状态：边框加深，增加苹果蓝的内阴影或外框 */
.base-input:focus {
  background: #ffffff; /* 聚焦时背景变白 */
  border-color: #0066cc; /* 苹果蓝色边框 */
  box-shadow: 0 0 0 4px rgba(0, 102, 204, 0.1); /* 极其微弱的蓝色光晕 */
}

.base-input::placeholder {
  color: #86868b; /* 苹果次级文字灰 */
  text-shadow: none; /* 去除投影 */
}

/* 如果有图标，给左边留出空间 */
.has-prefix {
  padding-left: 44px;
}

.prefix-icon {
  position: absolute;
  left: 14px;
  color: #86868b; /* 图标也改为灰色 */
  display: flex;
  align-items: center;
  z-index: 10;
}

/* 针对密码框等自动填充样式的优化 */
.base-input:-webkit-autofill {
  -webkit-box-shadow: 0 0 0px 1000px #f5f5f7 inset;
  -webkit-text-fill-color: #1d1d1f;
}
</style>