<template>
  <section class="auth-portal">
    <main class="auth-shell">
      <section class="brand-panel">
        <div class="brand-top">
          <div class="brand-logo">
            <span class="brand-logo__text">Breezy</span>
          </div>

          <h1>欢迎回来</h1>
          <p>登录后继续访问工作台、业务数据和个人服务。</p>
        </div>

        <div class="brand-metrics">
          <div class="metric">
            <strong>安全</strong>
            <span>统一身份认证</span>
          </div>
          <div class="metric">
            <strong>高效</strong>
            <span>快速进入工作台</span>
          </div>
          <div class="metric">
            <strong>稳定</strong>
            <span>可靠服务体验</span>
          </div>
        </div>
      </section>

      <section class="form-panel">
        <div class="form-card">
          <div class="form-head">
            <span class="eyebrow">用户登录</span>
            <h2>登录用户</h2>
          </div>

          <bz-form
            class="form-body"
            label-position="top"
            @submit.prevent="submit"
          >
            <bz-form-item label="用户名">
              <bz-input
                ref="usernameInput"
                v-model="username"
                placeholder="请输入用户名"
                @keyup.enter="submit"
              />
            </bz-form-item>

            <bz-form-item label="密码">
              <div class="password-field">
                <bz-input
                  v-model="password"
                  :type="passwordVisible ? 'text' : 'password'"
                  placeholder="请输入密码"
                  @keyup.enter="submit"
                />

                <button
                  class="password-toggle"
                  type="button"
                  :aria-label="passwordVisible ? '隐藏密码' : '显示密码'"
                  @click="passwordVisible = !passwordVisible"
                >
                  <svg
                    v-if="passwordVisible"
                    viewBox="0 0 24 24"
                    aria-hidden="true"
                  >
                    <path
                      d="M2.25 12s3.75-6.75 9.75-6.75S21.75 12 21.75 12 18 18.75 12 18.75 2.25 12 2.25 12Z"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="1.8"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    />
                    <path
                      d="M12 15.25A3.25 3.25 0 1 0 12 8.75a3.25 3.25 0 0 0 0 6.5Z"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="1.8"
                    />
                  </svg>

                  <svg
                    v-else
                    viewBox="0 0 24 24"
                    aria-hidden="true"
                  >
                    <path
                      d="M3 3l18 18"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="1.8"
                      stroke-linecap="round"
                    />
                    <path
                      d="M10.7 5.42c.42-.08.85-.12 1.3-.12 6 0 9.75 6.7 9.75 6.7a18.36 18.36 0 0 1-3.1 3.84M6.55 6.7A18.16 18.16 0 0 0 2.25 12S6 18.7 12 18.7c1.63 0 3.08-.5 4.32-1.2"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="1.8"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    />
                    <path
                      d="M9.9 9.9a3.25 3.25 0 0 0 4.2 4.2"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="1.8"
                      stroke-linecap="round"
                    />
                  </svg>
                </button>
              </div>
            </bz-form-item>
          </bz-form>

          <div class="form-error-slot">
            <bz-alert
              v-if="error"
              :title="error"
              type="error"
              show-icon
              class="form-error"
            />
          </div>

          <div class="form-actions form-actions--login">
            <bz-button
              type="primary"
              size="large"
              :loading="submitting"
              @click="submit"
            >
              登录
            </bz-button>

            <bz-button
              size="large"
              @click="router.push('/register')"
            >
              注册
            </bz-button>

            <bz-button
              size="large"
              @click="router.push('/')"
            >
              返回首页
            </bz-button>
          </div>
        </div>
      </section>
    </main>
  </section>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";

import { login, resolveLandingPathForUser } from "../registry/auth.registry";
import { refreshUserToolPermissions } from "../registry/user-tool-permissions.registry";
import { refreshUserToolsLoaded } from "../registry/user-tools.registry";
import { initDynamicRoutes } from "../router";

const router = useRouter();
const route = useRoute();

const username = ref("");
const password = ref("");
const error = ref("");
const submitting = ref(false);
const passwordVisible = ref(false);
const usernameInput = ref<{ focus: () => void } | null>(null);

function resolveRedirectPath(): string {
  const redirect = typeof route.query.redirect === "string" ? route.query.redirect.trim() : "";

  if (!redirect) {
    return resolveLandingPathForUser("EXTERNAL");
  }

  return redirect.startsWith("/admin") ? resolveLandingPathForUser("EXTERNAL") : redirect;
}

