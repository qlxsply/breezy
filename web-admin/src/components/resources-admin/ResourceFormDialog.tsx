import { useEffect, useMemo, useState } from "react";

import type { ResourceEntry } from "../../types/resource-admin";
import { BzAlert } from "../bz/BzAlert";
import { BzButton } from "../bz/BzButton";
import { BzDialog } from "../bz/BzDialog";
import { BzForm } from "../bz/BzForm";
import { BzFormItem } from "../bz/BzFormItem";
import { BzInput } from "../bz/BzInput";
import { BzInputNumber } from "../bz/BzInputNumber";
import { BzOption } from "../bz/BzOption";
import { BzSelect } from "../bz/BzSelect";
import { BzSwitch } from "../bz/BzSwitch";

interface ResourceFormDialogProps {
  mode: "create" | "edit";
  model: ResourceEntry | null;
  resources: ResourceEntry[];
  onClose: () => void;
  onSubmit: (model: ResourceEntry) => void;
}

const defaultModel: ResourceEntry = {
  id: "", parentId: "", name: "", icon: "", description: "",
  code: "", type: "MENU", scope: "SETTING", openMode: "PAGE",
  url: "", loadTarget: "", orderNo: 100, level: "CUSTOM",
  enabled: true, guestAccess: false,
};

