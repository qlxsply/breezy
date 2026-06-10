<template>
  <section class="auth-page">
    <div class="auth-card">
      <h1 class="auth-card__title">{{ formTitle }}</h1>

      <bz-form
        class="auth-card__form"
        label-position="top"
      >
        <bz-form-item label="账号">
          <bz-input
            ref="usernameInput"
            v-model="username"
            placeholder="请输入账号"
            @keyup.enter="submit"
          />
        </bz-form-item>

        <bz-form-item label="密码">
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
        class="auth-card__error"
      />

      <div
        class="auth-card__actions"
        :class="{ 'has-extra-action': hasExtraAction }"
      >
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
          @click="router.push(returnTo)"
        >
          {{ returnLabel }}
        </bz-button>

        <bz-button
          v-if="extraActionLabel && extraActionTo"
          size="large"
          @click="router.push(extraActionTo)"
        >
          {{ extraActionLabel }}
        </bz-button>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";

import type { AuthUserType } from "../../api/auth";
import { login, resolveLandingPathForUser } from "../../registry/auth.registry";
import { refreshRegistryLoaded } from "../../registry/bootstrap";
import { refreshPermissions } from "../../registry/permissions.registry";
import { initDynamicRoutes } from "../../router";
import type { AuthScope } from "../../utils/authStorage";

const props = defineProps<{
  scope: AuthScope;
  formTitle: string;
  returnLabel: string;
  returnTo: string;
  extraActionLabel?: string;
  extraActionTo?: string;
}>();

const router = useRouter();
const route = useRoute();

const username = ref("");
const password = ref("");
const error = ref("");
const submitting = ref(false);
const usernameInput = ref<{ focus: () => void } | null>(null);
const hasExtraAction = Boolean(props.extraActionLabel && props.extraActionTo);

function resolveRedirectPath(userType: AuthUserType): string {
  const redirect = typeof route.query.redirect === "string" ? route.query.redirect.trim() : "";

  if (!redirect) {
    return resolveLandingPathForUser(userType);
  }

  if (userType === "INTERNAL") {
    return redirect.startsWith("/admin") ? redirect : resolveLandingPathForUser(userType);
  }

  return redirect.startsWith("/admin") ? resolveLandingPathForUser(userType) : redirect;
}

async function submit() {
  if (!username.value.trim() || !password.value) {
    error.value = "账号和密码不能为空";
    return;
  }

  try {
    submitting.value = true;
    error.value = "";

    const current = await login(props.scope, username.value.trim(), password.value);

    await refreshRegistryLoaded();
    await refreshPermissions();
    await initDynamicRoutes();
    await router.push({ path: resolveRedirectPath(current.userType) });
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
.auth-page {
  min-height: calc(100vh - var(--status-bar-height));
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  box-sizing: border-box;
  background: #f5f7fb;
}

.auth-card {
  width: 100%;
  max-width: 420px;
  padding: 32px;
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.08);
  box-sizing: border-box;
}

.auth-card__title {
  margin: 0 0 28px;
  color: #111827;
  font-size: 24px;
  font-weight: 600;
  line-height: 1.3;
  text-align: center;
}

.auth-card__form {
  margin: 0;
}

.auth-card__error {
  margin-top: 12px;
}

.auth-card__actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-top: 24px;
}

.auth-card__actions.has-extra-action {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

@media (max-width: 640px) {
  .auth-page {
    padding: 16px;
  }

  .auth-card {
    padding: 24px 18px;
  }

  .auth-card__actions {
    grid-template-columns: 1fr;
  }
}
</style>
