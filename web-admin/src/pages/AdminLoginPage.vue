<template>
  <section class="login-page">
    <div class="login-card">
      <div class="login-card__head">
        <span class="login-card__eyebrow">Breezy Admin</span>
        <h1>后台登录</h1>
      </div>

      <BzForm
        class="login-form"
        label-position="top"
        @submit.prevent="submit"
      >
        <BzFormItem label="账号">
          <BzInput
            ref="accountInput"
            v-model="account"
            placeholder="请输入后台账号"
            @keyup.enter="submit"
          />
        </BzFormItem>
        <BzFormItem label="密码">
          <BzInput
            v-model="password"
            type="password"
            placeholder="请输入密码"
            @keyup.enter="submit"
          />
        </BzFormItem>
      </BzForm>

      <BzAlert
        v-if="error"
        :title="error"
        type="error"
        show-icon
        class="login-error"
      />

      <div class="login-actions">
        <BzButton
          type="primary"
          size="large"
          :loading="submitting"
          @click="submit"
        >
          登录后台
        </BzButton>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ensureAdminMenuResourcesLoaded } from "@admin/registry/admin-menu-resources";
import { login } from "@admin/registry/auth";
import { ensureAdminDynamicRoutes } from "@admin/router";
import { BzAlert, BzButton, BzForm, BzFormItem, BzInput } from "@shared/components/bz";
import { nextTick, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";

const router = useRouter();
const route = useRoute();

const account = ref("");
const password = ref("");
const error = ref("");
const submitting = ref(false);
const accountInput = ref<{ focus: () => void } | null>(null);

async function submit() {
  if (!account.value.trim() || !password.value.trim()) {
    error.value = "账号和密码不能为空";
    return;
  }
  try {
    submitting.value = true;
    error.value = "";
    await login(account.value.trim(), password.value.trim());
    await ensureAdminMenuResourcesLoaded(true);
    await ensureAdminDynamicRoutes();
    const redirect = typeof route.query.redirect === "string" ? route.query.redirect : "/";
    await router.push(redirect || "/");
  } catch (err) {
    error.value = err instanceof Error ? err.message : "登录失败";
  } finally {
    submitting.value = false;
  }
}

onMounted(() => {
  void nextTick(() => accountInput.value?.focus());
});
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background:
    radial-gradient(circle at top left, rgba(59, 130, 246, 0.18), transparent 34%),
    radial-gradient(circle at bottom right, rgba(37, 99, 235, 0.14), transparent 28%), #f4f7fb;
}
.login-card {
  width: min(460px, 100%);
  padding: 30px;
  border-radius: 24px;
  background: #fff;
  border: 1px solid #e5e7eb;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.12);
}
.login-card__eyebrow {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  color: #1d4ed8;
  background: #eff6ff;
  font-size: 12px;
  font-weight: 800;
}
.login-card__head h1 {
  margin: 16px 0 8px;
  font-size: 30px;
  letter-spacing: -0.03em;
}
.login-card__head p {
  margin: 0;
  color: #64748b;
  line-height: 1.7;
}
.login-form {
  margin-top: 20px;
}
.login-error {
  margin-top: 12px;
}
.login-actions {
  margin-top: 20px;
}
.login-actions :deep(.bz-button) {
  width: 100%;
}
</style>
