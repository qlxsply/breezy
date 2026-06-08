<template>
  <div class="admin-home">
    <section class="metric-grid">
      <article
        v-for="item in metrics"
        :key="item.label"
        class="metric-card"
      >
        <div>
          <h2>{{ item.label }}</h2>
          <strong>{{ item.value }}</strong>
        </div>
        <span class="metric-icon">{{ item.icon }}</span>
        <footer>
          <span>{{ item.caption }}</span>
          <b>{{ item.total }}</b>
        </footer>
      </article>
    </section>

    <section class="chart-panel">
      <header class="panel-header">
        <div class="segmented">
          <button
            class="active"
            type="button"
          >
            流量趋势
          </button>
          <button type="button">月访问量</button>
        </div>
      </header>
      <div class="chart">
        <div class="chart-grid"></div>
        <svg
          viewBox="0 0 1200 260"
          aria-hidden="true"
        >
          <path
            class="area area-blue"
            d="M0 250 C80 245 120 210 170 175 C230 128 260 65 330 58 C395 50 430 120 480 165 C540 220 610 230 670 190 C735 145 750 78 820 58 C900 36 930 144 1000 175 C1070 205 1130 225 1200 242 L1200 260 L0 260 Z"
          />
          <path
            class="line line-blue"
            d="M0 250 C80 245 120 210 170 175 C230 128 260 65 330 58 C395 50 430 120 480 165 C540 220 610 230 670 190 C735 145 750 78 820 58 C900 36 930 144 1000 175 C1070 205 1130 225 1200 242"
          />
          <path
            class="area area-green"
            d="M0 252 C160 250 250 250 330 228 C390 210 430 142 480 218 C560 250 665 244 720 205 C790 155 865 158 930 210 C1000 248 1120 252 1200 252 L1200 260 L0 260 Z"
          />
          <path
            class="line line-green"
            d="M0 252 C160 250 250 250 330 228 C390 210 430 142 480 218 C560 250 665 244 720 205 C790 155 865 158 930 210 C1000 248 1120 252 1200 252"
          />
        </svg>
      </div>
    </section>

    <section class="panel-grid">
      <article class="admin-panel">
        <header class="panel-header">
          <h2>常用入口</h2>
        </header>
        <div class="quick-grid">
          <button
            v-for="menu in quickMenus"
            :key="menu.id"
            class="quick-card"
            type="button"
            @click="router.push(menu.url)"
          >
            <span>{{ menu.parentName || "系统管理" }}</span>
            <strong>{{ menu.name }}</strong>
          </button>
        </div>
      </article>

      <article class="admin-panel">
        <header class="panel-header">
          <h2>后台信息</h2>
        </header>
        <div class="info-list">
          <div>
            <span>当前角色</span>
            <strong>{{ userTypeLabel }}</strong>
          </div>
          <div>
            <span>可访问菜单</span>
            <strong>{{ quickMenus.length }}</strong>
          </div>
          <div>
            <span>后台路径</span>
            <strong>/admin</strong>
          </div>
        </div>
      </article>
    </section>

    <section
      v-if="quickMenus.length > 6"
      class="admin-panel"
    >
      <header class="panel-header">
        <h2>更多入口</h2>
      </header>
      <div class="quick-grid compact">
        <button
          v-for="menu in quickMenus.slice(6)"
          :key="menu.id"
          class="quick-card"
          type="button"
          @click="router.push(menu.url)"
        >
          <span>{{ menu.parentName || "系统管理" }}</span>
          <strong>{{ menu.name }}</strong>
        </button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRouter } from "vue-router";

import { currentUserType } from "../registry/auth.registry";
import { hasMenuAccess } from "../registry/permissions.registry";
import { getResourceMap, getResources } from "../registry/resources.registry";

interface QuickMenuItem {
  id: string;
  name: string;
  url: string;
  orderNo: number;
  parentName: string;
}

const router = useRouter();

const USER_TYPE_LABELS: Record<string, string> = {
  INTERNAL: "账号",
  SYSTEM: "系统账号",
  EXTERNAL: "用户",
  GUEST: "游客",
};

const metrics = [
  { label: "用户量", value: "2,000", caption: "总用户量", total: "120,000", icon: "▤" },
  { label: "访问量", value: "20,000", caption: "总访问量", total: "500,000", icon: "◔" },
  { label: "下载量", value: "8,000", caption: "总下载量", total: "120,000", icon: "↓" },
  { label: "使用量", value: "5,000", caption: "总使用量", total: "50,000", icon: "◴" },
];

