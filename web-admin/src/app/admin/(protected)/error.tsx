"use client";

import { AdminRouteFeedback } from "@admin/shared/ui/admin/AdminRouteFeedback";
import { BzButton } from "@admin/shared/ui/bz";
import { useEffect } from "react";

export default function AdminError({ error, reset }: { error: Error; reset: () => void }) {
  useEffect(() => {
    console.error("[admin-route] page rendering failed", error);
  }, [error]);

  return (
    <AdminRouteFeedback
      badge="系统 / 页面异常"
      title="页面加载失败"
      description="当前页面发生异常，可以重试或返回工作台。"
      action={
        <div style={{ display: "flex", gap: 8 }}>
          <BzButton
            buttonType="primary"
            onClick={reset}
          >
            重新加载
          </BzButton>
          <BzButton onClick={() => window.location.assign("/admin")}>返回工作台</BzButton>
        </div>
      }
    />
  );
}
