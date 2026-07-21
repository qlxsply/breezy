<!-- /src/components/MyInfoBox.vue -->
<template>
  <div class="my-info">
    <div
      v-if="loading"
      class="loading"
    >
      正在加载个人权限画像...
    </div>
    <div
      v-else-if="error"
      class="error"
    >
      {{ error }}
    </div>
    <div
      v-else
      class="info-content"
    >
      <div class="section user-section">
        <div class="user-card">
          <div class="avatar">👤</div>
          <div class="user-meta">
            <div class="username">{{ details?.username }}</div>
            <div class="role-tags">
              <span
                v-for="role in details?.roles"
                :key="role"
                class="role-tag"
              >
                {{ role }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <div class="section-grid">
        <div class="section">
          <div class="section-title">权限编码 ({{ permissionCount }})</div>
          <div class="codes-container">
            <div
              v-for="code in details?.permissionCodes || []"
              :key="code"
              class="code-item"
            >
              {{ code }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";

import { getMyPermissionsDetails, type MyPermissionsDetailRes } from "../api/permissions";

const details = ref<MyPermissionsDetailRes | null>(null);
const loading = ref(true);
const error = ref("");

const permissionCount = computed(() => details.value?.permissionCodes?.length || 0);

onMounted(async () => {
  try {
    details.value = await getMyPermissionsDetails();
  } catch (errorObj: unknown) {
    if (errorObj instanceof Error && errorObj.message) {
      error.value = errorObj.message;
    } else {
      error.value = "加载失败";
    }
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.my-info {
  min-height: 200px;
}

.loading,
.error {
  padding: 40px;
  text-align: center;
  color: var(--text-muted);
}

.error {
  color: #ef4444;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 20px;
  background: #f8fafc;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 24px;
}

.avatar {
  font-size: 40px;
  background: #fff;
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.username {
  font-size: 20px;
  font-weight: 800;
  margin-bottom: 8px;
}

.role-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.role-tag {
  background: #ede9fe;
  color: #7c3aed;
  padding: 2px 10px;
  border-radius: 99px;
  font-size: 12px;
  font-weight: 600;
}

.section-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
}

.section-title {
  font-weight: 800;
  font-size: 14px;
  margin-bottom: 12px;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.codes-container {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px;
  max-height: 400px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.code-item {
  font-family: var(--font-family-mono);
  font-size: 12px;
  padding: 4px 8px;
  background: #f1f5f9;
  border-radius: 4px;
  word-break: break-all;
}
</style>
