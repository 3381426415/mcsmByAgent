<template>
  <div v-if="visible" class="settings-overlay" @click.self="close">
    <div class="settings-panel">
      <!-- 头部 -->
      <div class="settings-header">
        <span class="settings-title">系统设置</span>
        <button class="close-btn" @click="close">&times;</button>
      </div>

      <!-- 内容区 -->
      <div class="settings-body">
        <!-- 左侧 TAB 菜单 -->
        <aside class="settings-tabs">
          <div
            v-for="tab in tabs"
            :key="tab.key"
            :class="['tab-item', { active: activeTab === tab.key }]"
            @click="activeTab = tab.key"
          >
            {{ tab.label }}
          </div>
        </aside>

        <!-- 右侧内容 -->
        <main class="settings-content">
          <div v-if="activeTab === 'agent'" class="config-section">
            <h3 class="section-title">AI 模型配置</h3>
            <p class="section-desc">配置 OpenAI 兼容格式的 AI 模型连接参数，修改后 Agent 端将自动获取。</p>

            <el-form label-position="top" class="config-form">
              <!-- LLM 厂商 -->
              <el-form-item label="LLM 厂商">
                <div class="provider-row">
                  <el-select
                    v-model="form.provider"
                    placeholder="选择 LLM 厂商"
                    style="flex: 1"
                    @change="onProviderChange"
                  >
                    <el-option
                      v-for="p in providers"
                      :key="p.id"
                      :label="p.name"
                      :value="p.id"
                    />
                  </el-select>
                  <el-button @click="showAddDialog = true" style="margin-left: 8px">
                    <el-icon><Plus /></el-icon>
                  </el-button>
                </div>
                <!-- 自定义厂商列表 -->
                <div v-if="customProviders.length > 0" class="custom-providers-list">
                  <span class="custom-providers-label">自定义厂商：</span>
                  <div v-for="p in customProviders" :key="p.id" class="custom-provider-tag">
                    <span>{{ p.name }}</span>
                    <el-icon class="delete-tag-icon" @click="handleDeleteProvider(p)">
                      <Close />
                    </el-icon>
                  </div>
                </div>
                <span class="field-hint">选择厂商后自动填充 Base URL，也可手动修改</span>
              </el-form-item>

              <!-- Base URL -->
              <el-form-item label="Base URL">
                <el-input
                  v-model="form.baseUrl"
                  placeholder="https://api.openai.com/v1"
                />
                <span class="field-hint">API 基础地址，需包含 /v1</span>
              </el-form-item>

              <!-- API Key -->
              <el-form-item label="API Key">
                <el-input
                  v-model="form.apiKey"
                  :type="showApiKey ? 'text' : 'password'"
                  placeholder="sk-xxxxxx"
                >
                  <template #suffix>
                    <el-icon class="toggle-visibility" @click="showApiKey = !showApiKey">
                      <View v-if="showApiKey" />
                      <Hide v-else />
                    </el-icon>
                  </template>
                </el-input>
                <span class="field-hint">认证密钥</span>
              </el-form-item>

              <!-- 获取模型列表 -->
              <el-form-item>
                <el-button
                  @click="fetchModels"
                  :loading="loadingModels"
                  :disabled="!form.apiKey || !form.baseUrl"
                >
                  获取模型列表
                </el-button>
              </el-form-item>

              <!-- Pro 模型 -->
              <el-form-item label="Pro 模型（决策智能体）">
                <el-select
                  v-model="form.proModel"
                  filterable
                  allow-create
                  clearable
                  placeholder="留空则使用默认模型"
                  style="width: 100%"
                  :loading="loadingModels"
                >
                  <el-option
                    v-for="m in availableModels"
                    :key="m.id"
                    :label="m.id"
                    :value="m.id"
                  />
                </el-select>
                <span class="field-hint">用于复杂推理和任务规划的高质量模型</span>
              </el-form-item>

              <!-- Flash 模型 -->
              <el-form-item label="Flash 模型（子任务执行）">
                <el-select
                  v-model="form.flashModel"
                  filterable
                  allow-create
                  clearable
                  placeholder="留空则使用默认模型"
                  style="width: 100%"
                  :loading="loadingModels"
                >
                  <el-option
                    v-for="m in availableModels"
                    :key="m.id"
                    :label="m.id"
                    :value="m.id"
                  />
                </el-select>
                <span class="field-hint">用于快速执行子任务的轻量模型</span>
              </el-form-item>

              <el-form-item>
                <el-button type="primary" @click="handleSave" :loading="saving">
                  保存配置
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </main>
      </div>

      <!-- 添加自定义厂商对话框 -->
      <el-dialog
        v-model="showAddDialog"
        title="添加自定义厂商"
        width="440px"
        :close-on-click-modal="false"
        append-to-body
      >
        <el-form label-position="top" class="add-provider-form">
          <el-form-item label="厂商标识" required>
            <el-input
              v-model="addForm.providerId"
              placeholder="如: my-openai"
              @input="filterProviderId"
            />
            <span class="field-hint">仅限英文字母、数字、下划线和横线，创建后不可修改</span>
          </el-form-item>
          <el-form-item label="厂商名称" required>
            <el-input v-model="addForm.name" placeholder="如: 我的 OpenAI" />
          </el-form-item>
          <el-form-item label="Base URL" required>
            <el-input v-model="addForm.baseUrl" placeholder="https://api.example.com/v1" />
          </el-form-item>
          <el-form-item label="Models Endpoint">
            <el-input v-model="addForm.modelsEndpoint" placeholder="https://api.example.com/models" />
            <span class="field-hint">获取模型列表的完整 URL 地址</span>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showAddDialog = false">取消</el-button>
          <el-button type="primary" @click="handleAddProvider" :loading="addingProvider">
            添加
          </el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { View, Hide, Plus, Close } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '@/utils/request';

