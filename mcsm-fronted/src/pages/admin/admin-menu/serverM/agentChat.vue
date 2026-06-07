<template>
  <div class="agent-chat-wrapper">
    <!-- 聊天头部 -->
    <div class="chat-header">
      <div class="chat-header__left">
        <span class="chat-header__icon">🤖</span>
        <div class="chat-header__info">
          <span class="chat-header__title">AI 运维助手</span>
          <span class="chat-header__subtitle">Minecraft 服务器智能体</span>
        </div>
      </div>
      <div class="chat-header__right">
        <span class="chat-header__session" v-if="sessionId">
          会话: {{ sessionId.substring(0, 8) }}...
        </span>
        <button class="chat-header__clear" @click="handleClear" :disabled="loading">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="3 6 5 6 21 6"></polyline>
            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
          </svg>
          清空
        </button>
      </div>
    </div>

    <!-- 聊天消息区域 -->
    <div class="chat-messages" ref="chatMessagesRef">
      <!-- 欢迎消息 -->
      <div v-if="messages.length === 0" class="welcome-message">
        <div class="welcome-icon">🤖</div>
        <div class="welcome-text">
          <p>你好！我是 Minecraft 服务器运维智能体。</p>
          <p>我可以帮你：</p>
          <ul>
            <li>查看服务器状态和玩家信息</li>
            <li>管理服务器启停和重启</li>
            <li>执行 RCON 指令</li>
            <li>查看性能指标和日志</li>
          </ul>
          <p>请随时向我提问！</p>
        </div>
      </div>

      <!-- 消息列表 -->
      <div v-for="(msg, index) in messages" :key="index" :class="['message-item', msg.role]">
        <div class="message-avatar">
          <span v-if="msg.role === 'user'">👤</span>
          <span v-else>🤖</span>
        </div>
        <div class="message-content">
          <!-- 事件流（思考、工具、回复穿插显示） -->
          <div v-if="msg.stream?.length" class="stream-container">
            <template v-for="(item, sIdx) in msg.stream" :key="sIdx">
              <!-- 思考过程 -->
              <div v-if="item.type === 'thinking'" class="stream-thinking">
                <div class="thinking-header" @click="item.collapsed = !item.collapsed">
                  <span>💭 思考过程</span>
                  <span class="thinking-toggle">{{ item.collapsed ? '展开' : '收起' }}</span>
                </div>
                <div v-if="!item.collapsed" class="thinking-content">{{ item.content }}</div>
              </div>

              <!-- 工具调用 -->
              <div v-if="item.type === 'tool'" class="stream-tool">
                <div class="tool-header" @click="item.expanded = !item.expanded">
                  <span class="tool-icon">{{ toolIcon(item.name) }}</span>
                  <span class="tool-name">{{ item.name }}</span>
                  <span class="tool-status" v-if="item.success !== undefined">
                    {{ item.success ? '✓' : '✗' }}
                  </span>
                  <span class="tool-toggle">{{ item.expanded ? '▼' : '▶' }}</span>
                </div>
                <div v-if="item.expanded" class="tool-detail">
                  <div v-if="item.args" class="tool-args">
                    <div class="detail-label">参数：</div>
                    <pre class="detail-code">{{ formatJson(item.args) }}</pre>
                  </div>
                  <div v-if="item.result" class="tool-result">
                    <div class="detail-label">结果：</div>
                    <pre class="detail-code">{{ item.result }}</pre>
                  </div>
                </div>
              </div>

              <!-- 回复文本 -->
              <div v-if="item.type === 'text'" class="stream-text">{{ item.content }}</div>
            </template>
          </div>

          <!-- 无 stream 的普通消息（兼容旧格式） -->
          <div v-if="!msg.stream?.length && msg.content" class="message-text">{{ msg.content }}</div>
          <div class="message-time">{{ msg.time }}</div>
        </div>
      </div>

      <!-- 进行中的流式输出 -->
      <div v-if="currentStream.length > 0" class="message-item assistant">
        <div class="message-avatar"><span>🤖</span></div>
        <div class="message-content">
          <div class="stream-container">
            <template v-for="(item, sIdx) in currentStream" :key="sIdx">
              <div v-if="item.type === 'thinking'" class="stream-thinking">
                <div class="thinking-header" @click="item.collapsed = !item.collapsed">
                  <span>💭 思考过程</span>
                  <span class="thinking-toggle">{{ item.collapsed ? '展开' : '收起' }}</span>
                </div>
                <div v-if="!item.collapsed" class="thinking-content">{{ item.content }}</div>
              </div>

              <div v-if="item.type === 'tool'" class="stream-tool">
                <div class="tool-header" @click="item.expanded = !item.expanded">
                  <span class="tool-icon">{{ toolIcon(item.name) }}</span>
                  <span class="tool-name">{{ item.name }}</span>
                  <span class="tool-status" v-if="item.success !== undefined">
                    {{ item.success ? '✓' : '✗' }}
                  </span>
                  <span class="tool-toggle">{{ item.expanded ? '▼' : '▶' }}</span>
                </div>
                <div v-if="item.expanded" class="tool-detail">
                  <div v-if="item.args" class="tool-args">
                    <div class="detail-label">参数：</div>
                    <pre class="detail-code">{{ formatJson(item.args) }}</pre>
                  </div>
                  <div v-if="item.result" class="tool-result">
                    <div class="detail-label">结果：</div>
                    <pre class="detail-code">{{ item.result }}</pre>
                  </div>
                </div>
              </div>

              <div v-if="item.type === 'text'" class="stream-text">{{ item.content }}</div>
            </template>
          </div>
        </div>
      </div>

      <!-- 加载动画 -->
      <div v-if="loading && currentStream.length === 0" class="message-item assistant">
        <div class="message-avatar"><span>🤖</span></div>
        <div class="message-content">
          <div class="typing-indicator">
            <span></span><span></span><span></span>
          </div>
        </div>
      </div>
    </div>

    <!-- 连接状态指示器 -->
    <div v-if="wsState !== 'connected'" class="ws-status-bar" :class="wsState">
      <span class="ws-status-dot"></span>
      <span v-if="wsState === 'disconnected'">连接已断开，正在重连...</span>
      <span v-if="wsState === 'reconnecting'">正在重新连接...</span>
    </div>

    <!-- 输入区域 -->
    <div class="chat-input-wrapper">
      <div class="input-container">
        <el-input
          v-model="inputMessage"
          type="textarea"
          :rows="1"
          :autosize="{ minRows: 1, maxRows: 4 }"
          placeholder="输入你的问题，例如：查看服务器状态..."
          @keydown.enter.prevent="handleSend"
          :disabled="loading"
          class="chat-input"
        />
        <button
          class="send-button"
          @click="handleSend"
          :disabled="!inputMessage.trim() || loading"
        >
          <span v-if="loading" class="send-spinner"></span>
          <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="22" y1="2" x2="11" y2="13"></line>
            <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
          </svg>
        </button>
      </div>
      <div class="input-tips">
        <span class="tip-item">Enter 发送</span>
        <span class="tip-item">Shift+Enter 换行</span>
        <span class="tip-item context-usage" v-if="contextPercent > 0" :class="contextLevel">
          上下文 {{ contextPercent }}%
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onActivated, onDeactivated, onUnmounted } from 'vue';
import client, { wsState, onReconnect } from '@/utils/wsClient';
import { ElMessage } from 'element-plus';
import request from '@/utils/request';