export function ResourceFormDialog({ mode, model, resources, onClose, onSubmit }: ResourceFormDialogProps) {
  const isLocked = model?.level === "SYSTEM";
  const [form, setForm] = useState<ResourceEntry>(defaultModel);
  const [err, setErr] = useState("");

  useEffect(() => {
    if (model) {
      setForm({ ...model, parentId: model.parentId ?? "" });
    }
  }, [model]);

  const resourceMap = useMemo(() => {
    const map = new Map<string, ResourceEntry>();
    resources.forEach((r) => map.set(r.id, r));
    return map;
  }, [resources]);

  const parentInfo = useMemo(() => {
    const pid = String(form.parentId ?? "").trim();
    return pid ? resourceMap.get(pid) : undefined;
  }, [form.parentId, resourceMap]);

  const parentDisabledLock = useMemo(() => {
    let parent = parentInfo;
    while (parent) {
      if (!parent.enabled) return true;
      if (!parent.parentId) return false;
      parent = resourceMap.get(parent.parentId);
    }
    return false;
  }, [parentInfo, resourceMap]);

  useEffect(() => {
    if (parentDisabledLock) {
      setForm((prev) => ({ ...prev, enabled: false }));
    }
  }, [parentDisabledLock]);

  const parentDisabledWarning = parentInfo && parentDisabledLock && form.enabled;

  const parentOptions = useMemo(() => {
    const currentId = model?.id;
    return resources.filter((r) => {
      if (r.id === currentId) return false;
      if (r.type !== "MENU") return false;
      if (form.type === "MENU") return true;
      return r.openMode === "PAGE";
    });
  }, [resources, model?.id, form.type]);

  function updateField<K extends keyof ResourceEntry>(key: K, value: ResourceEntry[K]) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  function validate(): string {
    if (!form.name.trim()) return "名称不能为空";
    if (!form.code.trim()) return "编码不能为空";
    const parentId = String(form.parentId ?? "").trim();
    if (form.type !== "MENU" && !parentId) return "非菜单资源必须选择父级页面";
    if (parentId) {
      const parent = resourceMap.get(parentId);
      if (!parent) return "父级资源不存在";
      if (parent.type !== "MENU") return "父级资源必须是菜单类型";
      if (form.type !== "MENU" && parent.openMode !== "PAGE") return "非菜单资源必须挂在页面级菜单下";
    }
    if (form.openMode === "PAGE" && !form.url.trim()) return "PAGE 方式需要填写 URL";
    if (form.openMode !== "NONE" && !form.loadTarget.trim()) return "弹窗或页面必须填写加载资源";
    return "";
  }

  function handleSubmit() {
    if (isLocked) return;
    setErr("");
    const e = validate();
    if (e) {
      setErr(e);
      return;
    }
    const parentId = String(form.parentId ?? "").trim();
    const next: ResourceEntry = { ...form, parentId: parentId || null };
    if (next.type !== "MENU") {
      next.scope = "NONE";
      next.guestAccess = false;
    }
    if (next.openMode === "NONE") {
      next.url = "";
      next.loadTarget = "";
    }
    if (parentDisabledLock) next.enabled = false;
    onSubmit({ ...next });
  }

  return (
    <BzDialog
      modelValue={true}
      title={mode === "create" ? "新增资源" : "编辑资源"}
      width="720px"
      onClose={onClose}
      footer={(
        <div style={{ display: "flex", gap: 8, justifyContent: "flex-end" }}>
          <BzButton onClick={onClose}>取消</BzButton>
          <BzButton buttonType="primary" disabled={isLocked} onClick={handleSubmit}>
            {mode === "create" ? "创建" : "保存"}
          </BzButton>
        </div>
      )}
    >
      <BzForm>
        <div className="form-grid">
          <BzFormItem label="名称 *" className="span-2">
            <BzInput
              modelValue={form.name}
              placeholder="例如：JSON 格式化 / 资源管理"
              disabled={isLocked}
              onValueChange={(v) => updateField("name", v)}
            />
          </BzFormItem>

          <BzFormItem label="图标">
            <BzInput
              modelValue={form.icon}
              placeholder="例如：🧭"
              disabled={isLocked}
              onValueChange={(v) => updateField("icon", v)}
            />
          </BzFormItem>

          <BzFormItem label="描述">
            <BzInput
              modelValue={form.description}
              placeholder="一句话描述"
              disabled={isLocked}
              onValueChange={(v) => updateField("description", v)}
            />
          </BzFormItem>

          <BzFormItem label="类型 *">
            <BzSelect modelValue={form.type} disabled={isLocked} onValueChange={(v) => updateField("type", (v as ResourceEntry["type"]) || "MENU")}>
              <BzOption label="MENU（菜单）" value="MENU" />
              <BzOption label="BUTTON（按钮）" value="BUTTON" />
              <BzOption label="FEATURE（功能）" value="FEATURE" />
              <BzOption label="DATA（数据）" value="DATA" />
            </BzSelect>
          </BzFormItem>

          {form.type === "MENU" ? (
            <BzFormItem label="入口范围 *">
              <BzSelect modelValue={form.scope} disabled={isLocked} onValueChange={(v) => updateField("scope", (v as ResourceEntry["scope"]) || "SETTING")}>
                <BzOption label="SETTING（设置搜索）" value="SETTING" />
                <BzOption label="NONE（不进入搜索）" value="NONE" />
              </BzSelect>
            </BzFormItem>
          ) : null}

          <BzFormItem label="父级资源" className="span-2">
            <BzSelect modelValue={form.parentId ?? ""} disabled={isLocked} clearable onValueChange={(v) => updateField("parentId", v ?? "")}>
              <BzOption value="" disabled={form.type !== "MENU"} label="无（根节点）" />
              {parentOptions.map((p) => (
                <BzOption key={p.id} value={p.id} label={`${p.name} (${p.code})`} />
              ))}
            </BzSelect>
            <div className="tip">按钮/功能建议挂在页面级菜单下</div>
            {parentDisabledWarning ? <div className="tip warn">父级已停用，子资源启用可能无效</div> : null}
          </BzFormItem>

          <BzFormItem label="快捷编码 *" className="span-2">
            <BzInput
              modelValue={form.code}
              className="mono"
              placeholder="例如：res / usr.add"
              disabled={isLocked}
              onValueChange={(v) => updateField("code", v)}
            />
            <div className="tip">用于授权识别与搜索的唯一编码</div>
          </BzFormItem>

          <BzFormItem label="打开方式 *">
            <BzSelect modelValue={form.openMode} disabled={isLocked} onValueChange={(v) => updateField("openMode", (v as ResourceEntry["openMode"]) || "NONE")}>
              <BzOption label="NONE（不跳转）" value="NONE" />
              <BzOption label="MODAL（弹窗）" value="MODAL" />
              <BzOption label="PAGE（页面）" value="PAGE" />
            </BzSelect>
          </BzFormItem>

          {form.openMode === "PAGE" ? (
            <BzFormItem label="URL *">
              <BzInput
                modelValue={form.url}
                className="mono"
                placeholder="例如：/sm 或 /configs"
                disabled={isLocked}
                onValueChange={(v) => updateField("url", v)}
              />
            </BzFormItem>
          ) : null}

          {form.openMode !== "NONE" ? (
            <BzFormItem label="加载资源 *" className="span-2">
              <BzInput
                modelValue={form.loadTarget}
                className="mono"
                placeholder="例如：pages/ResourcesAdminPage.vue"
                disabled={isLocked}
                onValueChange={(v) => updateField("loadTarget", v)}
              />
              <div className="tip">组件路径，用于动态 import</div>
            </BzFormItem>
          ) : null}

          <BzFormItem label="序号">
            <BzInputNumber modelValue={form.orderNo} min={0} disabled={isLocked} onValueChange={(v) => updateField("orderNo", v)} />
          </BzFormItem>

          <BzFormItem label="级别 *">
            <BzSelect modelValue={form.level} disabled={isLocked || mode === "create"} onValueChange={(v) => updateField("level", (v as ResourceEntry["level"]) || "CUSTOM")}>
              <BzOption label="SYSTEM（系统内置）" value="SYSTEM" />
              <BzOption label="CUSTOM（自定义）" value="CUSTOM" />
            </BzSelect>
          </BzFormItem>

          <BzFormItem label="启用">
            <BzSwitch
              modelValue={form.enabled}
              disabled={isLocked || parentDisabledLock}
              activeText="启用"
              inactiveText={parentDisabledLock ? "继承停用" : "停用"}
              onValueChange={(v) => updateField("enabled", v)}
            />
          </BzFormItem>

          {form.type === "MENU" ? (
            <BzFormItem label="游客可用">
              <BzSwitch
                modelValue={form.guestAccess}
                disabled={isLocked}
                activeText="允许"
                inactiveText="禁止"
                onValueChange={(v) => updateField("guestAccess", v)}
              />
              <div className="tip">未登录用户可访问该菜单入口</div>
            </BzFormItem>
          ) : null}
        </div>
      </BzForm>

      {isLocked ? <div className="tip locked">系统级资源不可编辑</div> : null}

      {err ? <BzAlert title={err} type="error" showIcon className="form-error" /> : null}

    </BzDialog>
  );
}