const props = defineProps({
  visible: { type: Boolean, default: false }
});

const emit = defineEmits(['update:visible']);

const activeTab = ref('agent');
const showApiKey = ref(false);
const saving = ref(false);
const loadingModels = ref(false);
const availableModels = ref([]);
const providers = ref([]);
const showAddDialog = ref(false);
const addingProvider = ref(false);

const tabs = [
  { key: 'agent', label: 'AI 模型配置' }
];

const form = ref({
  provider: '',
  baseUrl: '',
  apiKey: '',
  proModel: '',
  flashModel: ''
});

const addForm = ref({
  providerId: '',
  name: '',
  baseUrl: '',
  modelsEndpoint: ''
});

const customProviders = computed(() => {
  return providers.value.filter(p => !p.builtin);
});

const filterProviderId = (val) => {
  addForm.value.providerId = val.replace(/[^a-zA-Z0-9_-]/g, '');
};

const close = () => {
  emit('update:visible', false);
};

const onProviderChange = (providerId) => {
  const p = providers.value.find(item => item.id === providerId);
  if (p && p.baseUrl) {
    form.value.baseUrl = p.baseUrl;
  }
  form.value.apiKey = '';
  form.value.proModel = '';
  form.value.flashModel = '';
  availableModels.value = [];
};

const fetchProviders = async () => {
  try {
    const res = await request.get('/api/admin/ai-config/providers', { silent: true });
    if (res.data) {
      providers.value = res.data;
    }
  } catch (e) {
    console.error('获取厂商列表失败:', e);
  }
};

const fetchModels = async () => {
  if (!form.value.apiKey) {
    ElMessage.warning('请先填写 API Key');
    return;
  }
  if (!form.value.baseUrl) {
    ElMessage.warning('请先填写 Base URL');
    return;
  }

  loadingModels.value = true;
  try {
    const res = await request.post('/api/admin/ai-config/models', {
      baseUrl: form.value.baseUrl,
      apiKey: form.value.apiKey,
      provider: form.value.provider
    });
    if (res.code === 2000 && res.data) {
      availableModels.value = res.data;
      if (res.data.length > 0) {
        ElMessage.success(`获取到 ${res.data.length} 个模型`);
      } else {
        ElMessage.info('未获取到可用模型');
      }
    } else {
      ElMessage.error(res.msg || '获取模型列表失败');
    }
  } catch (e) {
    ElMessage.error(e.friendlyMsg || '获取模型列表失败');
  } finally {
    loadingModels.value = false;
  }
};

const fetchConfig = async () => {
  try {
    const res = await request.get('/api/admin/ai-config', { silent: true });
    if (res.data) {
      form.value.provider = res.data.provider || '';
      form.value.baseUrl = res.data.baseUrl || '';
      form.value.apiKey = res.data.apiKey || '';
      form.value.proModel = res.data.proModel || '';
      form.value.flashModel = res.data.flashModel || '';
    }
  } catch (e) {
    ElMessage.error(e.friendlyMsg || '获取配置失败');
  }
};