// Props
const props = defineProps({
  sessionId: {
    type: String,
    default: 'default'
  }
});

// 响应式数据
const messages = ref([]);
const inputMessage = ref('');
const loading = ref(false);
const chatMessagesRef = ref(null);

// 当前流式事件流
const currentStream = ref([]);

// 上下文用量百分比
const contextPercent = ref(0);
const contextLevel = computed(() => {
  if (contextPercent.value >= 80) return 'context-danger';
  if (contextPercent.value >= 60) return 'context-warn';
  return 'context-safe';
});

// WebSocket 订阅句柄
let chatSubscription = null;

// ACK 机制
let lastAckSeq = 0;
let ackTimer = null;

// 超时保护
let timeoutTimer = null;
const TIMEOUT_MS = 120000; // 120 秒

// 重连恢复回调注销
let unregisterReconnect = null;

// 生成唯一的会话 ID
const generateSessionId = () => {
  const timestamp = Date.now().toString(36);
  const random = Math.random().toString(36).substring(2, 8);
  return `${timestamp}-${random}`;
};

const sessionId = ref('');

const initSessionId = () => {
  if (props.sessionId && props.sessionId !== 'default') {
    sessionId.value = props.sessionId;
  } else {
    sessionId.value = generateSessionId();
  }
};

