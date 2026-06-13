<template>
  <div class="drawer-backdrop" @mousedown.self="$emit('close')">
    <aside
      ref="drawerRef"
      class="auth-drawer"
      role="dialog"
      aria-modal="true"
      :aria-label="mode === 'login' ? '用户登录' : '用户注册'"
      tabindex="-1"
    >
      <span class="mobile-sheet-handle" aria-hidden="true"></span>

      <div class="drawer-top">
        <div class="drawer-context">
          <span class="context-dot" aria-hidden="true"></span>
          <span>{{ mode === "login" ? "用户登录" : "用户注册" }}</span>
        </div>

        <button class="icon-button" type="button" aria-label="关闭认证抽屉" @click="$emit('close')">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M6 6l12 12M18 6 6 18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
          </svg>
        </button>
      </div>

      <div class="drawer-scroll">
        <div class="auth-tabs" role="tablist" aria-label="认证方式">
          <button
            class="auth-tab"
            :class="{ 'is-active': mode === 'login' }"
            type="button"
            role="tab"
            :aria-selected="mode === 'login'"
            @click="switchMode('login')"
          >
            登录
          </button>
          <button
            class="auth-tab"
            :class="{ 'is-active': mode === 'register' }"
            type="button"
            role="tab"
            :aria-selected="mode === 'register'"
            @click="switchMode('register')"
          >
            注册
          </button>
        </div>

        <form class="form" @submit.prevent="submit">
          <div class="field">
            <label class="label" for="auth-username">用户名</label>
            <div class="control">
              <input
                id="auth-username"
                ref="usernameInputRef"
                v-model="username"
                class="input"
                autocomplete="username"
                placeholder="请输入用户名"
              />
            </div>
          </div>

          <div class="field">
            <label class="label" for="auth-password">密码</label>
            <div class="control">
              <input
                id="auth-password"
                v-model="password"
                class="input has-action"
                :type="passwordVisible ? 'text' : 'password'"
                :autocomplete="mode === 'login' ? 'current-password' : 'new-password'"
                placeholder="请输入密码"
              />
              <button
                type="button"
                class="password-toggle"
                :aria-label="passwordVisible ? '隐藏密码' : '显示密码'"
                :aria-pressed="passwordVisible"
                @click="passwordVisible = !passwordVisible"
              >
                <svg v-if="!passwordVisible" viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M2.75 12s3.5-6.25 9.25-6.25S21.25 12 21.25 12 17.75 18.25 12 18.25 2.75 12 2.75 12Z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
                  <path d="M12 15.25A3.25 3.25 0 1 0 12 8.75a3.25 3.25 0 0 0 0 6.5Z" fill="none" stroke="currentColor" stroke-width="1.8" />
                </svg>
                <svg v-else viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M3.25 3.25 20.75 20.75" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                  <path d="M10.35 5.88A8.52 8.52 0 0 1 12 5.75c5.75 0 9.25 6.25 9.25 6.25a17.16 17.16 0 0 1-3.08 3.72M6.57 6.92A17.3 17.3 0 0 0 2.75 12s3.5 6.25 9.25 6.25c1.45 0 2.75-.4 3.9-1.01" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
                  <path d="M9.88 9.88a3.25 3.25 0 0 0 4.24 4.24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                </svg>
              </button>
            </div>
          </div>

          <div v-if="mode === 'register'" class="field">
            <label class="label" for="auth-confirm-password">确认密码</label>
            <div class="control">
              <input
                id="auth-confirm-password"
                v-model="confirmPassword"
                class="input has-action"
                :type="confirmPasswordVisible ? 'text' : 'password'"
                autocomplete="new-password"
                placeholder="请再次输入密码"
              />
              <button
                type="button"
                class="password-toggle"
                :aria-label="confirmPasswordVisible ? '隐藏确认密码' : '显示确认密码'"
                :aria-pressed="confirmPasswordVisible"
                @click="confirmPasswordVisible = !confirmPasswordVisible"
              >
                <svg v-if="!confirmPasswordVisible" viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M2.75 12s3.5-6.25 9.25-6.25S21.25 12 21.25 12 17.75 18.25 12 18.25 2.75 12 2.75 12Z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
                  <path d="M12 15.25A3.25 3.25 0 1 0 12 8.75a3.25 3.25 0 0 0 0 6.5Z" fill="none" stroke="currentColor" stroke-width="1.8" />
                </svg>
                <svg v-else viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M3.25 3.25 20.75 20.75" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                  <path d="M10.35 5.88A8.52 8.52 0 0 1 12 5.75c5.75 0 9.25 6.25 9.25 6.25a17.16 17.16 0 0 1-3.08 3.72M6.57 6.92A17.3 17.3 0 0 0 2.75 12s3.5 6.25 9.25 6.25c1.45 0 2.75-.4 3.9-1.01" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
                  <path d="M9.88 9.88a3.25 3.25 0 0 0 4.24 4.24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                </svg>
              </button>
            </div>
          </div>

          <div v-if="message" class="form-message is-visible" :class="resolvedMessageType === 'success' ? 'is-success' : 'is-error'">
            {{ message }}
          </div>

          <button class="submit-button" type="submit" :disabled="submitting">
            {{ submitting ? (mode === "login" ? "登录中..." : "注册中...") : mode === "login" ? "登录" : "注册" }}
          </button>
        </form>

        <div class="auth-footnote">
          {{ mode === "login" ? "没有账户？切换到注册即可创建用户。" : "已有用户？切换到登录即可继续使用。" }}
        </div>
      </div>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from "vue";

type AuthMode = "login" | "register";
type MessageType = "error" | "success";

