<template>
  <view class="resume-upload">
    <view class="page-header">
      <text class="page-title">简历优化</text>
      <text class="page-desc">上传简历，AI 深度诊断项目薄弱点，给出具体优化建议</text>
    </view>

    <view class="model-card" v-if="hasOptions">
      <text class="model-card-label">分析模型</text>
      <view class="model-opts">
        <view v-for="m in options" :key="m.id" class="model-opt" :class="{ active: resumeModel === m.id }" @click="selectModel(m.id)">{{ m.label }}</view>
      </view>
    </view>

    <!-- 上传区域 -->
    <view class="upload-zone" @click="chooseFile">
      <view class="upload-icon-box">
        <MianIcon name="file" size="58rpx" color="#D9750A" stroke-width="1.7" />
      </view>
      <text class="upload-text">{{ file ? file.name : '点击上传简历' }}</text>
      <text class="upload-hint">支持 PDF、DOCX、JPG、PNG 格式</text>
      <text class="upload-size-hint" v-if="file">{{ formatSize(file.size) }}</text>
    </view>

    <!-- JD 表单 -->
    <view class="form-section" v-if="file">
      <text class="form-label">目标岗位 JD（可选）</text>
      <textarea
        class="form-textarea"
        v-model="jobDescription"
        placeholder="粘贴目标岗位的职位描述，AI 将针对性优化..."
        :maxlength="-1"
      />
    </view>

    <!-- 提交按钮 -->
    <view class="submit-btn" :class="{ disabled: !file || submitting }" @click="submitResume">
      <text>{{ submitting ? '分析中...' : '开始分析' }}</text>
    </view>

    <!-- 历史记录入口 -->
    <view class="history-link" @click="goHistory" v-if="hasHistory">
      <view class="history-link-left">
        <MianIcon name="clipboard" size="34rpx" color="#4A4A4A" stroke-width="1.7" />
        <text class="history-link-text">查看历史诊断记录</text>
      </view>
      <text class="history-link-arrow">→</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { BASE_URL, get, getToken } from '@/utils/request';
import { useModelToggle } from '@/composables/useModelToggle';
import MianIcon from '@/components/MianIcon.vue';

interface FileInfo { name: string; size: number; path: string; }
interface ResumeItem { id: number; position?: string; createTime?: string; }

const file = ref<FileInfo | null>(null);
const jobDescription = ref('');
const submitting = ref(false);
const hasHistory = ref(false);
const { currentModel: resumeModel, options, hasOptions, selectModel } = useModelToggle();

onMounted(async () => {
  try {
    const r = await get<ResumeItem[]>('/api/resume/list');
    hasHistory.value = Array.isArray(r.data) && r.data.length > 0;
  } catch {
    // 历史加载失败，不影响主流程
  }
});

function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

function chooseFile() {
  uni.chooseMessageFile({
    count: 1,
    type: 'file',
    extension: ['pdf', 'doc', 'docx', 'jpg', 'jpeg', 'png'],
    success: (res) => {
      const first = res.tempFiles[0];
      if (!first) return;
      file.value = {
        name: first.name,
        size: first.size,
        path: first.path,
      };
    },
  });
}

async function submitResume() {
  if (!file.value || submitting.value) return;
  submitting.value = true;

  try {
    const token = getToken();

    const res = await uni.uploadFile({
      url: `${BASE_URL}/api/resume/upload`,
      filePath: file.value.path,
      name: 'file',
      formData: {
        jobDescription: jobDescription.value.trim(),
        model: resumeModel.value,
      },
      header: {
        Authorization: 'Bearer ' + token,
      },
    });

    const data: { code: number; message?: string; data?: { resumeId: number } } = JSON.parse(res.data);
    if (data.code !== 200) throw new Error(data.message || '上传失败');

    if (data.data?.resumeId) {
      uni.navigateTo({ url: `/pages/resume/report?resumeId=${data.data.resumeId}` });
    } else {
      throw new Error('服务器返回数据异常');
    }
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '上传失败';
    uni.showToast({ title: msg, icon: 'error' });
    submitting.value = false;
  }
}