const handleSave = async () => {
  saving.value = true;
  try {
    await request.put('/api/admin/ai-config', form.value);
    ElMessage.success('保存成功');
    close();
  } catch (e) {
    ElMessage.error(e.friendlyMsg || '保存失败');
  } finally {
    saving.value = false;
  }
};

const handleAddProvider = async () => {
  const id = addForm.value.providerId?.trim();
  if (!id) {
    ElMessage.warning('请填写厂商标识');
    return;
  }
  if (!/^[a-zA-Z0-9_-]+$/.test(id)) {
    ElMessage.warning('厂商标识只能包含英文字母、数字、下划线和横线');
    return;
  }
  if (!addForm.value.name?.trim()) {
    ElMessage.warning('请填写厂商名称');
    return;
  }
  if (!addForm.value.baseUrl?.trim()) {
    ElMessage.warning('请填写 Base URL');
    return;
  }

  addingProvider.value = true;
  try {
    await request.post('/api/admin/ai-config/providers', {
      providerId: id,
      name: addForm.value.name.trim(),
      baseUrl: addForm.value.baseUrl.trim(),
      modelsEndpoint: addForm.value.modelsEndpoint?.trim() || '/models'
    });
    ElMessage.success('添加成功');
    showAddDialog.value = false;
    addForm.value = { providerId: '', name: '', baseUrl: '', modelsEndpoint: '' };
    await fetchProviders();
  } catch (e) {
    ElMessage.error(e.friendlyMsg || '添加失败');
  } finally {
    addingProvider.value = false;
  }
};

const handleDeleteProvider = async (provider) => {
  try {
    await ElMessageBox.confirm(
      `确定删除厂商「${provider.name}」吗？`,
      '确认删除',
      { type: 'warning' }
    );
  } catch {
    return; // 用户取消
  }

  try {
    await request.delete(`/api/admin/ai-config/providers/${provider.id}`);
    ElMessage.success('删除成功');
    if (form.value.provider === provider.id) {
      form.value.provider = '';
      form.value.baseUrl = '';
    }
    await fetchProviders();
  } catch (e) {
    ElMessage.error(e.friendlyMsg || '删除失败');
  }
};

watch(() => props.visible, (val) => {
  if (val) {
    fetchProviders();
    fetchConfig();
  }
});
</script>

<style scoped>
.settings-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 2000;
  display: flex;
  justify-content: flex-end;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.settings-panel {
  width: 680px;
  max-width: 90vw;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  box-shadow: -4px 0 24px rgba(0, 0, 0, 0.12);
  animation: slideIn 0.25s ease;
}

@keyframes slideIn {
  from { transform: translateX(100%); }
  to { transform: translateX(0); }
}

.settings-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.settings-title {
  font-size: 16px;
  font-weight: 600;
  color: #1d1d1f;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #999;
  cursor: pointer;
  padding: 0 4px;
  line-height: 1;
  transition: color 0.2s;
}

.close-btn:hover {
  color: #333;
}

.settings-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.settings-tabs {
  width: 160px;
  padding: 16px 0;
  border-right: 1px solid #f0f0f0;
  background: #fafafa;
  flex-shrink: 0;
}

.tab-item {
  padding: 10px 20px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
  border-left: 3px solid transparent;
}

.tab-item:hover {
  background: #f0f0f0;
  color: #333;
}

.tab-item.active {
  color: #0071e3;
  background: #fff;
  border-left-color: #0071e3;
  font-weight: 500;
}

.settings-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.config-section {
  max-width: 480px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0 0 8px 0;
}

.section-desc {
  font-size: 13px;
  color: #86868b;
  margin: 0 0 24px 0;
}

.config-form :deep(.el-form-item__label) {
  font-size: 13px;
  font-weight: 500;
  color: #333;
}

.field-hint {
  display: block;
  font-size: 12px;
  color: #aaa;
  margin-top: 4px;
}

.toggle-visibility {
  cursor: pointer;
  color: #999;
}

.toggle-visibility:hover {
  color: #333;
}

.provider-row {
  display: flex;
  align-items: center;
  width: 100%;
}

.custom-providers-list {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.custom-providers-label {
  font-size: 12px;
  color: #999;
}

.custom-provider-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  background: #f0f0f0;
  border-radius: 4px;
  font-size: 12px;
  color: #666;
}

.delete-tag-icon {
  cursor: pointer;
  font-size: 12px;
  color: #999;
  transition: color 0.2s;
}

.delete-tag-icon:hover {
  color: #f56c6c;
}

.add-provider-form :deep(.el-form-item__label) {
  font-size: 13px;
  font-weight: 500;
  color: #333;
}
</style>