const getCurrentTime = () => {
  const now = new Date();
  const hours = String(now.getHours()).padStart(2, '0');
  const minutes = String(now.getMinutes()).padStart(2, '0');
  return `${hours}:${minutes}`;
};

// 订阅聊天消息
const doSubscribe = () => {
  if (chatSubscription) return;
  chatSubscription = client.subscribe('/user/queue/agent', (message) => {
    try {
      const data = JSON.parse(message.body);
      console.log('[AgentChat] 收到消息:', data);
      handleAgentMessage(data);
    } catch (e) {
      console.error('[AgentChat] 解析消息失败:', e, message.body);
    }
  });
};

const subscribeChat = () => {
  if (!client.active) {
    client.activate();
  }
  if (client.connected) {
    doSubscribe();
  }
  // 未连接时由 onReconnect 回调处理订阅
};

// 防抖发送 ACK
const scheduleAck = () => {
  if (ackTimer) return;
  ackTimer = setTimeout(() => {
    ackTimer = null;
    sendAck(lastAckSeq);
  }, 500);
};

const sendAck = (seq) => {
  if (!seq || !sessionId.value) return;
  request.post('/api/agent/ack', {
    session_id: sessionId.value,
    seq: seq
  }).catch(() => {});
};

// 超时保护
const resetTimeout = () => {
  if (timeoutTimer) {
    clearTimeout(timeoutTimer);
    timeoutTimer = null;
  }
  if (loading.value) {
    timeoutTimer = setTimeout(handleTimeout, TIMEOUT_MS);
  }
};

const handleTimeout = async () => {
  console.warn('[AgentChat] 超时，尝试恢复...');
  await recoverFromServer();
  // 如果恢复后 loading 仍然为 true，说明 agent 确实无响应
  if (loading.value) {
    messages.value.push({
      role: 'assistant',
      content: '⚠️ 响应超时，请检查网络后重试。',
      time: getCurrentTime()
    });
    loading.value = false;
  }
};

// 断线恢复
const handleReconnect = async () => {
  console.log('[AgentChat] WebSocket 重连，尝试恢复...');
  await recoverFromServer();
};

const recoverFromServer = async () => {
  try {
    const res = await request.get('/api/agent/result', {
      params: { session_id: sessionId.value }
    });
    if (res.code === 2000 && res.data) {
      const { reply, error, hasResult } = res.data;
      if (hasResult) {
        if (error) {
          messages.value.push({
            role: 'assistant',
            content: `❌ ${error}`,
            time: getCurrentTime()
          });
        } else if (reply) {
          // 补显示最终结果
          if (currentStream.value.length > 0) {
            finalizeAssistantReply(reply);
          } else {
            messages.value.push({
              role: 'assistant',
              content: reply,
              time: getCurrentTime()
            });
          }
        }
        loading.value = false;
        if (timeoutTimer) { clearTimeout(timeoutTimer); timeoutTimer = null; }
        nextTick(() => scrollToBottom());
      }
      // hasResult=false: agent 还在处理中，不干扰正常流式输出
    }
  } catch (e) {
    console.error('[AgentChat] 恢复失败:', e);
  }
};