function goHistory() {
  uni.navigateTo({ url: '/pages/resume/history' });
}
</script>

<style lang="scss" scoped>
@import "@/styles/tokens.scss";

.resume-upload { min-height: 100vh; background: $bg-canvas; padding: 40rpx 28rpx; }

// 头部
.page-header { margin-bottom: 44rpx; }
.page-title {
  font-family: Georgia, serif; font-size: 40rpx; font-weight: 600;
  color: $text-main; display: block; margin-bottom: 12rpx; letter-spacing: -0.5px;
}
.page-desc { font-size: 26rpx; color: $text-muted; line-height: 1.6; display: block; }

.model-card { background: $bg-paper; border: 1px solid $border-light; border-radius: $radius-lg; padding: 24rpx; margin-bottom: 24rpx; box-shadow: $shadow-sm; }
.model-card-label { font-size: 24rpx; color: $text-muted; display: block; margin-bottom: 14rpx; }
.model-opts { display: flex; flex-wrap: wrap; gap: 10rpx; }
.model-opt { font-size: 22rpx; color: $text-light; background: $bg-surface; border: 1px solid $border-light; padding: 10rpx 22rpx; border-radius: $radius-full; }
.model-opt.active { color: #fff; background: $bg-dark; border-color: $bg-dark; }

// 上传区域
.upload-zone {
  background: $bg-paper; border: 2rpx dashed $border-medium;
  border-radius: $radius-lg; padding: 64rpx 40rpx;
  text-align: center; margin-bottom: 28rpx;
  display: flex; flex-direction: column; align-items: center;
}
.upload-zone:active { background: $bg-surface; }
.upload-icon-box {
  width: 120rpx; height: 120rpx; background: $bg-surface;
  border-radius: 24rpx; display: flex; align-items: center;
  justify-content: center; margin-bottom: 24rpx;
}
.upload-text {
  font-size: 28rpx; font-weight: 500; color: $text-main;
  display: block; margin-bottom: 10rpx; word-break: break-all;
}
.upload-hint { font-size: 24rpx; color: $text-light; display: block; }
.upload-size-hint { font-size: 22rpx; color: $text-light; margin-top: 8rpx; display: block; }

// 表单
.form-section {
  background: $bg-paper; border: 1px solid $border-light;
  border-radius: $radius-lg; padding: 28rpx; margin-bottom: 28rpx;
  box-shadow: $shadow-sm;
}
.form-label { font-size: 26rpx; font-weight: 600; color: $text-main; margin-bottom: 14rpx; display: block; }
.form-textarea {
  width: 100%; min-height: 160rpx; background: $bg-surface;
  border: 1px solid $border-medium; border-radius: $radius-md;
  padding: 20rpx; font-size: 26rpx; color: $text-main; box-sizing: border-box;
}

// 提交按钮
.submit-btn {
  width: 100%; height: 100rpx; background: $bg-dark;
  color: #fff; font-size: 30rpx; font-weight: 600;
  border-radius: $radius-lg; border: none;
  display: flex; align-items: center; justify-content: center;
}
.submit-btn:active { opacity: 0.9; }
.submit-btn.disabled { opacity: 0.4; }

// 历史入口
.history-link {
  display: flex; align-items: center; justify-content: space-between;
  margin-top: 32rpx; padding: 28rpx 24rpx;
  background: $bg-paper; border: 1px solid $border-light;
  border-radius: $radius-lg; box-shadow: $shadow-sm;
}
.history-link:active { background: $bg-surface; }
.history-link-left { display: flex; align-items: center; gap: 16rpx; }
.history-link-text { font-size: 26rpx; font-weight: 500; color: $text-main; }
.history-link-arrow { font-size: 28rpx; color: $text-light; }

@media (min-width: 1025px) { .resume-upload { max-width: 700px; margin: 0 auto; } }
</style>
