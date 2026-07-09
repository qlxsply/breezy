"use client";

import type { AdminProfileEntry, AdminProfileLoginActivityEntry } from "@admin/api/admin-profile";
import {
  getAdminProfile,
  pageAdminProfileLoginActivities,
  updateAdminProfile,
} from "@admin/api/admin-profile";
import type { BzTableColumn } from "@admin/components/bz";
import { BzButton, BzCard, BzInput, BzPagination, BzTable, BzTag } from "@admin/components/bz";
import { formatDateTime } from "@admin/core/formatter";
import { message } from "@admin/core/message";
import type { PageResult } from "@admin/types/page";
import { useEffect, useMemo, useRef, useState } from "react";

const pageSizeOptions = [10, 20, 30, 50, 100];

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

export function AdminProfilePage() {
  const [profile, setProfile] = useState<AdminProfileEntry | null>(null);
  const [basicSaving, setBasicSaving] = useState(false);
  const [editingBasic, setEditingBasic] = useState(false);
  const [nicknameDraft, setNicknameDraft] = useState("");

  const [activityPage, setActivityPage] = useState<PageResult<AdminProfileLoginActivityEntry>>({
    pageNo: 1,
    pageSize: 10,
    numberOfElements: 0,
    totalPages: 0,
    totalElements: 0,
    elements: [],
  });
  const [activityPageNo, setActivityPageNo] = useState(1);
  const [activityPageSize, setActivityPageSize] = useState(10);
  const loadedRef = useRef(false);

  useEffect(() => {
    if (loadedRef.current) return;
    loadedRef.current = true;
    void Promise.all([reloadProfile(), reloadActivities()]);
  }, []);

  const activityTotalPages = useMemo(
    () => Math.max(1, activityPage.totalPages || 1),
    [activityPage.totalPages],
  );

  async function reloadProfile() {
    try {
      const data = await getAdminProfile();
      setProfile(data);
      setNicknameDraft(data.nickname || "");
    } catch (error) {
      message.error(error instanceof Error ? error.message : "个人中心加载失败");
    }
  }

  async function reloadActivities() {
    try {
      const page = await pageAdminProfileLoginActivities({
        pageNo: activityPageNo,
        pageSize: activityPageSize,
      });
      setActivityPage(page);
      setActivityPageNo(page.pageNo || 1);
      setActivityPageSize(page.pageSize || activityPageSize);
    } catch (error) {
      message.error(error instanceof Error ? error.message : "登录记录加载失败");
    }
  }

  function startBasicEdit() {
    if (!profile) return;
    setNicknameDraft(profile.nickname || "");
    setEditingBasic(true);
  }

  function cancelBasicEdit() {
    setEditingBasic(false);
    setNicknameDraft(profile?.nickname || "");
  }

  async function saveBasic() {
    if (!profile) return;
    const nickname = nicknameDraft.trim();
    if (!nickname) {
      message.warning("昵称不能为空");
      return;
    }
    setBasicSaving(true);
    try {
      const updated = await updateAdminProfile(nickname);
      setProfile(updated);
      setNicknameDraft(updated.nickname || "");
      setEditingBasic(false);
      message.success("已确认");
    } catch (error) {
      message.error(error instanceof Error ? error.message : "基础信息保存失败");
    } finally {
      setBasicSaving(false);
    }
  }

  useEffect(() => {
    if (loadedRef.current && activityPageNo !== 1) {
      void reloadActivities();
    }
  }, [activityPageNo]);

  useEffect(() => {
    if (loadedRef.current) {
      void reloadActivities();
    }
  }, [activityPageSize]);

  const activityColumns = useMemo<Array<BzTableColumn<AdminProfileLoginActivityEntry>>>(
    () => [
      {
        key: "eventType",
        title: "事件",
        width: 120,
        render: (row) => <>{activityTypeLabel(row.eventType)}</>,
      },
      {
        key: "success",
        title: "结果",
        width: 100,
        render: (row) => (
          <BzTag type={row.success ? "success" : "danger"}>{row.success ? "成功" : "失败"}</BzTag>
        ),
      },
      {
        key: "loginIp",
        title: "IP",
        minWidth: 140,
        render: (row) => <>{row.loginIp || "-"}</>,
      },
      {
        key: "remark",
        title: "备注",
        minWidth: 220,
        render: (row) => <>{row.remark || "-"}</>,
      },
      {
        key: "occurredAt",
        title: "发生时间",
        width: 180,
        render: (row) => <>{formatDateTime(row.occurredAt)}</>,
      },
    ],
    [],
  );

  return (
    <div className="admin-page">
      <div className="content">
        <div className="admin-page-stack">
          <BzCard
            className="admin-panel admin-table-card"
            shadow="never"
            header={
              <div className="admin-table-header">
                <div className="admin-table-title">个人中心</div>
              </div>
            }
          >
            <div className="profile-grid">
              <section className="profile-section">
                <div className="profile-section__head">
                  <div className="profile-section__title">基础信息</div>
                  <div className="profile-section__actions">
                    {editingBasic ? (
                      <BzButton
                        disabled={basicSaving}
                        onClick={cancelBasicEdit}
                      >
                        取消
                      </BzButton>
                    ) : null}
                    <BzButton
                      buttonType={editingBasic ? "primary" : undefined}
                      loading={basicSaving}
                      onClick={editingBasic ? saveBasic : startBasicEdit}
                    >
                      {editingBasic ? "确认" : "编辑"}
                    </BzButton>
                  </div>
                </div>

                {profile ? (
                  <div className="profile-info-grid">
                    <div className="profile-info-item">
                      <span className="profile-info-item__label">账号</span>
                      <span className="profile-info-item__value">{profile.username}</span>
                    </div>
                    <div className="profile-info-item">
                      <span className="profile-info-item__label">昵称</span>
                      {editingBasic ? (
                        <BzInput
                          modelValue={nicknameDraft}
                          placeholder="请输入昵称"
                          onValueChange={setNicknameDraft}
                        />
                      ) : (
                        <span className="profile-info-item__value">{profile.nickname || "-"}</span>
                      )}
                    </div>
                    <div className="profile-info-item">
                      <span className="profile-info-item__label">类型</span>
                      <span className="profile-info-item__value">
                        {userTypeLabel(profile.userType)}
                      </span>
                    </div>
                    <div className="profile-info-item">
                      <span className="profile-info-item__label">状态</span>
                      <span className="profile-info-item__value">{profile.status}</span>
                    </div>
                    <div className="profile-info-item">
                      <span className="profile-info-item__label">最近一次密码修改</span>
                      <span className="profile-info-item__value">
                        {formatDateTime(profile.lastPasswordChangedAt)}
                      </span>
                    </div>
                    <div className="profile-info-item">
                      <span className="profile-info-item__label">最近更新时间</span>
                      <span className="profile-info-item__value">
                        {formatDateTime(profile.updatedAt)}
                      </span>
                    </div>
                  </div>
                ) : null}
              </section>

              <section className="profile-section profile-section--wide">
                <div className="profile-section__head">
                  <div className="profile-section__title">最近登录/退出记录</div>
                </div>

                <div className="admin-table-surface">
                  <BzTable
                    columns={activityColumns}
                    data={activityPage.elements}
                    size="small"
                    emptyText="暂无记录"
                  />
                </div>

                {activityPage.totalElements > 0 ? (
                  <div className="dict-pagination-bar profile-pagination">
                    <div className="dict-pagination-summary">
                      共 {activityPage.totalElements} 条记录
                    </div>
                    <div className="dict-pagination-right">
                      <BzPagination
                        total={activityPage.totalElements}
                        pageSize={activityPageSize}
                        currentPage={activityPageNo}
                        pageSizes={pageSizeOptions}
                        onCurrentChange={setActivityPageNo}
                        onSizeChange={(size) => {
                          if (!Number.isFinite(size) || size <= 0 || size === activityPageSize)
                            return;
                          setActivityPageSize(size);
                          setActivityPageNo(1);
                        }}
                      />
                    </div>
                  </div>
                ) : null}
              </section>
            </div>
          </BzCard>
        </div>
      </div>
    </div>
  );
}
