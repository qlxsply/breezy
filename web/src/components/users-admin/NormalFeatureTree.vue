<template>
  <div class="feature-list">
    <label
      v-for="feature in features"
      :key="feature.id"
      class="feature-row"
      :class="{ disabled: !feature.enabled }"
    >
      <div class="feature-main">
        <bz-checkbox
          :model-value="selectedSet.has(feature.id)"
          :disabled="!feature.enabled"
          @change="() => toggle(feature.id)"
        />
        <div class="feature-copy">
          <div class="feature-title-line">
            <span class="feature-name">{{ feature.name }}</span>
            <span class="feature-code">{{ feature.code }}</span>
          </div>
          <div
            v-if="feature.description"
            class="feature-description"
          >
            {{ feature.description }}
          </div>
        </div>
      </div>
      <div
        v-if="feature.permissionCodes.length"
        class="feature-permissions"
      >
        <bz-tag
          v-for="permissionCode in feature.permissionCodes"
          :key="permissionCode"
          size="small"
          type="info"
        >
          {{ permissionCode }}
        </bz-tag>
      </div>
    </label>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";

import type { NormalFeatureEntry } from "../../types/normal-feature";

const props = defineProps<{
  features: NormalFeatureEntry[];
  modelValue: string[];
}>();

const emit = defineEmits<{
  (e: "update:modelValue", value: string[]): void;
}>();

const selectedSet = computed(() => new Set(props.modelValue));

function toggle(featureId: string) {
  const next = new Set(selectedSet.value);
  if (next.has(featureId)) {
    next.delete(featureId);
  } else {
    next.add(featureId);
  }
  emit("update:modelValue", Array.from(next));
}
</script>

<style scoped>
.feature-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.feature-row {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px 16px;
  border: 1px solid var(--border-color);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.86);
  cursor: pointer;
}

.feature-row.disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.feature-main {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.feature-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.feature-title-line {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.feature-name {
  font-size: 14px;
  font-weight: 800;
  color: var(--text-main);
}

.feature-code {
  font-size: 11px;
  color: var(--text-muted);
  font-family:
    ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New",
    monospace;
}

.feature-description {
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.6;
}

.feature-permissions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-left: 30px;
}
</style>
