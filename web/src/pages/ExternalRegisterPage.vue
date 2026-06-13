<template>
  <div class="auth-redirecting">正在打开注册面板...</div>
</template>

<script setup lang="ts">
import { onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";

import { openRegisterDialog } from "../registry/auth-dialog.registry";

const router = useRouter();
const route = useRoute();

onMounted(() => {
  const redirect = typeof route.query.redirect === "string" ? route.query.redirect.trim() : "";
  openRegisterDialog(redirect);
  void router.replace("/");
});
</script>

<style scoped>
.auth-redirecting {
  min-height: 100vh;
  display: grid;
  place-items: center;
  color: #64748b;
}
</style>
