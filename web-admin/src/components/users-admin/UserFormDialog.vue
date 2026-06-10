<!-- /src/components/users-admin/UserFormDialog.vue -->
<template>
  <bz-dialog
    :model-value="true"
    :title="mode === 'create' ? '新增用户' : '编辑用户'"
    width="520px"
    @close="$emit('close')"
  >
    <bz-form label-position="top">
      <bz-form-item label="用户名 *">
        <bz-input
          v-model="m.username"
          :disabled="mode === 'edit'"
          placeholder="请输入用户名"
        />
      </bz-form-item>
      <bz-form-item label="昵称 *">
        <bz-input
          v-model="m.nickname"
          placeholder="请输入昵称"
        />
      </bz-form-item>
      <bz-form-item
        v-if="mode === 'create'"
        label="密码 *"
      >
        <bz-input
          v-model="m.password"
          type="password"
          placeholder="请输入初始密码"
          show-password
        />
      </bz-form-item>
      <bz-form-item
        v-if="mode === 'edit'"
        label="状态 *"
      >
        <bz-select v-model="m.status">
          <bz-option
            label="启用"
            value="ENABLED"
          />
          <bz-option
            label="停用"
            value="DISABLED"
          />
        </bz-select>
      </bz-form-item>
    </bz-form>
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
      >
        {{ mode === "create" ? "创建" : "保存" }}
      </bz-button>
    </template>
  </bz-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from "vue";

import type { UserEntry, UserStatus } from "../../types/user-admin";

const props = defineProps<{
  mode: "create" | "edit";
  model: UserEntry | null;
}>();

const emit = defineEmits<{
  (e: "close"): void;
  (
    e: "submit",
    payload: { username: string; nickname: string; password?: string; status: UserStatus },
  ): void;
}>();

const m = reactive({
  username: "",
  nickname: "",
  password: "",
  status: "ENABLED" as UserStatus,
});

const err = ref("");

watch(
  () => props.model,
  (value) => {
    if (!value) return;
    m.username = value.username;
    m.nickname = value.nickname || "";
    m.password = "";
    m.status = value.status;
  },
  { immediate: true },
);

function validate(): string {
  if (!m.username.trim()) return "用户名不能为空";
  if (!m.nickname.trim()) return "昵称不能为空";
  if (props.mode === "create" && !m.password.trim()) return "密码不能为空";
  return "";
}

function submit() {
  err.value = "";
  const validationError = validate();
  if (validationError) {
    err.value = validationError;
    return;
  }
  const payload: { username: string; nickname: string; password?: string; status: UserStatus } = {
    username: m.username.trim(),
    nickname: m.nickname.trim(),
    status: m.status,
  };
  if (props.mode === "create") {
    payload.password = m.password.trim();
  }
  emit("submit", payload);
}
</script>

<style scoped>
.form-error {
  margin-top: 4px;
}
</style>
