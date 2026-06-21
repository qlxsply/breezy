import { useEffect, useMemo, useState } from "react";

import type { ApiEntry } from "../../types/api-admin";
import { BzButton } from "../bz/BzButton";
import { BzCheckbox } from "../bz/BzCheckbox";
import { BzDialog } from "../bz/BzDialog";
import { BzEmpty } from "../bz/BzEmpty";
import { BzInput } from "../bz/BzInput";
import { BzLoading } from "../bz/BzLoading";
import { BzTag } from "../bz/BzTag";

interface ResourceApiDialogProps {
  resourceName: string;
  apis: ApiEntry[];
  selectedIds: string[];
  loading?: boolean;
  canSave?: boolean;
  onClose: () => void;
  onSubmit: (ids: string[]) => void;
}

export function ResourceApiDialog({ resourceName, apis, selectedIds, loading = false, canSave = true, onClose, onSubmit }: ResourceApiDialogProps) {
  const [keyword, setKeyword] = useState("");
  const [selectedSet, setSelectedSet] = useState<Set<string>>(new Set());

  useEffect(() => {
    setSelectedSet(new Set(selectedIds || []));
  }, [selectedIds]);

  const filteredApis = useMemo(() => {
    const kw = keyword.trim().toLowerCase();
    const rows = [...apis].sort((left, right) => {
      if (left.module !== right.module) return left.module.localeCompare(right.module);
      if (left.pathPattern !== right.pathPattern) return left.pathPattern.localeCompare(right.pathPattern);
      return left.httpMethod.localeCompare(right.httpMethod);
    });
    if (!kw) return rows;
    return rows.filter((api) => {
      const text = [api.pathPattern, api.module, api.handlerClass, api.handlerMethod]
        .filter(Boolean)
        .join(" ")
        .toLowerCase();
      return text.includes(kw);
    });
  }, [apis, keyword]);

  function toggleApi(id: string, checked: boolean) {
    setSelectedSet((prev) => {
      const next = new Set(prev);
      if (checked) next.add(id);
      else next.delete(id);
      return next;
    });
  }

  function handleSubmit() {
    if (canSave === false) return;
    onSubmit(Array.from(selectedSet));
  }

  return (
    <BzDialog
      modelValue={true}
      title={`API 绑定 - ${resourceName}`}
      width="900px"
      onClose={onClose}
      footer={(
        <div style={{ display: "flex", gap: 8, justifyContent: "flex-end" }}>
          <BzButton onClick={onClose}>取消</BzButton>
          {canSave !== false ? <BzButton buttonType="primary" onClick={handleSubmit}>保存</BzButton> : null}
        </div>
      )}
    >
      <div className="toolbar">
        <BzInput modelValue={keyword} className="keyword-input" placeholder="搜索路径、应用、处理器" clearable onValueChange={setKeyword} />
        <div className="count">已选 {selectedSet.size} 项</div>
      </div>

      <BzLoading loading={loading} className="list">
        {!loading && filteredApis.length === 0 ? (
          <BzEmpty description="暂无 API" />
        ) : (
          <div className="rows">
            {filteredApis.map((api) => (
              <div key={api.id} className="row">
                {canSave !== false ? (
                  <BzCheckbox className="api-checkbox" modelValue={selectedSet.has(api.id)} onChange={(checked) => toggleApi(api.id, checked)} />
                ) : (
                  <span className="readonly-mark">{selectedSet.has(api.id) ? "已绑定" : "-"}</span>
                )}
                <div className="meta">
                  <div className="meta-main">
                    <BzTag size="small" type="info" className="method-tag">{api.httpMethodLabel || api.httpMethod}</BzTag>
                    <span className="mono">{api.pathPattern}</span>
                    <BzTag size="small">{api.module}</BzTag>
                    <BzTag size="small" type={api.enabled ? "success" : "warning"}>{api.enabled ? "启用" : "停用"}</BzTag>
                  </div>
                  <div className="sub">
                    {api.handlerClass || "-"}
                    {api.handlerMethod ? <>#{api.handlerMethod}</> : null}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </BzLoading>

    </BzDialog>
  );
}