// 处理 Agent 推送的消息
const handleAgentMessage = (rawData) => {
  const data = rawData.data || rawData;
  const type = data.type || rawData.type;
  const payload = data.data || data.payload || data;

  // 跟踪 seq 并调度 ACK
  const seq = payload?.seq || data?.seq;
  if (seq && seq > lastAckSeq) {
    lastAckSeq = seq;
    scheduleAck();
  }

  // 收到任何事件，重置超时计时器
  resetTimeout();

  switch (type) {
    case 'AGENT_START': {
      // 如果已有流式输出，先结束上一轮
      if (currentStream.value.length > 0) {
        finalizeAssistantReply('');
      }
      break;
    }

    case 'THINKING': {
      const content = typeof payload === 'string' ? payload : payload?.content || '';
      if (content) {
        if (content.trimStart().startsWith('{') && content.includes('"tool_calls"')) {
          break;
        }
        const cleaned = content
          .replace(/\n{3,}/g, '\n\n')
          .replace(/([^\n])\n([^\n])/g, '$1 $2');
        const lastItem = currentStream.value[currentStream.value.length - 1];
        if (lastItem?.type === 'thinking') {
          lastItem.content += cleaned;
        } else {
          currentStream.value.push({ type: 'thinking', content: cleaned, collapsed: false });
        }
      }
      break;
    }

    case 'TOOL_CALL': {
      currentStream.value.push({
        type: 'tool',
        name: payload?.toolName || 'unknown',
        args: payload?.args || '',
        result: null,
        success: undefined,
        expanded: false
      });
      break;
    }

    case 'TOOL_RESULT': {
      const toolName = payload?.toolName;
      const lastTool = [...currentStream.value].reverse().find(
        t => t.type === 'tool' && t.name === toolName && t.result === null
      );
      if (lastTool) {
        lastTool.success = payload?.success;
        lastTool.result = payload?.summary || '';
      }
      break;
    }

    case 'AGENT_DONE':
    case 'AGENT_ERROR':
      // 不再需要处理 scope 状态
      break;

    case 'CONTEXT_INFO':
      contextPercent.value = payload?.percent || 0;
      break;

    case 'REPLY_CHUNK': {
      const chunk = typeof payload === 'string' ? payload : payload?.content || '';
      if (chunk && chunk.trimStart().startsWith('{') && chunk.includes('"tool_calls"')) {
        break;
      }
      appendAssistantChunk(chunk);
      break;
    }

    case 'REPLY_DONE':
      finalizeAssistantReply(typeof payload === 'string' ? payload : payload?.content || payload?.result || '');
      loading.value = false;
      if (timeoutTimer) { clearTimeout(timeoutTimer); timeoutTimer = null; }
      break;

    case 'CONFIRMATION_NEEDED':
      finalizeAssistantReply(typeof payload === 'string' ? payload : payload?.reply || '请确认是否执行此操作。');
      loading.value = false;
      if (timeoutTimer) { clearTimeout(timeoutTimer); timeoutTimer = null; }
      break;

    case 'ERROR':
      // 先 finalize 已有流式内容
      if (currentStream.value.length > 0) {
        finalizeAssistantReply('');
      }
      messages.value.push({
        role: 'assistant',
        content: `❌ ${typeof payload === 'string' ? payload : payload?.message || 'Agent 服务异常'}`,
        time: getCurrentTime()
      });
      loading.value = false;
      streamingMsgIndex = -1;
      if (timeoutTimer) { clearTimeout(timeoutTimer); timeoutTimer = null; }
      break;

    default:
      if (payload?.content || payload?.message) {
        messages.value.push({
          role: 'assistant',
          content: payload.content || payload.message,
          time: getCurrentTime()
        });
        loading.value = false;
      }
      break;
  }

  nextTick(() => scrollToBottom());
};

// 流式追加助手回复 — 打字机效果
let streamingMsgIndex = -1;
let pendingText = '';
let typingTimer = null;
let streamActive = false; // 标记流是否还在进行中
let idleCount = 0; // 空转计数器

const appendAssistantChunk = (chunk) => {
  if (!chunk) return;
  streamActive = true;
  idleCount = 0;
  pendingText += chunk;
  if (!typingTimer) {
    typingTimer = setTimeout(typingLoop, 0);
  }
};

const typingLoop = () => {
  if (pendingText.length === 0) {
    if (!streamActive) {
      // 流已结束，停止打字机
      typingTimer = null;
      return;
    }
    // 流还在进行中，等待新数据，但限制最大空转次数（3秒）
    idleCount++;
    if (idleCount > 60) {
      console.warn('[AgentChat] typingLoop idle timeout, stopping');
      typingTimer = null;
      streamActive = false;
      return;
    }
    typingTimer = setTimeout(typingLoop, 50);
    return;
  }

  idleCount = 0;
  const char = pendingText[0];
  pendingText = pendingText.slice(1);

  const lastItem = currentStream.value[currentStream.value.length - 1];
  if (lastItem?.type === 'text') {
    lastItem.content += char;
  } else {
    currentStream.value.push({ type: 'text', content: char });
  }

  if (streamingMsgIndex === -1) streamingMsgIndex = 0;

  const delay = pendingText.length > 100 ? 8
              : pendingText.length > 20 ? 20
              : 30;
  typingTimer = setTimeout(typingLoop, delay);
};

