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
              <div class="admin-table-title">个人中心</div>
            </div>
          </template>

          <div class="profile-grid">
            <section class="profile-section">
              <div class="profile-section__head">
                <div class="profile-section__title">基础信息</div>
                <div class="profile-section__actions">
                  <bz-button
                    v-if="editingBasic"
                    :disabled="basicSaving"
                    @click="cancelBasicEdit"
                    >取消</bz-button
                  >
                  <bz-button
                    :type="editingBasic ? 'primary' : 'default'"
                    :loading="basicSaving"
                    @click="editingBasic ? saveBasic() : startBasicEdit()"
                  >
                    {{ editingBasic ? "确认" : "编辑" }}
                  </bz-button>
                </div>
              </div>

              <div
                v-if="profile"
                class="profile-info-grid"
              >
                <div class="profile-info-item">
                  <span class="profile-info-item__label">账号</span>
                  <span class="profile-info-item__value">{{ profile.username }}</span>
                </div>
                <div class="profile-info-item">
                  <span class="profile-info-item__label">昵称</span>
                  <bz-input
                    v-if="editingBasic"
                    v-model="basicForm.nickname"
                    placeholder="请输入昵称"
                  />
                  <span
                    v-else
                    class="profile-info-item__value"
                    >{{ profile.nickname || "-" }}</span
                  >
                </div>
                <div class="profile-info-item">
                  <span class="profile-info-item__label">类型</span>
                  <span class="profile-info-item__value">{{
                    userTypeLabel(profile.userType)
                  }}</span>
                </div>
                <div class="profile-info-item">
                  <span class="profile-info-item__label">状态</span>
                  <span class="profile-info-item__value">{{ profile.status }}</span>
                </div>
                <div class="profile-info-item">
                  <span class="profile-info-item__label">最近一次密码修改</span>
                  <span class="profile-info-item__value">{{
                    formatDateTime(profile.lastPasswordChangedAt)
                  }}</span>
                </div>
                <div class="profile-info-item">
                  <span class="profile-info-item__label">最近更新时间</span>
                  <span class="profile-info-item__value">{{
                    formatDateTime(profile.updatedAt)
                  }}</span>
                </div>
              </div>
            </section>

            <section class="profile-section profile-section--wide">
              <div class="profile-section__head">
                <div class="profile-section__title">最近登录/退出记录</div>
              </div>

              <div class="admin-table-surface">
                <bz-table
                  :data="activityPage.elements"
                  size="small"
                  empty-text="暂无记录"
                >
                  <bz-table-column
                    label="事件"
                    width="120"
                  >
                    <template #default="scope">{{
                      activityTypeLabel(scope.row.eventType)
                    }}</template>
                  </bz-table-column>
                  <bz-table-column
                    label="结果"
                    width="100"
                  >
                    <template #default="scope">
                      <bz-tag :type="scope.row.success ? 'success' : 'danger'">{{
                        scope.row.success ? "成功" : "失败"
                      }}</bz-tag>
                    </template>
                  </bz-table-column>
                  <bz-table-column
                    prop="loginIp"
                    label="IP"
                    min-width="140"
                    show-overflow-tooltip
                  >
                    <template #default="scope">{{ scope.row.loginIp || "-" }}</template>
                  </bz-table-column>
                  <bz-table-column
                    prop="remark"
                    label="备注"
                    min-width="220"
                    show-overflow-tooltip
                  >
                    <template #default="scope">{{ scope.row.remark || "-" }}</template>
                  </bz-table-column>
                  <bz-table-column
                    label="发生时间"
                    width="180"
                  >
                    <template #default="scope">{{ formatDateTime(scope.row.occurredAt) }}</template>
                  </bz-table-column>
                </bz-table>
              </div>

              <div
                v-if="activityPage.totalElements > 0"
                class="dict-pagination-bar profile-pagination"
              >
                <div class="dict-pagination-summary">
                  共 {{ activityPage.totalElements }} 条记录
                </div>
                <div class="dict-pagination-right">
                  <label class="dict-page-size">
                    <select
                      class="dict-page-size__select"
                      :value="activityPageSize"
                      @change="handleActivityPageSizeSelect"
                    >
                      <option
                        v-for="size in pageSizeOptions"
                        :key="size"
                        :value="size"
                      >
                        {{ size }}条/页
                      </option>
                    </select>
                  </label>
                  <div class="dict-page-list">
                    <button
                      class="dict-page-btn dict-page-btn--icon"
                      type="button"
                      :disabled="activityIsFirstPage"
                      @click="goToActivityPage(1)"
                    >
                      <span aria-hidden="true">|&lt;</span>
                    </button>
                    <button
                      class="dict-page-btn dict-page-btn--icon"
                      type="button"
                      :disabled="activityIsFirstPage"
                      @click="goToActivityPage(activityPageNo - 1)"
                    >
                      <span aria-hidden="true">&lt;</span>
                    </button>
                    <template
                      v-for="(token, tokenIndex) in activityPageTokens"
                      :key="`${String(token)}-${tokenIndex}`"
                    >
                      <button
                        v-if="typeof token === 'number'"
                        class="dict-page-btn"
                        :class="{ 'is-active': token === activityPageNo }"
                        type="button"
                        @click="goToActivityPage(token)"
                      >
                        {{ token }}
                      </button>
                      <span
                        v-else
                        class="dict-page-ellipsis"
                        >...</span
                      >
                    </template>
                    <button
                      class="dict-page-btn dict-page-btn--icon"
                      type="button"
                      :disabled="activityIsLastPage"
                      @click="goToActivityPage(activityPageNo + 1)"
                    >
                      <span aria-hidden="true">&gt;</span>
                    </button>
                    <button
                      class="dict-page-btn dict-page-btn--icon"
                      type="button"
                      :disabled="activityIsLastPage"
                      @click="goToActivityPage(activityTotalPages)"
                    >
                      <span aria-hidden="true">&gt;|</span>
                    </button>
                  </div>
                </div>
              </div>
            </section>
          </div>
        </bz-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  type AdminProfileEntry,
  type AdminProfileLoginActivityEntry,
  getAdminProfile,
  pageAdminProfileLoginActivities,
  updateAdminProfile,
} from "@admin/api/admin-profile";
import type { PageResult } from "@admin/types/page";
import { formatDateTime } from "@shared/utils/formatter";
import { message } from "@shared/utils/message";
import { computed, reactive, ref } from "vue";

