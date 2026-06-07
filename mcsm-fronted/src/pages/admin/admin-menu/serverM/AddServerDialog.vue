<template>
  <el-dialog
    v-model="dialogVisible"
    title="创建服务器实例"
    width="550px"
    :close-on-click-modal="false"
    @close="resetForm"
  >
    <el-form :model="form" label-width="100px" @submit.prevent>
      <el-form-item label="服务器名称">
        <el-input v-model="form.name" placeholder="例如：生存服" />
      </el-form-item>
      <el-form-item label="端口">
        <el-input-number v-model="form.port" :min="1024" :max="65535" />
      </el-form-item>
      <el-form-item label="Java 参数">
        <el-input v-model="form.javaArgs" placeholder="-Xmx2G -Xms1G" />
      </el-form-item>

      <!-- 模板选择 -->
      <el-form-item label="快速模板" v-if="templates.length > 0">
        <div class="template-list">
          <div
            v-for="tpl in templates"
            :key="tpl.id"
            class="template-item"
            :class="{ active: selectedTemplate === tpl.id }"
            @click="selectTemplate(tpl)"
          >
            <div class="template-name">{{ tpl.name }}</div>
            <div class="template-meta">{{ tpl.category }} · {{ tpl.size }}</div>
          </div>
        </div>
      </el-form-item>

      <el-form-item label="服务器文件" v-if="!selectedTemplate">
        <el-upload
          drag
          :auto-upload="false"
          :limit="1"
          accept=".zip,.jar"
          :on-change="onFileChange"
          :file-list="fileList"
        >
          <el-icon size="40"><UploadFilled /></el-icon>
          <div class="upload-text">将 .zip 或 .jar 文件拖到此处</div>
          <template #tip>
            <div class="upload-tip">支持上传核心 jar 或包含服务端文件的 zip 压缩包</div>
          </template>
        </el-upload>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button
        type="primary"
        @click="handleSubmit"
        :loading="submitting"
        :disabled="!selectedFile && !selectedTemplate"
      >
        {{ selectedTemplate ? '从模板创建' : '上传并注册' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { UploadFilled } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import request from '@/utils/request';

const props = defineProps({ visible: Boolean });
const emit = defineEmits(['update:visible', 'added']);

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
});

const submitting = ref(false);
const selectedFile = ref(null);
const fileList = ref([]);
const form = ref({ name: '', port: 25565, javaArgs: '-Xmx2G -Xms1G' });

const templates = ref([]);
const selectedTemplate = ref(null);

// 打开弹窗时加载模板列表
watch(() => props.visible, async (val) => {
  if (val) {
    try {
      const res = await request.get('/api/server/templates');
      if (res.code === 2000) {
        templates.value = res.data || [];
      }
    } catch (e) {
      console.warn('加载模板列表失败', e);
    }
  }
});

const selectTemplate = (tpl) => {
  if (selectedTemplate.value === tpl.id) {
    selectedTemplate.value = null;
  } else {
    selectedTemplate.value = tpl.id;
    // 自动填充名称（如果为空）
    if (!form.value.name) {
      form.value.name = tpl.name;
    }
  }
};

const resetForm = () => {
  form.value = { name: '', port: 25565, javaArgs: '-Xmx2G -Xms1G' };
  selectedFile.value = null;
  fileList.value = [];
  selectedTemplate.value = null;
};

const onFileChange = (file) => {
  selectedFile.value = file.raw;
  fileList.value = [file];
};

const handleSubmit = async () => {
  if (!form.value.name) { ElMessage.warning('请填写服务器名称'); return; }

  submitting.value = true;
  try {
    if (selectedTemplate.value) {
      // 模板创建
      const res = await request.post('/api/server/use-template', {
        templateId: selectedTemplate.value,
        name: form.value.name,
        port: String(form.value.port),
        javaArgs: form.value.javaArgs
      });
      if (res.code === 2000) {
        ElMessage.success('服务器创建成功');
        emit('added');
        dialogVisible.value = false;
      } else {
        ElMessage.error(res.msg || '创建失败');
      }
    } else {
      // 文件上传
      if (!selectedFile.value) { ElMessage.warning('请选择服务器文件'); return; }
      const fd = new FormData();
      fd.append('file', selectedFile.value);
      fd.append('name', form.value.name);
      fd.append('port', String(form.value.port));
      fd.append('javaArgs', form.value.javaArgs);

      const res = await request.post('/api/server/upload', fd, {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 120000
      });
      if (res.code === 2000) {
        ElMessage.success('服务器上传成功');
        emit('added');
        dialogVisible.value = false;
      } else {
        ElMessage.error(res.msg || '上传失败');
      }
    }
  } catch (e) {
    ElMessage.error(e.friendlyMsg || '操作失败');
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
.upload-text { font-size: 14px; color: #606266; margin-top: 8px; }
.upload-tip { font-size: 12px; color: #909399; }
.template-list { display: flex; flex-wrap: wrap; gap: 8px; width: 100%; }
.template-item {
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 10px 14px;
  cursor: pointer;
  transition: all 0.2s;
  flex: 1;
  min-width: 120px;
}
.template-item:hover { border-color: #409eff; background: #ecf5ff; }
.template-item.active { border-color: #409eff; background: #ecf5ff; }
.template-name { font-size: 14px; font-weight: 500; color: #303133; }
.template-meta { font-size: 12px; color: #909399; margin-top: 4px; }
</style>
