"use client";

import { useRouter } from "next/navigation";

export function AdminNotFoundPage() {
  const router = useRouter();

  return (
    <div className="admin-page-placeholder">
      <section className="admin-placeholder-card">
        <h1>404</h1>
        <p>未找到对应页面或工具。</p>
        <div className="admin-placeholder-actions">
          <button className="admin-placeholder-btn primary" type="button" onClick={() => router.replace("/admin")}>
            返回首页
          </button>
        </div>
      </section>
    </div>
  );
}
