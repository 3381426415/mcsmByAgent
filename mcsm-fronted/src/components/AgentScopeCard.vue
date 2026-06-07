<template>
  <div class="agent-scope" :class="[scope.status, { 'is-root': isRoot }]">
    <!-- Agent 头部 -->
    <div class="scope-header" @click="toggleExpand">
      <span class="scope-icon">{{ agentIcon }}</span>
      <span class="scope-type">{{ agentLabel }}</span>
      <span class="scope-task" v-if="scope.task">{{ scope.task }}</span>
      <span class="scope-status">
        <span v-if="scope.status === 'running'" class="status-dot running"></span>
        <span v-else-if="scope.status === 'done'" class="status-dot done">✓</span>
        <span v-else-if="scope.status === 'error'" class="status-dot error">✗</span>
      </span>
      <span class="scope-toggle">{{ expanded ? '▼' : '▶' }}</span>
    </div>

    <!-- 展开内容 -->
    <div v-if="expanded" class="scope-body">
      <!-- 思考区域 -->
      <div v-if="scope.thinking?.content" class="thinking-section">
        <div class="thinking-header" @click="thinkingCollapsed = !thinkingCollapsed">
          <span class="thinking-icon">💭</span>
          <span class="thinking-label">思考过程</span>
          <span class="thinking-toggle">{{ thinkingCollapsed ? '展开' : '收起' }}</span>
        </div>
        <div v-if="!thinkingCollapsed" class="thinking-content">
          {{ scope.thinking.content }}
        </div>
      </div>

      <!-- 工具调用列表 -->
      <div v-if="scope.toolCalls?.length" class="tools-section">
        <div
          v-for="(tool, idx) in scope.toolCalls"
          :key="idx"
          class="tool-item"
        >
          <div class="tool-header" @click="toggleTool(idx)">
            <span class="tool-icon">{{ toolIcon(tool.name) }}</span>
            <span class="tool-name">{{ tool.name }}</span>
            <span class="tool-summary" v-if="tool.summary">{{ tool.summary }}</span>
            <span class="tool-status" v-if="tool.success !== undefined">
              {{ tool.success ? '✓' : '✗' }}
            </span>
            <span class="tool-toggle">{{ expandedTools.has(idx) ? '▼' : '▶' }}</span>
          </div>
          <div v-if="expandedTools.has(idx)" class="tool-detail">
            <div v-if="tool.args" class="tool-args">
              <div class="detail-label">参数：</div>
              <pre class="detail-code">{{ formatJson(tool.args) }}</pre>
            </div>
            <div v-if="tool.result" class="tool-result">
              <div class="detail-label">结果：</div>
              <pre class="detail-code">{{ tool.result }}</pre>
            </div>
          </div>
        </div>
      </div>

      <!-- 子 Agent 作用域（递归） -->
      <div v-if="scope.children?.length" class="children-section">
        <AgentScopeCard
          v-for="child in scope.children"
          :key="child.agentId"
          :scope="child"
          :is-root="false"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';

const props = defineProps({
  scope: { type: Object, required: true },
  isRoot: { type: Boolean, default: true }
});

const expanded = ref(true);
const thinkingCollapsed = ref(true);
const expandedTools = ref(new Set());

const agentIcon = computed(() => {
  const icons = {
    'DECISION': '🤖',
    'ROLE': '🔍',
    'EXECUTOR': '📝',
    'CHECKER': '✅',
    'PLANNER': '📋',
    'SUMMARIZER': '📊',
    'KNOWLEDGE': '📚'
  };
  return icons[props.scope.agentType] || '🤖';
});

const agentLabel = computed(() => {
  const labels = {
    'DECISION': '决策智能体',
    'ROLE': '分析智能体',
    'EXECUTOR': '执行智能体',
    'CHECKER': '校验智能体',
    'PLANNER': '规划智能体',
    'SUMMARIZER': '总结智能体',
    'KNOWLEDGE': '知识智能体'
  };
  return labels[props.scope.agentType] || props.scope.agentType;
});

const toggleExpand = () => {
  expanded.value = !expanded.value;
};

const toggleTool = (idx) => {
  if (expandedTools.value.has(idx)) {
    expandedTools.value.delete(idx);
  } else {
    expandedTools.value.add(idx);
  }
};

