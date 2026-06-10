<!-- /src/pages/NotFoundPage.vue -->
<template>
  <div class="app-shell">
    <div class="content">
      <div class="card">
        <h2 class="title">404</h2>
        <p class="desc">未找到对应页面或工具。</p>
        <!-- v-if 根据 showHomeLink 决定是否显示操作按钮。 -->
        <div
          v-if="showHomeLink"
          class="actions"
        >
          <!-- @click 是 v-on:click 缩写，绑定点击事件到 goHome 方法。 -->
          <button
            class="btn primary"
            @click="goHome"
          >
            返回首页
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// <script setup> 顶层即 setup()，用 TS 写组合式逻辑。
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";

// useRoute/useRouter 来自 vue-router：读取当前路由/执行跳转。
const route = useRoute();
const router = useRouter();

// computed 返回只读的 ref，会随依赖变化自动更新。
// route.state 来自 history state；官方类型未暴露该字段，因此这里做类型断言。
const showHomeLink = computed(() =>
  Boolean((route as { state?: { fromHome?: boolean } }).state?.fromHome),
);

function goHome() {
  router.replace({ name: "home" });
}
</script>

<style scoped>
.app-shell {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.content {
  flex: 1;
  padding: 40px 20px;
  max-width: 900px;
  margin: 0 auto;
  width: 100%;
  overflow-y: auto;
}

.card {
  background: #fff;
  padding: 40px;
  border-radius: 20px;
  border: 1px solid var(--border-color);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.title {
  margin: 0 0 12px;
}

.desc {
  color: var(--text-muted);
  line-height: 1.6;
}

.actions {
  margin-top: 20px;
  display: flex;
  gap: 10px;
}

.btn {
  border: 1px solid var(--border-color);
  background: #fff;
  padding: 8px 12px;
  border-radius: 10px;
  cursor: pointer;
}

.btn.primary {
  border-color: #bfdbfe;
  background: #eff6ff;
  color: var(--primary-color);
  font-weight: 800;
}
</style>