async function submit() {
  if (!username.value.trim() || !password.value) {
    error.value = "用户名和密码不能为空";
    return;
  }

  try {
    submitting.value = true;
    error.value = "";

    await login(username.value.trim(), password.value);

    await refreshUserToolsLoaded();
    await refreshUserToolPermissions();
    await initDynamicRoutes();
    await router.push({ path: resolveRedirectPath() });
  } catch (err) {
    error.value = err instanceof Error ? err.message : "登录失败";
  } finally {
    submitting.value = false;
  }
}

onMounted(() => {
  void nextTick(() => usernameInput.value?.focus());
});
</script>

<style scoped>
.auth-portal {
  min-height: calc(100vh - var(--status-bar-height));
  padding: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(circle at 8% 12%, rgba(59, 130, 246, 0.18), transparent 38%),
    radial-gradient(circle at 92% 88%, rgba(14, 165, 233, 0.16), transparent 34%), #f4f7fc;
}

.auth-shell {
  width: min(1120px, 100%);
  min-height: 680px;
  border-radius: 26px;
  overflow: hidden;
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  box-shadow: 0 30px 60px rgba(15, 23, 42, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.65);
  background: #fff;
}

.brand-panel {
  padding: 52px;
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  background:
    linear-gradient(145deg, rgba(15, 23, 42, 0.93), rgba(30, 64, 175, 0.86)),
    url("https://images.unsplash.com/photo-1551434678-e076c223a692?auto=format&fit=crop&w=1400&q=80");
  background-size: cover;
  background-position: center;
}

.brand-logo {
  display: inline-flex;
  align-items: center;
}

.brand-logo__text {
  font-size: 20px;
  font-weight: 700;
}

.brand-top h1 {
  margin: 84px 0 16px;
  font-size: clamp(34px, 4.4vw, 54px);
  line-height: 1.08;
  letter-spacing: -0.04em;
}

.brand-top p {
  margin: 0;
  max-width: 440px;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.7;
}

.brand-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.metric {
  padding: 14px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(255, 255, 255, 0.08);
}

.metric strong {
  display: block;
  font-size: 20px;
  margin-bottom: 3px;
}

.metric span {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
}

.form-panel {
  min-height: 100%;
  padding: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(180deg, #ffffff, #fbfdff);
}

.form-card {
  width: 100%;
  max-width: 430px;
  min-height: 430px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
}

.form-head {
  height: 92px;
  flex: 0 0 92px;
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  padding: 7px 12px;
  border-radius: 999px;
  color: #1d4ed8;
  background: #eff6ff;
  font-size: 12px;
  font-weight: 700;
}

.form-head h2 {
  margin: 16px 0 0;
  font-size: 32px;
  line-height: 1.2;
  letter-spacing: -0.03em;
  color: #0f172a;
}

.form-body {
  margin-top: 0;
}

.password-field {
  position: relative;
  width: 100%;
}

.password-field :deep(.bz-input__inner),
.password-field :deep(input) {
  padding-right: 44px;
}

.password-toggle {
  position: absolute;
  right: 10px;
  top: 50%;
  z-index: 2;
  width: 32px;
  height: 32px;
  padding: 0;
  border: 0;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  background: transparent;
  cursor: pointer;
  transform: translateY(-50%);
  transition:
    color 0.18s ease,
    background-color 0.18s ease;
}

.password-toggle:hover {
  color: #2563eb;
  background: #eff6ff;
}

.password-toggle svg {
  width: 20px;
  height: 20px;
}

.form-error-slot {
  min-height: 52px;
  margin-top: 12px;
}

.form-error {
  margin: 0;
}

.form-actions {
  display: grid;
  gap: 12px;
}

.form-actions--login {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.form-actions :deep(.bz-button) {
  width: 100%;
}

@media (max-width: 960px) {
  .auth-portal {
    padding: 16px;
    align-items: flex-start;
  }

  .auth-shell {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .brand-panel {
    min-height: 340px;
    padding: 32px;
  }

  .brand-top h1 {
    margin-top: 52px;
  }

  .form-panel {
    padding: 36px 22px;
  }

  .form-card {
    min-height: 430px;
  }
}

@media (max-width: 640px) {
  .brand-metrics {
    grid-template-columns: 1fr;
  }

  .form-actions--login {
    grid-template-columns: 1fr;
  }
}
</style>
