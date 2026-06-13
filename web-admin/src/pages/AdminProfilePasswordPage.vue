<template>
  <div class="admin-page">
    <div class="content">
      <div class="admin-page-stack">
        <bz-card
          class="admin-panel admin-table-card"
          shadow="never"
        >
          <template #header>
            <div class="admin-table-header">
              <div class="admin-table-title">修改密码</div>
            </div>
          </template>

          <div class="password-layout">
            <bz-form
              label-position="top"
              class="password-form"
            >
              <bz-form-item label="当前密码">
                <bz-input
                  v-model="form.oldPassword"
                  type="password"
                  show-password
                  placeholder="请输入当前密码"
                />
              </bz-form-item>
              <bz-form-item label="新密码">
                <bz-input
                  v-model="form.newPassword"
                  type="password"
                  show-password
                  placeholder="请输入新密码"
                />
              </bz-form-item>
              <bz-form-item label="确认新密码">
                <bz-input
                  v-model="form.confirmPassword"
                  type="password"
                  show-password
                  placeholder="请再次输入新密码"
                />
              </bz-form-item>
            </bz-form>
            <div class="password-actions">
              <bz-button
                type="primary"
                :loading="saving"
                @click="submit"
                >确认</bz-button
              >
            </div>
          </div>
        </bz-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";

import { changePassword } from "../api/auth";
import { message } from "../utils/message";

const saving = ref(false);
const form = reactive({ oldPassword: "", newPassword: "", confirmPassword: "" });

async function submit() {
  const oldPassword = form.oldPassword.trim();
  const newPassword = form.newPassword.trim();
  const confirmPassword = form.confirmPassword.trim();
  if (!oldPassword || !newPassword || !confirmPassword) {
    message.warning("请完整填写密码信息");
    return;
  }
  if (newPassword !== confirmPassword) {
    message.warning("两次输入的新密码不一致");
    return;
  }
  saving.value = true;
  try {
    await changePassword("internal", oldPassword, newPassword);
    form.oldPassword = "";
    form.newPassword = "";
    form.confirmPassword = "";
    message.success("已确认");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "密码修改失败");
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped>
.content {
  flex: 1;
  min-height: 0;
  width: 100%;
  overflow-y: auto;
  box-sizing: border-box;
}
.password-layout {
  display: grid;
  gap: 16px;
  max-width: 520px;
}
.password-form {
  display: grid;
  gap: 8px;
}
.password-actions {
  display: flex;
  justify-content: flex-end;
}
</style>
