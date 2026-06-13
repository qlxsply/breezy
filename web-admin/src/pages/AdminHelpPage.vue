<template>
  <div class="admin-page">
    <div class="content">
      <div class="admin-page-stack">
        <bz-card
          class="admin-panel admin-table-card"
          shadow="never"
        >
          <template #header>
            <div class="admin-table-header">
              <div class="admin-table-title">问题与帮助</div>
            </div>
          </template>

          <div class="help-grid">
            <section class="help-card">
              <div class="help-card__title">账号使用</div>
              <p>可在个人中心查看基础信息与最近登录/退出记录。</p>
              <p>密码修改请前往“修改密码”，个性化设置请前往“偏好设置”。</p>
            </section>
            <section class="help-card">
              <div class="help-card__title">界面说明</div>
              <p>后台页面统一使用顶部工具区、内容工作区和标准操作按钮。</p>
              <p>通用提交动作优先使用“确认”，避免为相同语义重复造新按钮。</p>
            </section>
            <section class="help-card">
              <div class="help-card__title">快捷键</div>
              <p><code>Alt Q</code>：退出登录</p>
              <p><code>Ctrl/Cmd + K</code>：打开搜索</p>
            </section>
          </div>
        </bz-card>
      </div>
    </div>
  </div>
</template>

<style scoped>
.content {
  flex: 1;
  min-height: 0;
  width: 100%;
  overflow-y: auto;
  box-sizing: border-box;
}
.help-grid {
  display: grid;
  gap: 16px;
}
.help-card {
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  background: #fff;
  padding: 18px;
  display: grid;
  gap: 10px;
  color: #334155;
  line-height: 1.7;
}
.help-card__title {
  font-size: 15px;
  font-weight: 800;
  color: #0f172a;
}
</style>