const props = defineProps<{
  mode: AuthMode;
  message?: string;
  messageType?: MessageType;
  submitting?: boolean;
}>();

const emit = defineEmits<{
  (e: "close"): void;
  (e: "switch-mode", mode: AuthMode): void;
  (e: "submit-login", payload: { username: string; password: string }): void;
  (e: "submit-register", payload: { username: string; password: string; confirmPassword: string }): void;
}>();

const drawerRef = ref<HTMLElement | null>(null);
const usernameInputRef = ref<HTMLInputElement | null>(null);
const username = ref("");
const password = ref("");
const confirmPassword = ref("");
const passwordVisible = ref(false);
const confirmPasswordVisible = ref(false);

const resolvedMessageType = computed(() => props.messageType ?? "error");

function resetForm() {
  username.value = "";
  password.value = "";
  confirmPassword.value = "";
  passwordVisible.value = false;
  confirmPasswordVisible.value = false;
}

function focusUsername() {
  void nextTick(() => {
    drawerRef.value?.focus();
    usernameInputRef.value?.focus();
  });
}

function switchMode(mode: AuthMode) {
  emit("switch-mode", mode);
}

function submit() {
  if (props.mode === "login") {
    emit("submit-login", {
      username: username.value.trim(),
      password: password.value,
    });
    return;
  }

  emit("submit-register", {
    username: username.value.trim(),
    password: password.value,
    confirmPassword: confirmPassword.value,
  });
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === "Escape") emit("close");
}

watch(
  () => props.mode,
  () => {
    resetForm();
    focusUsername();
  },
  { immediate: true },
);

onMounted(() => {
  document.body.style.overflow = "hidden";
  window.addEventListener("keydown", handleKeydown);
  focusUsername();
});

onUnmounted(() => {
  document.body.style.overflow = "";
  window.removeEventListener("keydown", handleKeydown);
});
</script>

<style scoped>
.drawer-backdrop {
  position: fixed;
  inset: 0;
  z-index: 220;
  display: flex;
  justify-content: flex-end;
  background: rgba(15, 23, 42, 0.36);
}

.auth-drawer {
  width: min(480px, 100vw);
  height: 100%;
  background: #fff;
  box-shadow: 0 28px 70px rgba(15, 23, 42, 0.18);
  display: flex;
  flex-direction: column;
  outline: none;
}

.drawer-top {
  padding: 18px 20px 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.drawer-context {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.context-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 4px #dcfce7;
}

.icon-button {
  width: 38px;
  height: 38px;
  border: 1px solid #e2e8f0;
  border-radius: 13px;
  background: #fff;
  color: #475569;
  display: grid;
  place-items: center;
}

.icon-button svg {
  width: 18px;
  height: 18px;
}

.drawer-scroll {
  overflow: auto;
  padding: 24px 34px 34px;
}

.auth-tabs {
  margin-bottom: 24px;
  padding: 5px;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  background: #f8fafc;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 5px;
}

.auth-tab {
  min-height: 42px;
  border: none;
  border-radius: 12px;
  color: #64748b;
  background: transparent;
  font-weight: 800;
}

.auth-tab.is-active {
  color: #0f172a;
  background: #fff;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
}

.form {
  display: grid;
  gap: 16px;
}

.field {
  display: grid;
  gap: 8px;
}

.label {
  color: #334155;
  font-size: 13px;
  font-weight: 800;
}

.control {
  position: relative;
}

.input {
  width: 100%;
  height: 48px;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 0 14px;
  background: #fff;
  color: #0f172a;
  outline: none;
}

.input::placeholder {
  color: #94a3b8;
}

.input:focus {
  border-color: #93c5fd;
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.12);
}

.input.has-action {
  padding-right: 48px;
}

.password-toggle {
  position: absolute;
  right: 8px;
  top: 50%;
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 10px;
  display: grid;
  place-items: center;
  color: #64748b;
  background: transparent;
  transform: translateY(-50%);
}

.password-toggle:hover {
  color: #2563eb;
  background: #eff6ff;
}

.password-toggle svg {
  width: 20px;
  height: 20px;
}

.form-message {
  display: none;
  border-radius: 14px;
  padding: 11px 12px;
  font-size: 13px;
  line-height: 1.5;
}

.form-message.is-visible {
  display: block;
}

.form-message.is-error {
  color: #991b1b;
  border: 1px solid #fecaca;
  background: #fef2f2;
}

.form-message.is-success {
  color: #166534;
  border: 1px solid #bbf7d0;
  background: #f0fdf4;
}

.submit-button {
  height: 50px;
  border: none;
  border-radius: 15px;
  color: #fff;
  background: linear-gradient(135deg, #2563eb, #0ea5e9);
  font-weight: 900;
  box-shadow: 0 16px 30px rgba(37, 99, 235, 0.22);
}

.submit-button:disabled {
  cursor: not-allowed;
  opacity: 0.68;
}

.auth-footnote {
  margin-top: 18px;
  padding: 14px;
  border: 1px solid #e0f2fe;
  border-radius: 16px;
  background: #f8fbff;
  color: #64748b;
  line-height: 1.65;
  font-size: 13px;
}

.mobile-sheet-handle {
  display: none;
}

@media (max-width: 540px) {
  .drawer-backdrop {
    align-items: flex-end;
  }

  .auth-drawer {
    width: 100vw;
    height: min(92vh, 760px);
    border-radius: 24px 24px 0 0;
  }

  .mobile-sheet-handle {
    display: block;
    width: 42px;
    height: 5px;
    border-radius: 999px;
    background: #cbd5e1;
    margin: 10px auto 0;
  }

  .drawer-scroll {
    padding: 20px 22px 26px;
  }
}
</style>
