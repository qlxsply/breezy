<template>
  <bz-dialog
    :model-value="true"
    title="重置密码"
    width="420px"
    @close="$emit('close')"
  >
    <bz-alert
      title="确认将该用户密码重置为 123456"
      type="warning"
      show-icon
      :closable="false"
    />
    <div class="hint">重置后用户下次登录需修改密码。</div>
    <template #footer>
      <bz-button @click="$emit('close')">取消</bz-button>
      <bz-button
        type="primary"
        @click="submit"
        >确认</bz-button
      >
    </template>
  </bz-dialog>
</template>

<script setup lang="ts">
const emit = defineEmits<{
  (e: "close"): void;
  (e: "submit"): void;
}>();

function submit() {
  emit("submit");
}
</script>

<style scoped>
.hint {
  margin-top: 12px;
  font-size: 12px;
  color: var(--text-muted);
  font-weight: 600;
}
</style>