// 流式结束
const finalizeAssistantReply = (fullReply) => {
  // 停止打字机
  streamActive = false;
  if (typingTimer) {
    clearTimeout(typingTimer);
    typingTimer = null;
  }
  // 清空剩余缓冲
  if (pendingText) {
    const lastItem = currentStream.value[currentStream.value.length - 1];
    if (lastItem?.type === 'text') {
      lastItem.content += pendingText;
    } else {
      currentStream.value.push({ type: 'text', content: pendingText });
    }
    pendingText = '';
  }

  if (currentStream.value.length > 0 || fullReply) {
    // 如果有 fullReply，替换最后一个 text 块
    if (fullReply) {
      const lastItem = currentStream.value[currentStream.value.length - 1];
      if (lastItem?.type === 'text') {
        lastItem.content = fullReply;
      } else {
        currentStream.value.push({ type: 'text', content: fullReply });
      }
    }

    messages.value.push({
      role: 'assistant',
      content: fullReply || '',
      time: getCurrentTime(),
      stream: [...currentStream.value]
    });
  }

  currentStream.value = [];
  streamingMsgIndex = -1;
};

const toolIcon = (name) => {
  if (name?.includes('read') || name?.includes('search') || name?.includes('grep')) return '📖';
  if (name?.includes('write') || name?.includes('edit')) return '📝';
  if (name?.includes('delete')) return '🗑️';
  if (name?.includes('list')) return '📂';
  if (name?.includes('spawn')) return '🤖';
  if (name?.includes('server') || name?.includes('start') || name?.includes('stop')) return '🖥️';
  if (name?.includes('rcon') || name?.includes('command')) return '⌨️';
  if (name?.includes('think_more')) return '🔄';
  return '🔧';
};

const formatJson = (str) => {
  try {
    return JSON.stringify(JSON.parse(str), null, 2);
  } catch {
    return str;
  }
};

// 发送消息
const handleSend = () => {
  const message = inputMessage.value.trim();
  if (!message || loading.value) return;

  messages.value.push({
    role: 'user',
    content: message,
    time: getCurrentTime()
  });

  inputMessage.value = '';
  loading.value = true;
  streamingMsgIndex = -1;
  currentStream.value = [];
  lastAckSeq = 0;

  nextTick(() => scrollToBottom());

  // 启动超时计时器
  resetTimeout();

  if (client.connected) {
    client.publish({
      destination: '/app/agent.send',
      body: JSON.stringify({
        message: message,
        session_id: sessionId.value
      })
    });
  } else {
    messages.value.push({
      role: 'assistant',
      content: '❌ WebSocket 未连接，请刷新页面重试。',
      time: getCurrentTime()
    });
    loading.value = false;
  }
};

// 清空对话
const handleClear = () => {
  messages.value = [];
  currentStream.value = [];
  streamingMsgIndex = -1;
  pendingText = '';
  streamActive = false;
  idleCount = 0;
  contextPercent.value = 0;
  if (typingTimer) {
    clearTimeout(typingTimer);
    typingTimer = null;
  }
  ElMessage.success('对话已清空');
};

// 销毁会话
const destroySession = () => {
  if (client.connected) {
    client.publish({
      destination: '/app/agent.destroy',
      body: JSON.stringify({ session_id: sessionId.value })
    });
  }
};

const scrollToBottom = () => {
  if (chatMessagesRef.value) {
    chatMessagesRef.value.scrollTop = chatMessagesRef.value.scrollHeight;
  }
};

const handleKeydown = (e) => {
  if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
    e.preventDefault();
    handleClear();
  }
};

onMounted(() => {
  initSessionId();
});

onActivated(() => {
  // 清理旧的重连回调，防止累积
  if (unregisterReconnect) { unregisterReconnect(); unregisterReconnect = null; }

  subscribeChat();
  unregisterReconnect = onReconnect(() => {
    if (chatSubscription) {
      chatSubscription.unsubscribe();
      chatSubscription = null;
    }
    doSubscribe();
    handleReconnect();
  });
  document.addEventListener('keydown', handleKeydown);
});

