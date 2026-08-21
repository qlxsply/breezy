"use client";

import { ensureRuntimeConfigLoaded, reloadRuntimeConfig } from "@admin/shared/transport";
import { type ReactNode, useEffect, useState } from "react";

interface RuntimeConfigGateProps {
  children: ReactNode;
}

export function RuntimeConfigGate({ children }: RuntimeConfigGateProps) {
  const [state, setState] = useState<"loading" | "ready" | "error">("loading");
  const [errorMessage, setErrorMessage] = useState("");

  const load = (reload: boolean) => {
    setState("loading");
    setErrorMessage("");
    void (reload ? reloadRuntimeConfig() : ensureRuntimeConfigLoaded())
      .then(() => setState("ready"))
      .catch((error: unknown) => {
        setErrorMessage(error instanceof Error ? error.message : "运行时配置加载失败");
        setState("error");
      });
  };

  useEffect(() => {
    load(false);
  }, []);

  if (state === "ready") return children;

  return (
    <main className="runtime-config-gate">
      <section className="runtime-config-gate__panel">
        <div className="runtime-config-gate__eyebrow">Breezy Admin</div>
        <h1>{state === "loading" ? "正在加载运行配置" : "运行配置不可用"}</h1>
        <p>
          {state === "loading"
            ? "正在验证后台服务地址，请稍候。"
            : errorMessage || "请检查部署环境中的 runtime-config.json。"}
        </p>
        {state === "error" ? (
          <button
            type="button"
            onClick={() => load(true)}
          >
            重新加载
          </button>
        ) : null}
      </section>
    </main>
  );
}
