<template>
  <section class="placeholder-page">
    <div class="placeholder-card admin-panel">
      <span class="placeholder-tag">迁移中</span>
      <h1>{{ title }}</h1>
      <p>
        这个后台页面已进入独立 `web-admin/` 工程的路由与菜单体系，但具体业务 UI 仍待从旧 `web/`
        工程迁移过来。
      </p>
      <dl class="placeholder-meta">
        <div>
          <dt>页面标题</dt>
          <dd>{{ title }}</dd>
        </div>
        <div>
          <dt>原资源路径</dt>
          <dd>{{ originalPath }}</dd>
        </div>
        <div>
          <dt>资源编码</dt>
          <dd>{{ resourceCode }}</dd>
        </div>
      </dl>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRoute } from "vue-router";

const route = useRoute();

const title = computed(() => String(route.meta.title || "后台页面"));
const originalPath = computed(() => String(route.meta.originalPath || route.path));
const resourceCode = computed(() => String(route.meta.resourceCode || "-"));
</script>

<style scoped>
.placeholder-card {
  padding: 24px;
}
.placeholder-tag {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  color: #92400e;
  background: #fef3c7;
  font-size: 12px;
  font-weight: 700;
}
.placeholder-card h1 {
  margin: 16px 0 12px;
  color: #0f172a;
}
.placeholder-card p {
  margin: 0;
  color: #64748b;
  line-height: 1.7;
}
.placeholder-meta {
  margin: 20px 0 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}
.placeholder-meta div {
  padding: 16px;
  border-radius: 16px;
  background: #f8fafc;
}
.placeholder-meta dt {
  color: #64748b;
  font-size: 12px;
  margin-bottom: 6px;
}
.placeholder-meta dd {
  margin: 0;
  color: #0f172a;
  font-weight: 600;
  word-break: break-all;
}
@media (max-width: 960px) {
  .placeholder-meta {
    grid-template-columns: 1fr;
  }
}
</style>
