<template>
  <section class="workbench-page">
    <div class="hero-card admin-panel">
      <div>
        <h1>后台工作台</h1>
        <p>
          这是 `web-admin/`
          独立工程的第一版工作台。当前已完成后台登录、基础壳体、菜单资源加载与独立路由边界。
        </p>
      </div>
      <div class="hero-badges">
        <span>独立前端入口</span>
        <span>独立登录态</span>
        <span>独立菜单装载</span>
      </div>
    </div>

    <div class="grid-cards">
      <article class="status-card admin-panel">
        <h2>当前阶段</h2>
        <p>后台工程已从原 `web/` 中拆出独立入口，但后台业务页面仍处于渐进迁移阶段。</p>
      </article>
      <article class="status-card admin-panel">
        <h2>下一步建议</h2>
        <p>
          优先迁移账号、角色、配置、接口管理等高频后台页，并同步迁移后台专属 API 包装与状态管理。
        </p>
      </article>
      <article class="status-card admin-panel">
        <h2>边界原则</h2>
        <p>用户前端不再承载后台壳体与后台菜单心智，后台前端也不再复用首页搜索入口。</p>
      </article>
    </div>
  </section>
</template>

<style scoped>
.workbench-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.hero-card,
.status-card {
  padding: 22px;
}
.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: flex-start;
}
.hero-card h1,
.status-card h2 {
  margin: 0 0 10px;
  color: #0f172a;
}
.hero-card p,
.status-card p {
  margin: 0;
  color: #64748b;
  line-height: 1.7;
}
.hero-badges {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.hero-badges span {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  color: #1d4ed8;
  background: #eff6ff;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}
.grid-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}
@media (max-width: 960px) {
  .hero-card {
    flex-direction: column;
  }
  .grid-cards {
    grid-template-columns: 1fr;
  }
}
</style>