const profile = ref<AdminProfileEntry | null>(null);
const basicSaving = ref(false);
const editingBasic = ref(false);

const basicForm = reactive({
  nickname: "",
});

const activityPage = ref<PageResult<AdminProfileLoginActivityEntry>>({
  pageNo: 1,
  pageSize: 10,
  numberOfElements: 0,
  totalPages: 0,
  totalElements: 0,
  elements: [],
});
const activityPageNo = ref(1);
const activityPageSize = ref(10);
const pageSizeOptions = [10, 20, 30, 50, 100] as const;

void Promise.all([reloadProfile(), reloadActivities()]);

const activityTotalPages = computed(() => Math.max(1, activityPage.value.totalPages || 1));
const activityIsFirstPage = computed(() => activityPageNo.value <= 1);
const activityIsLastPage = computed(() => activityPageNo.value >= activityTotalPages.value);
const activityPageTokens = computed<Array<number | "ellipsis">>(() =>
  buildTokens(activityPageNo.value, activityTotalPages.value),
);

async function reloadProfile() {
  try {
    profile.value = await getAdminProfile();
    basicForm.nickname = profile.value.nickname || "";
  } catch (error) {
    message.error(error instanceof Error ? error.message : "个人中心加载失败");
  }
}

async function reloadActivities() {
  try {
    activityPage.value = await pageAdminProfileLoginActivities({
      pageNo: activityPageNo.value,
      pageSize: activityPageSize.value,
    });
    activityPageNo.value = activityPage.value.pageNo || 1;
    activityPageSize.value = activityPage.value.pageSize || activityPageSize.value;
  } catch (error) {
    message.error(error instanceof Error ? error.message : "登录记录加载失败");
  }
}

function startBasicEdit() {
  if (!profile.value) return;
  basicForm.nickname = profile.value.nickname || "";
  editingBasic.value = true;
}

function cancelBasicEdit() {
  editingBasic.value = false;
  basicForm.nickname = profile.value?.nickname || "";
}