const quickMenus = computed<QuickMenuItem[]>(() => {
  const resourceMap = getResourceMap();
  const rawMenus = getResources()
    .filter((resource) => resource.type === "MENU")
    .filter((resource) => resource.scope === "SETTING")
    .filter((resource) => resource.openMode === "PAGE")
    .filter((resource) => Boolean(resource.url))
    .filter((resource) => hasMenuAccess(resource, resourceMap))
    .map((resource) => ({
      id: resource.id,
      name: resource.name,
      url: resource.url,
      orderNo: resource.orderNo,
      parentName: resource.parentId ? resourceMap.get(resource.parentId)?.name || "" : "",
    }))
    .sort((left, right) => {
      if (left.orderNo !== right.orderNo) return left.orderNo - right.orderNo;
      return left.name.localeCompare(right.name);
    });
  return rawMenus;
});

const userTypeLabel = computed(() => USER_TYPE_LABELS[currentUserType.value] || "账号");
</script>

<style scoped>
.admin-home {
  min-height: 100%;
  padding: 20px;
  box-sizing: border-box;
  background: #14171d;
  color: #f5f7fb;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.metric-card {
  min-height: 136px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 20px;
  border: 1px solid #30343c;
  border-radius: 8px;
  background: #1a1d23;
}

.metric-card h2,
.panel-header h2 {
  margin: 0;
  color: #f5f7fb;
  font-size: 20px;
  line-height: 1.2;
}

.metric-card strong {
  display: block;
  margin-top: 22px;
  font-size: 22px;
  line-height: 1;
}

.metric-card footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #f5f7fb;
  font-size: 13px;
}

.metric-icon {
  align-self: flex-end;
  color: #60a5fa;
  font-size: 28px;
}

.chart-panel,
.admin-panel {
  margin-top: 20px;
  border: 1px solid #30343c;
  border-radius: 8px;
  background: #1a1d23;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 48px;
  padding: 0 18px;
}

.segmented {
  display: inline-flex;
  align-items: center;
  padding: 4px;
  border-radius: 6px;
  background: #23272e;
}

.segmented button {
  height: 28px;
  border: none;
  border-radius: 5px;
  padding: 0 14px;
  background: transparent;
  color: #a3aab7;
  cursor: pointer;
  font-weight: 700;
}

.segmented button.active {
  background: #2d323a;
  color: #fff;
}

.chart {
  position: relative;
  height: 300px;
  margin: 0 18px 18px;
  overflow: hidden;
}

.chart-grid {
  position: absolute;
  inset: 20px 0 0;
  background:
    linear-gradient(#30343c 1px, transparent 1px),
    linear-gradient(90deg, #30343c 1px, transparent 1px);
  background-size: 90px 68px;
  opacity: 0.75;
}

.chart svg {
  position: absolute;
  inset: 18px 0 0;
  width: 100%;
  height: calc(100% - 18px);
}

.area {
  opacity: 0.72;
}

.area-blue {
  fill: #4aa3df;
}

.area-green {
  fill: #13b8a6;
}

.line {
  fill: none;
  stroke-width: 3;
}

.line-blue {
  stroke: #5bb7ff;
}

.line-green {
  stroke: #15c7b5;
}

.panel-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 20px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  padding: 0 18px 18px;
}

.quick-grid.compact {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.quick-card {
  min-height: 88px;
  border: 1px solid #30343c;
  border-radius: 8px;
  background: #20242b;
  color: #f5f7fb;
  display: grid;
  gap: 8px;
  align-content: center;
  padding: 14px;
  text-align: left;
  cursor: pointer;
}

.quick-card:hover {
  border-color: #1677ff;
  background: #252a32;
}

.quick-card span,
.info-list span {
  color: #a3aab7;
  font-size: 13px;
}

.quick-card strong {
  font-size: 16px;
}

.info-list {
  display: grid;
  gap: 0;
  padding: 0 18px 18px;
}

.info-list div {
  min-height: 50px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #30343c;
}

.info-list div:last-child {
  border-bottom: none;
}

@media (max-width: 1200px) {
  .metric-grid,
  .quick-grid,
  .quick-grid.compact {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .panel-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .metric-grid,
  .quick-grid,
  .quick-grid.compact {
    grid-template-columns: 1fr;
  }
}
</style>
