"use client";

import { message } from "@admin/shared/lib/feedback/message";
import { formatDateTime } from "@admin/shared/lib/formatter";
import {
  type AdminDetailSection,
  AdminDetailTable,
  AdminEditableSection,
  AdminReadonlyListSection,
} from "@admin/shared/ui/admin";
import layoutStyles from "@admin/shared/ui/admin/AdminPageLayout.module.css";
import {
  BzButton,
  BzInput,
  BzSimpleTable,
  type BzSimpleTableColumn,
  BzTag,
} from "@admin/shared/ui/bz";
import { useEffect, useMemo, useRef, useState } from "react";

import { getAdminProfile, updateAdminProfile } from "./api/client";
import type { AdminProfileEntry, AdminProfileLoginActivityEntry } from "./model/types";
import styles from "./Profile.module.css";

function userTypeLabel(userType: string): string {
  if (userType === "SYSTEM") return "系统账号";
  if (userType === "ADMIN") return "账号";
  if (userType === "USER") return "用户";
  return "游客";
}

function activityTypeLabel(eventType: string): string {
  if (eventType === "LOGIN_SUCCESS") return "登录";
  if (eventType === "LOGOUT") return "退出";
  return eventType;
}

function profileStatusLabel(status?: string | null): string {
  if (status === "ENABLED") return "启用";
  if (status === "DISABLED") return "停用";
  return status || "-";
}

export function AdminProfilePage() {
  const [profile, setProfile] = useState<AdminProfileEntry | null>(null);
  const [basicSaving, setBasicSaving] = useState(false);
  const [editingBasic, setEditingBasic] = useState(false);
  const [nicknameDraft, setNicknameDraft] = useState("");
  const loadControllerRef = useRef<AbortController | null>(null);
  const saveLockRef = useRef(false);

  useEffect(() => {
    const controller = new AbortController();
    loadControllerRef.current = controller;
    void reloadProfile(controller);
    return () => controller.abort();
  }, []);

  async function reloadProfile(controller: AbortController) {
    try {
      const data = await getAdminProfile({ signal: controller.signal });
      if (loadControllerRef.current !== controller || controller.signal.aborted) return;
      setProfile(data);
      setNicknameDraft(data.nickname || "");
    } catch (error) {
      if (!controller.signal.aborted) {
        message.error(error instanceof Error ? error.message : "个人中心加载失败");
      }
    }
  }

  function startBasicEdit() {
    if (!profile || saveLockRef.current) return;
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
    saveLockRef.current = true;
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
      saveLockRef.current = false;
      setBasicSaving(false);
    }
  }

  const detailSections = useMemo<AdminDetailSection[]>(
    () => [
      {
        title: "基础信息",
        fields: [
          { label: "账号", value: profile?.username || "-" },
          {
            label: "昵称",
            value: editingBasic ? (
              <BzInput
                modelValue={nicknameDraft}
                placeholder="请输入昵称"
                onValueChange={setNicknameDraft}
              />
            ) : (
              <span>{profile?.nickname || "-"}</span>
            ),
          },
          { label: "类型", value: userTypeLabel(profile?.userType || "") },
          {
            label: "状态",
            value: (
              <BzTag type={profile?.status === "ENABLED" ? "success" : "danger"}>
                {profileStatusLabel(profile?.status)}
              </BzTag>
            ),
          },
          { label: "最近一次密码修改", value: formatDateTime(profile?.lastPasswordChangedAt) },
          { label: "最近更新时间", value: formatDateTime(profile?.updatedAt) },
        ],
      },
    ],
    [editingBasic, nicknameDraft, profile],
  );

  const recentActivities = useMemo(() => (profile?.recentActivities || []).slice(0, 10), [profile]);

  const activityColumns = useMemo<Array<BzSimpleTableColumn<AdminProfileLoginActivityEntry>>>(
    () => [
      {
        key: "eventType",
        title: "事件",
        width: 120,
        text: (row) => activityTypeLabel(row.eventType),
      },
      {
        key: "success",
        title: "结果",
        width: 100,
        render: (row) => (
          <BzTag type={row.success ? "success" : "danger"}>{row.success ? "成功" : "失败"}</BzTag>
        ),
      },
      { key: "loginIp", title: "IP", minWidth: 140 },
      {
        key: "remark",
        title: "备注",
        minWidth: 220,
      },
      {
        key: "occurredAt",
        title: "发生时间",
        width: 180,
        text: (row) => formatDateTime(row.occurredAt),
      },
    ],
    [],
  );

  return (
    <div className={layoutStyles.page}>
      <div className={layoutStyles.content}>
        <div className={layoutStyles.pageStack}>
          <div className={styles.pagePanel}>
            <AdminEditableSection
              title="基础信息"
              actions={
                <>
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
                    {editingBasic ? "保存" : "编辑"}
                  </BzButton>
                </>
              }
            >
              <div className={styles.detailTable}>
                <AdminDetailTable
                  sections={detailSections}
                  variant="plain"
                />
              </div>
            </AdminEditableSection>

            <AdminReadonlyListSection
              title="最近登录/退出记录"
              className={styles.activitySection}
              meta={<div className={styles.activityMeta}>最新 10 条</div>}
            >
              <BzSimpleTable
                columns={activityColumns}
                data={recentActivities}
                rowKey="id"
                emptyText="暂无记录"
              />
            </AdminReadonlyListSection>
          </div>
        </div>
      </div>
    </div>
  );
}