async function saveBasic() {
  if (!profile.value) return;
  const nickname = basicForm.nickname.trim();
  if (!nickname) {
    message.warning("昵称不能为空");
    return;
  }
  basicSaving.value = true;
  try {
    profile.value = await updateAdminProfile(nickname);
    basicForm.nickname = profile.value.nickname || "";
    editingBasic.value = false;
    message.success("已确认");
  } catch (error) {
    message.error(error instanceof Error ? error.message : "基础信息保存失败");
  } finally {
    basicSaving.value = false;
  }
}

function goToActivityPage(nextPage: number) {
  const target = Math.min(Math.max(nextPage, 1), activityTotalPages.value);
  if (target === activityPageNo.value) return;
  activityPageNo.value = target;
  void reloadActivities();
}

function handleActivityPageSizeSelect(event: Event) {
  const nextPageSize = Number((event.target as HTMLSelectElement).value);
  if (
    !Number.isFinite(nextPageSize) ||
    nextPageSize <= 0 ||
    nextPageSize === activityPageSize.value
  )
    return;
  activityPageSize.value = nextPageSize;
  activityPageNo.value = 1;
  void reloadActivities();
}

function buildTokens(currentPageNo: number, totalPageCount: number): Array<number | "ellipsis"> {
  const total = totalPageCount;
  const current = Math.min(Math.max(currentPageNo, 1), total);
  if (total <= 7) return Array.from({ length: total }, (_, index) => index + 1);
  if (current <= 4) return [1, 2, 3, 4, 5, "ellipsis", total];
  if (current >= total - 3)
    return [1, "ellipsis", total - 4, total - 3, total - 2, total - 1, total];
  return [1, "ellipsis", current - 1, current, current + 1, "ellipsis", total];
}

function userTypeLabel(userType: string): string {
  if (userType === "SYSTEM") return "系统账号";
  if (userType === "INTERNAL") return "账号";
  if (userType === "EXTERNAL") return "用户";
  return "游客";
}

function activityTypeLabel(eventType: string): string {
  if (eventType === "LOGIN_SUCCESS") return "登录";
  if (eventType === "LOGOUT") return "退出";
  return eventType;
}
</script>

<style scoped>
.content {
  flex: 1;
  min-height: 0;
  width: 100%;
  overflow-y: auto;
  box-sizing: border-box;
}
.profile-grid {
  display: grid;
  gap: 16px;
}
.profile-section {
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  background: #fff;
  padding: 18px;
  display: grid;
  gap: 14px;
}
.profile-section--wide {
  grid-column: 1 / -1;
}
.profile-section__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.profile-section__title {
  font-size: 15px;
  font-weight: 800;
  color: #0f172a;
}
.profile-section__actions {
  display: flex;
  gap: 8px;
}
.profile-info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 16px;
}
.profile-info-item {
  display: grid;
  gap: 6px;
}
.profile-info-item__label {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}
.profile-info-item__value {
  color: #0f172a;
  font-size: 14px;
  font-weight: 600;
  word-break: break-all;
}
.dict-pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
  padding: 14px 0 0;
  color: #475569;
}
.dict-pagination-summary {
  font-size: 13px;
  color: #64748b;
}
.dict-pagination-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.dict-page-size__select {
  min-width: 92px;
  height: 32px;
  padding: 0 12px;
  color: #0f172a;
  background: #ffffff;
  border: 1px solid #dbe1ea;
  border-radius: 8px;
  font-size: 13px;
}
.dict-page-list {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.dict-page-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 32px;
  height: 32px;
  padding: 0 10px;
  color: #334155;
  background: #ffffff;
  border: 1px solid #dbe1ea;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
}
.dict-page-btn:hover:not(:disabled) {
  color: #2563eb;
  background: #eff6ff;
  border-color: #bfdbfe;
}
.dict-page-btn.is-active {
  color: #ffffff;
  background: #1677ff;
  border-color: #1677ff;
}
.dict-page-btn:disabled {
  opacity: 0.52;
  cursor: not-allowed;
}
.dict-page-ellipsis {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  color: #94a3b8;
}
@media (max-width: 900px) {
  .profile-info-grid {
    grid-template-columns: 1fr;
  }
  .profile-section__head {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