onDeactivated(() => {
  document.removeEventListener('keydown', handleKeydown);
  if (unregisterReconnect) { unregisterReconnect(); unregisterReconnect = null; }
  if (typingTimer) { clearTimeout(typingTimer); typingTimer = null; }
  if (timeoutTimer) { clearTimeout(timeoutTimer); timeoutTimer = null; }
  if (ackTimer) { clearTimeout(ackTimer); ackTimer = null; }
});

onUnmounted(() => {
  destroySession();
  if (chatSubscription) {
    chatSubscription.unsubscribe();
    chatSubscription = null;
  }
  if (typingTimer) {
    clearTimeout(typingTimer);
    typingTimer = null;
  }
  if (timeoutTimer) {
    clearTimeout(timeoutTimer);
    timeoutTimer = null;
  }
  if (ackTimer) {
    clearTimeout(ackTimer);
    ackTimer = null;
  }
  if (unregisterReconnect) {
    unregisterReconnect();
    unregisterReconnect = null;
  }
  document.removeEventListener('keydown', handleKeydown);
});
</script>

<style scoped>
.agent-chat-wrapper {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid rgba(0, 0, 0, 0.04);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

/* ==================== 聊天头部 ==================== */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  border-bottom: 1px solid #f2f2f7;
  background: #fafafa;
}

.chat-header__left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chat-header__icon { font-size: 24px; }

.chat-header__info {
  display: flex;
  flex-direction: column;
}

.chat-header__title {
  font-size: 15px;
  font-weight: 600;
  color: #1d1d1f;
}

.chat-header__subtitle {
  font-size: 11px;
  color: #86868b;
}

.chat-header__right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-header__session {
  font-size: 11px;
  color: #aeaeb2;
  font-family: 'SF Mono', monospace;
}

.chat-header__clear {
  display: flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: 1px solid #d1d1d6;
  color: #86868b;
  font-size: 12px;
  padding: 5px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.chat-header__clear:hover:not(:disabled) {
  background: #f5f5f7;
  color: #ff3b30;
  border-color: #ff3b30;
}

.chat-header__clear:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* ==================== 聊天消息区域 ==================== */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f8f9fa;
}

