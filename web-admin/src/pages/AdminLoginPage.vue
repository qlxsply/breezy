<template>
  <AuthLoginPageCard
    scope="internal"
    form-title="后台登录"
    return-label="返回首页"
    return-to="/admin"
  />
</template>

<script setup lang="ts">
import AuthLoginPageCard from "../components/auth/AuthLoginPageCard.vue";
</script>
