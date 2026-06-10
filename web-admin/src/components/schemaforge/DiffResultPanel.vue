<template>
  <bz-card
    v-loading="loading"
    class="diff-result-panel"
    element-loading-text="正在对比数据库结构..."
  >
    <bz-empty
      v-if="!loading && !result"
      description="请选择起始库和目标库后开始对比"
    />

    <template v-else-if="result">
      <div class="summary-head">
        <div>
          <div class="summary-title">结构差异摘要</div>
          <div class="summary-subtitle">
            起始库：{{ referenceLabel }} → 目标库：{{ targetLabel }}
          </div>
          <div class="summary-subtitle">SQL 结果会将起始库结构变更为目标库结构。</div>
        </div>
      </div>

      <div class="summary-grid">
        <div class="stat-card added">
          <div class="stat-label">新增对象</div>
          <div class="stat-value">{{ result.addedCount }}</div>
        </div>
        <div class="stat-card removed">
          <div class="stat-label">删除对象</div>
          <div class="stat-value">{{ result.removedCount }}</div>
        </div>
        <div class="stat-card changed">
          <div class="stat-label">变更对象</div>
          <div class="stat-value">{{ result.changedCount }}</div>
        </div>
      </div>

      <div class="result-toolbar">
        <div class="mode-switch">
          <bz-button
            size="small"
            :type="mode === 'SQL' ? 'primary' : 'default'"
            @click="mode = 'SQL'"
            >SQL</bz-button
          >
          <bz-button
            size="small"
            :type="mode === 'XML' ? 'primary' : 'default'"
            @click="mode = 'XML'"
            >XML</bz-button
          >
        </div>
        <bz-button
          size="small"
          @click="copyCurrent"
          >复制{{ mode }}内容</bz-button
        >
      </div>

      <pre class="result-code">{{ currentContent }}</pre>
    </template>
  </bz-card>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";

import type { SchemaDiffResult } from "../../types/schemaforge";
import { message } from "../../utils/message";

const props = defineProps<{
  loading: boolean;
  result: SchemaDiffResult | null;
  referenceLabel: string;
  targetLabel: string;
}>();

const mode = ref<"SQL" | "XML">("SQL");

const currentContent = computed(() => {
  if (!props.result) {
    return "";
  }
  return mode.value === "SQL" ? props.result.changeSql : props.result.changeLogXml;
});

async function copyCurrent() {
  if (!currentContent.value) {
    message.warning("暂无可复制内容");
    return;
  }

  try {
    await navigator.clipboard.writeText(currentContent.value);
    message.success("内容已复制");
  } catch (_err) {
    message.warning("复制失败，请手动复制");
  }
}
</script>

<style scoped>
.diff-result-panel {
  overflow: hidden;
}

.diff-result-panel :deep(.bz-card__body) {
  padding: 12px;
}

.summary-head {
  margin-bottom: 10px;
}

.summary-title {
  font-size: 16px;
  font-weight: 800;
  color: var(--text-main);
}

.summary-subtitle {
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 12px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.stat-card {
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  padding: 10px;
}

.stat-card.added {
  background: #f0fdf4;
  border-color: #bbf7d0;
}

.stat-card.removed {
  background: #fef2f2;
  border-color: #fecaca;
}

.stat-card.changed {
  background: #eff6ff;
  border-color: #bfdbfe;
}

.stat-label {
  color: #475569;
  font-size: 12px;
}

.stat-value {
  margin-top: 4px;
  color: #0f172a;
  font-size: 24px;
  font-weight: 800;
  line-height: 1;
}

.result-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

.mode-switch {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.result-code {
  margin: 0;
  border-radius: 10px;
  background: #0f172a;
  color: #e2e8f0;
  padding: 12px;
  max-height: 56vh;
  overflow: auto;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 768px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
