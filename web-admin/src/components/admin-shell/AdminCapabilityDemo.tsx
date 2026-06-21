"use client";

import { BzAlert, BzButton, BzCard, BzForm, BzFormItem, BzInput } from "@admin/components/bz";
import { resolveBootstrapUrl } from "@admin/core/bootstrap-config";
import { bzConfirm } from "@admin/core/confirm";
import { getPublicRuntimeEnv } from "@admin/core/env";
import { formatDateTime, formatDecimal } from "@admin/core/formatter";
import { message } from "@admin/core/message";
import { readJson, writeJson } from "@admin/core/storage";
import { useState } from "react";

const DEMO_STORAGE_KEY = "breezy:admin:capability-demo";

export function AdminCapabilityDemo() {
  const [text, setText] = useState(readJson<string>(DEMO_STORAGE_KEY, ""));
  const env = getPublicRuntimeEnv();

  async function handleConfirmDemo() {
    const accepted = await bzConfirm({
      title: "基础能力确认",
      content: `当前输入内容为：${text || "(空)"}，确认继续吗？`,
      confirmText: "继续",
      cancelText: "取消",
    });

    if (accepted) {
      message.success("确认能力已接通");
      return;
    }

    message.info("已取消操作");
  }

  return (
    <BzCard
      shadow="never"
      header={<div style={{ fontWeight: 600 }}>T5 基础能力迁移验证</div>}
    >
      <div style={{ display: "grid", gap: 16 }}>
        <BzAlert
          title="以下区域用于确认新的 core 能力已经从 Vue 版剥离，并可以在 Next/React 下独立工作。"
          type="info"
          showIcon
          closable={false}
        />

        <BzForm>
          <BzFormItem label="本地存储示例">
            <BzInput
              modelValue={text}
              placeholder="输入任意文本后点保存"
              clearable
              onValueChange={setText}
            />
          </BzFormItem>
        </BzForm>

        <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
          <BzButton
            buttonType="primary"
            onClick={() => {
              writeJson(DEMO_STORAGE_KEY, text);
              message.success("已写入 localStorage");
            }}
          >
            保存存储
          </BzButton>
          <BzButton onClick={() => setText(readJson<string>(DEMO_STORAGE_KEY, ""))}>
            读取存储
          </BzButton>
          <BzButton
            buttonType="warning"
            onClick={() => void handleConfirmDemo()}
          >
            确认弹层
          </BzButton>
          <BzButton
            buttonType="success"
            onClick={() => message.success("消息桥接已接通")}
          >
            消息提示
          </BzButton>
        </div>

        <div style={{ display: "grid", gap: 6, color: "#475569", fontSize: 13 }}>
          <div>API Base：{env.apiBaseUrl}</div>
          <div>Bootstrap URL：{resolveBootstrapUrl()}</div>
          <div>当前时间格式化：{formatDateTime(Date.now())}</div>
          <div>数值格式化：{formatDecimal(123456.789)}</div>
        </div>
      </div>
    </BzCard>
  );
}
