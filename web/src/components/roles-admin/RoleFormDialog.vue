<!-- /src/components/roles-admin/RoleFormDialog.vue -->
<template>
  <bz-dialog
    :model-value="true"
    :title="mode === 'create' ? '新增角色' : '编辑角色'"
    width="560px"
    class="role-form-dialog"
    @close="$emit('close')"
  >
    <div class="role-form-shell">
      <bz-form label-position="top">
        <div class="role-form-grid">
          <bz-form-item label="编码 *">
            <bz-input
              v-model="m.code"
              placeholder="例如：admin"
              class="mono"
            />
          </bz-form-item>
          <bz-form-item label="名称 *">
            <bz-input
              v-model="m.name"
              placeholder="例如：管理员"
            />
          </bz-form-item>
        </div>

        <bz-form-item label="启用状态">
          <bz-switch
            v-model="m.enabled"
            active-text="启用"
            inactive-text="停用"
          />
        </bz-form-item>
      </bz-form>
    </div>

    <bz-alert
      v-if="err"
      :title="err"
      type="error"
      show-icon
      class="form-error"
    />
    <template #footer>
      <bz-button @click="$emit('close')">取消</bz-button>
      <bz-button
        type="primary"
        @click="submit"
        >{{ mode === "create" ? "创建" : "保存" }}</bz-button
      >
    </template>
  </bz-dialog>
</template>

<script setup lang="ts">
// <script setup> + TS：顶层即 setup()。
import { reactive, ref, watch } from "vue";

import type { RoleEntry } from "../../types/role-admin";

const props = defineProps<{
  mode: "create" | "edit";
  model: RoleEntry | null;
}>();

const emit = defineEmits<{
  (e: "close"): void;
  (e: "submit", model: RoleEntry): void;
}>();

const m = reactive<RoleEntry>({
  id: "",
  code: "",
  name: "",
  enabled: true,
});

const err = ref("");

watch(
  () => props.model,
  (v) => {
    if (!v) return;
    Object.assign(m, v);
  },
  { immediate: true },
);

function validate(): string {
  if (!m.code.trim()) return "编码不能为空";
  if (!m.name.trim()) return "名称不能为空";
  if (!/^[a-zA-Z0-9_-]+$/.test(m.code.trim())) return "编码建议仅包含字母/数字/_/-";
  return "";
}

function submit() {
  err.value = "";
  const e = validate();
  if (e) {
    err.value = e;
    return;
  }
  emit("submit", { ...m });
}
</script>

<style scoped>
.mono {
  font-family:
    ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New",
    monospace;
}

.mono :deep(.el-input__inner) {
  font-family:
    ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New",
    monospace;
}

.role-form-shell {
  display: grid;
  gap: 16px;
}

.role-form-hint {
  padding: 10px 12px;
  border: 1px solid #dbeafe;
  border-radius: 12px;
  background: #eff6ff;
  color: #475569;
  font-size: 12px;
  line-height: 1.6;
}

.role-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 14px;
}

.form-error {
  margin-top: 4px;
}

@media (max-width: 640px) {
  .role-form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