.chat-messages::-webkit-scrollbar { width: 6px; }
.chat-messages::-webkit-scrollbar-track { background: transparent; }
.chat-messages::-webkit-scrollbar-thumb { background-color: #d1d1d6; border-radius: 3px; }
.chat-messages::-webkit-scrollbar-thumb:hover { background-color: #aeaeb2; }

/* ==================== 欢迎消息 ==================== */
.welcome-message {
  display: flex;
  gap: 16px;
  padding: 24px;
  background: linear-gradient(135deg, #0071e3 0%, #5856d6 100%);
  border-radius: 16px;
  color: #ffffff;
  margin-bottom: 20px;
}

.welcome-icon { font-size: 48px; line-height: 1; }
.welcome-text { flex: 1; }
.welcome-text p { margin: 0 0 8px 0; font-size: 14px; line-height: 1.6; }
.welcome-text ul { margin: 8px 0; padding-left: 20px; }
.welcome-text ul li { margin-bottom: 4px; font-size: 13px; }

/* ==================== 消息项 ==================== */
.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  animation: fadeIn 0.3s ease-in;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.message-item.user { flex-direction: row-reverse; }

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
  background: #ffffff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.message-content {
  max-width: 75%;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.message-item.user .message-content { align-items: flex-end; }
.message-item.assistant .message-content { align-items: flex-start; }

.message-text {
  padding: 12px 16px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.6;
  word-wrap: break-word;
  white-space: pre-wrap;
}

.message-item.user .message-text {
  background: #0071e3;
  color: #ffffff;
  border-bottom-right-radius: 4px;
}

.message-item.assistant .message-text {
  background: #ffffff;
  color: #1d1d1f;
  border: 1px solid #e5e5ea;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.message-time {
  font-size: 11px;
  color: #aeaeb2;
  padding: 0 4px;
}

/* ==================== 事件流 ==================== */
.stream-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

/* 思考过程 */
.stream-thinking {
  border: 1px solid #e8e8ed;
  border-radius: 10px;
  overflow: hidden;
  background: #fafafa;
}

.thinking-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  cursor: pointer;
  font-size: 13px;
  color: #86868b;
  transition: background 0.2s;
}

.thinking-header:hover { background: #f0f0f5; }

.thinking-toggle {
  font-size: 11px;
  color: #0071e3;
}

.thinking-content {
  padding: 0 12px 10px;
  font-size: 13px;
  line-height: 1.6;
  color: #48484a;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 300px;
  overflow-y: auto;
}

/* 工具调用 */
.stream-tool {
  border: 1px solid #e8e8ed;
  border-radius: 10px;
  overflow: hidden;
  background: #ffffff;
}

.tool-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  cursor: pointer;
  transition: background 0.2s;
}

.tool-header:hover { background: #f8f9fa; }

.tool-icon { font-size: 14px; }

.tool-name {
  font-size: 12px;
  font-weight: 600;
  color: #1d1d1f;
  font-family: 'SF Mono', 'Fira Code', monospace;
}

.tool-status { font-size: 12px; margin-left: auto; }

.tool-toggle {
  font-size: 10px;
  color: #aeaeb2;
}

.tool-detail {
  padding: 0 12px 10px;
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

/* 回复文本 */
.stream-text {
  padding: 12px 16px;
  background: #ffffff;
  border: 1px solid #e5e5ea;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  color: #1d1d1f;
  word-wrap: break-word;
  white-space: pre-wrap;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

/* ==================== 打字动画 ==================== */
.typing-indicator {
  padding: 16px 20px;
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e5e5ea;
  display: flex;
  gap: 4px;
  align-items: center;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #aeaeb2;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-indicator span:nth-child(1) { animation-delay: -0.32s; }
.typing-indicator span:nth-child(2) { animation-delay: -0.16s; }
.typing-indicator span:nth-child(3) { animation-delay: 0s; }

@keyframes typing {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

/* ==================== 连接状态指示器 ==================== */
.ws-status-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 20px;
  font-size: 12px;
  font-weight: 500;
  border-top: 1px solid #f2f2f7;
}

.ws-status-bar.disconnected {
  background: #fff3e0;
  color: #e65100;
}

.ws-status-bar.reconnecting {
  background: #fff8e1;
  color: #f57f17;
}

.ws-status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.ws-status-bar.disconnected .ws-status-dot {
  background: #e65100;
  animation: pulse-dot 1.5s ease-in-out infinite;
}

.ws-status-bar.reconnecting .ws-status-dot {
  background: #f57f17;
  animation: pulse-dot 1s ease-in-out infinite;
}

@keyframes pulse-dot {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

/* ==================== 输入区域 ==================== */
.chat-input-wrapper {
  border-top: 1px solid #f2f2f7;
  background: #ffffff;
  padding: 14px 20px;
}

.input-container {
  display: flex;
  gap: 10px;
  align-items: flex-end;
}

.chat-input { flex: 1; }

.chat-input :deep(.el-textarea__inner) {
  border-radius: 12px;
  background: #f5f5f7;
  border: 1px solid #e5e5ea;
  padding: 10px 14px;
  font-size: 14px;
  line-height: 1.5;
  transition: all 0.2s;
  font-family: -apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.chat-input :deep(.el-textarea__inner):focus {
  background: #ffffff;
  border-color: #0071e3;
  box-shadow: 0 0 0 3px rgba(0, 113, 227, 0.15);
}

.send-button {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: #0071e3;
  border: none;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}

.send-button:hover:not(:disabled) { background: #0077ed; }
.send-button:active:not(:disabled) { transform: scale(0.95); }
.send-button:disabled { opacity: 0.4; cursor: not-allowed; }

.send-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.input-tips {
  display: flex;
  gap: 16px;
  margin-top: 8px;
  padding: 0 4px;
}

.tip-item { font-size: 11px; color: #aeaeb2; }

.context-usage {
  margin-left: auto;
  font-weight: 500;
  font-variant-numeric: tabular-nums;
}

.context-safe { color: #34c759; }
.context-warn { color: #ff9500; }
.context-danger { color: #ff3b30; }

/* ==================== 响应式 ==================== */
@media (max-width: 768px) {
  .message-content { max-width: 85%; }
  .chat-header__session { display: none; }
}
</style>
