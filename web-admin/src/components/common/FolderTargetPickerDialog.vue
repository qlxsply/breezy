<template>
  <bz-dialog
    :model-value="visible"
    width="680px"
    @close="$emit('close')"
  >
    <template #header>
      <div class="dialog-title">
        <span>{{ title }}</span>
        <span
          v-if="subTitle"
          class="dialog-sub"
          >{{ subTitle }}</span
        >
      </div>
    </template>

    <bz-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      show-icon
      class="alert"
    />
    <div
      v-else
      v-loading="loading"
      class="target-list"
    >
      <bz-empty
        v-if="!loading && targets.length === 0"
        description="暂无目录"
      />
      <bz-radio-group
        v-else
        :model-value="modelValue"
        @update:model-value="$emit('update:modelValue', $event)"
      >
        <bz-radio
          v-for="target in targets"
          :key="target.value"
          :label="target.value"
          :disabled="target.disabled"
          class="target-item"
        >
          <span
            class="target-label"
            :style="{ paddingLeft: `${target.depth * 18}px` }"
            >{{ target.label }}</span
          >
        </bz-radio>
      </bz-radio-group>
    </div>

    <template #footer>
      <bz-button
        type="primary"
        :disabled="submitting || loading || !!errorMessage"
        @click="$emit('confirm')"
      >
        {{ submitting ? "处理中..." : confirmText }}
      </bz-button>
      <bz-button
        :disabled="submitting"
        @click="$emit('close')"
        >取消</bz-button
      >
    </template>
  </bz-dialog>
</template>

<script setup lang="ts">
export interface FolderTargetOption {
  value: string;
  label: string;
  depth: number;
  disabled: boolean;
}

withDefaults(
  defineProps<{
    visible: boolean;
    title: string;
    subTitle?: string;
    loading: boolean;
    submitting: boolean;
    errorMessage?: string;
    targets: FolderTargetOption[];
    modelValue: string;
    confirmText?: string;
  }>(),
  {
    subTitle: "",
    errorMessage: "",
    confirmText: "确认",
  },
);

defineEmits<{
  close: [];
  confirm: [];
  "update:modelValue": [value: string];
}>();
</script>

<style scoped>
.dialog-title {
  font-size: 14px;
  font-weight: 700;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.dialog-sub {
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 500;
}

.alert {
  margin-bottom: 12px;
}

.target-list {
  padding: 4px 4px 0;
  overflow-y: auto;
  max-height: 52vh;
}

.target-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  width: 100%;
}

.target-label {
  font-size: 13px;
}
</style>