const toolIcon = (name) => {
  if (name?.includes('read') || name?.includes('search') || name?.includes('grep')) return '📖';
  if (name?.includes('write') || name?.includes('edit')) return '📝';
  if (name?.includes('delete')) return '🗑️';
  if (name?.includes('list')) return '📂';
  if (name?.includes('spawn')) return '🤖';
  if (name?.includes('server') || name?.includes('start') || name?.includes('stop')) return '🖥️';
  if (name?.includes('rcon') || name?.includes('command')) return '⌨️';
  return '🔧';
};

const formatJson = (str) => {
  try {
    return JSON.stringify(JSON.parse(str), null, 2);
  } catch {
    return str;
  }
};

// 自动展开运行中的 scope
watch(() => props.scope.status, (newStatus) => {
  if (newStatus === 'running') expanded.value = true;
});
</script>

<style scoped>
.agent-scope {
  border: 1px solid #e5e5ea;
  border-radius: 10px;
  margin-bottom: 8px;
  overflow: hidden;
  background: #ffffff;
}

.agent-scope.is-root {
  border-color: #d1d1d6;
}

.agent-scope.running {
  border-color: #007aff;
}

.agent-scope.done {
  border-color: #34c759;
}

.agent-scope.error {
  border-color: #ff3b30;
}

/* 头部 */
.scope-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  cursor: pointer;
  background: #fafafa;
  transition: background 0.2s;
}

.scope-header:hover {
  background: #f0f0f5;
}

.scope-icon {
  font-size: 16px;
}

.scope-type {
  font-size: 13px;
  font-weight: 600;
  color: #1d1d1f;
}

.scope-task {
  font-size: 12px;
  color: #86868b;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.scope-status {
  margin-left: auto;
}

.status-dot {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  font-size: 10px;
}

.status-dot.running {
  border: 2px solid #007aff;
  border-top-color: transparent;
  animation: spin 1s linear infinite;
}

.status-dot.done {
  background: #34c759;
  color: white;
}

.status-dot.error {
  background: #ff3b30;
  color: white;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.scope-toggle {
  font-size: 10px;
  color: #aeaeb2;
}

/* 内容区 */
.scope-body {
  padding: 0 14px 14px;
}

/* 思考区域 */
.thinking-section {
  margin-bottom: 10px;
}

.thinking-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 0;
  cursor: pointer;
}

.thinking-icon {
  font-size: 14px;
}

.thinking-label {
  font-size: 12px;
  font-weight: 500;
  color: #86868b;
}

.thinking-toggle {
  font-size: 11px;
  color: #007aff;
  margin-left: auto;
}

.thinking-content {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 13px;
  line-height: 1.6;
  color: #48484a;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 300px;
  overflow-y: auto;
}

/* 工具调用 */
.tools-section {
  margin-bottom: 10px;
}

.tool-item {
  border: 1px solid #f0f0f5;
  border-radius: 8px;
  margin-bottom: 6px;
  overflow: hidden;
}

.tool-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  cursor: pointer;
  transition: background 0.2s;
}

.tool-header:hover {
  background: #f8f9fa;
}

.tool-icon {
  font-size: 14px;
}

.tool-name {
  font-size: 12px;
  font-weight: 600;
  color: #1d1d1f;
  font-family: 'SF Mono', 'Fira Code', monospace;
}

.tool-summary {
  font-size: 11px;
  color: #86868b;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tool-status {
  font-size: 12px;
}

.tool-toggle {
  font-size: 10px;
  color: #aeaeb2;
}

/* 工具详情 */
.tool-detail {
  padding: 0 10px 10px;
  border-top: 1px solid #f0f0f5;
}

.detail-label {
  font-size: 11px;
  font-weight: 500;
  color: #86868b;
  margin: 8px 0 4px;
}

.detail-code {
  background: #f5f5f7;
  border-radius: 6px;
  padding: 8px 10px;
  font-size: 11px;
  line-height: 1.5;
  font-family: 'SF Mono', 'Fira Code', monospace;
  color: #48484a;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 200px;
  overflow-y: auto;
  margin: 0;
}

/* 子 Agent */
.children-section {
  margin-top: 8px;
  padding-left: 12px;
  border-left: 2px solid #e5e5ea;
}
</style>
