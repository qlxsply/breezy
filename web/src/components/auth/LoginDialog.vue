<!-- /src/components/auth/LoginDialog.vue -->
<template>
  <bz-dialog
    :model-value="true"
    title="登录"
    width="420px"
    :close-on-click-modal="false"
    @close="$emit('close')"
  >
    <div
      v-if="allowScopeSwitch"
      class="login-mode"
    >
      <bz-button
        :type="mode === 'external' ? 'primary' : 'default'"
        @click="switchMode('external')"
      >
        用户
      </bz-button>
      <bz-button
        :type="mode === 'internal' ? 'primary' : 'default'"
        @click="switchMode('internal')"
      >
        账号
      </bz-button>
    </div>
    <bz-form label-position="top">
      <bz-form-item label="账号 *">
        <bz-input
          ref="usernameInput"
          v-model="username"
          placeholder="请输入账号"
          @keyup.enter="submit"
        />
      </bz-form-item>
      <bz-form-item label="密码 *">
        <bz-input
          v-model="password"
          type="password"
          show-password
          placeholder="请输入密码"
          @keyup.enter="submit"
        />
      </bz-form-item>
    </bz-form>
    <bz-alert
      v-if="error"
      :title="error"
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
        登录
      </bz-button>
    </template>
  </bz-dialog>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from "vue";

import type { AuthScope } from "../../utils/authStorage";

const props = defineProps<{
  error?: string;
  initialMode?: AuthScope;
  allowScopeSwitch?: boolean;
}>();

const emit = defineEmits<{
  (e: "close"): void;
  (e: "submit", payload: { scope: AuthScope; username: string; password: string }): void;
}>();

const username = ref("");
const password = ref("");
const mode = ref<AuthScope>(props.initialMode || "external");
const allowScopeSwitch = computed(() => props.allowScopeSwitch !== false);
const usernameInput = ref<{ focus: () => void } | null>(null);
const error = computed(() => props.error ?? "");

function submit() {
  emit("submit", { scope: mode.value, username: username.value.trim(), password: password.value });
}

function switchMode(next: AuthScope) {
  if (!allowScopeSwitch.value) return;
  mode.value = next;
}

onMounted(() => {
  void nextTick(() => usernameInput.value?.focus());
});
</script>

<style scoped>
.login-mode {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.form-error {
  margin-top: 4px;
}
</style>
