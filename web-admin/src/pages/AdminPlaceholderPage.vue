<template>
  <div class="admin-page">
    <div class="content">
      <div class="admin-page-stack">
        <bz-card
          class="admin-panel placeholder-panel"
          shadow="never"
        >
          <div class="placeholder-body">
            <div class="placeholder-mark">{{ title.slice(0, 2) || "页面" }}</div>
            <div class="placeholder-title">{{ title }}</div>
            <div class="placeholder-desc">{{ description }}</div>
          </div>
        </bz-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRoute } from "vue-router";

const route = useRoute();

const title = computed(() => {
  const header = (route.meta.header as { title?: string } | undefined) ?? {};
  return header.title?.trim() || "空白页面";
});

const description = computed(() => {
  const metaDescription =
    typeof route.meta.description === "string" ? route.meta.description.trim() : "";
  return metaDescription || "当前页面已预留，后续可以在这里补充对应功能。";
});
</script>

<style scoped>
.content {
  min-height: 0;
  width: 100%;
}

.placeholder-panel {
  min-height: 320px;
}

.placeholder-body {
  display: grid;
  place-items: center;
  gap: 14px;
  min-height: 280px;
  padding: 24px;
  text-align: center;
}

.placeholder-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  color: #2563eb;
  background: linear-gradient(135deg, #dbeafe 0%, #eff6ff 100%);
  border: 1px solid #bfdbfe;
  border-radius: 24px;
  font-size: 22px;
  font-weight: 700;
}

.placeholder-title {
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}

.placeholder-desc {
  max-width: 560px;
  color: #64748b;
  line-height: 1.7;
}
</style>
