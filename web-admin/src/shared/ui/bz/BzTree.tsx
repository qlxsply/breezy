"use client";

import type { MouseEvent, ReactNode } from "react";
import { useState } from "react";

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type TreeNodeData = { [key: string]: any };

interface TreePropsMap {
  label?: string;
  children?: string;
}

interface BzTreeProps {
  data: TreeNodeData[];
  nodeKey?: string;
  defaultExpandAll?: boolean;
  props?: TreePropsMap;
  children?: (node: TreeNodeData) => ReactNode;
}

function readNodeKey(node: TreeNodeData, nodeKey: string): string {
  const value = node[nodeKey];
  return value === undefined || value === null ? JSON.stringify(node) : String(value);
}

function readChildren(node: TreeNodeData, childrenKey: string): TreeNodeData[] {
  const value = node[childrenKey];
  return Array.isArray(value) ? (value as TreeNodeData[]) : [];
}

interface TreeNodeProps {
  node: TreeNodeData;
  level: number;
  expandedKeys: Set<string>;
  nodeKey: string;
  propsMap: Required<TreePropsMap>;
  defaultExpandAll: boolean;
  onToggle: (key: string) => void;
  renderContent?: (node: TreeNodeData) => ReactNode;
}

function TreeNode({
  node,
  level,
  expandedKeys,
  nodeKey,
  propsMap,
  defaultExpandAll,
  onToggle,
  renderContent,
}: TreeNodeProps) {
  const key = readNodeKey(node, nodeKey);
  const children = readChildren(node, propsMap.children);
  const isLeaf = children.length === 0;
  const expanded = defaultExpandAll || expandedKeys.has(key);

  function handleToggle(event: MouseEvent) {
    event.stopPropagation();
    if (!isLeaf && !defaultExpandAll) {
      onToggle(key);
    }
  }

  return (
    <div className="bz-tree-node">
      <div
        className="bz-tree-node__line"
        style={{ paddingLeft: `${level * 16}px` }}
      >
        <button
          className="bz-tree-node__toggle"
          type="button"
          onClick={handleToggle}
          disabled={isLeaf}
        >
          {isLeaf ? "" : expanded ? "▾" : "▸"}
        </button>
        <div className="bz-tree-node__content">
          {renderContent ? renderContent(node) : String(node[propsMap.label] ?? "-")}
        </div>
      </div>
      {expanded &&
        children.map((child) => (
          <TreeNode
            key={readNodeKey(child, nodeKey)}
            node={child}
            level={level + 1}
            expandedKeys={expandedKeys}
            nodeKey={nodeKey}
            propsMap={propsMap}
            defaultExpandAll={defaultExpandAll}
            onToggle={onToggle}
            renderContent={renderContent}
          />
        ))}
    </div>
  );
}

export function BzTree({
  data = [],
  nodeKey = "id",
  defaultExpandAll = false,
  props: treeProps,
  children,
}: BzTreeProps) {
  const [expandedKeys, setExpandedKeys] = useState<Set<string>>(new Set());
  const propsMap: Required<TreePropsMap> = {
    label: treeProps?.label ?? "label",
    children: treeProps?.children ?? "children",
  };

  function handleToggle(key: string) {
    setExpandedKeys((prev) => {
      const next = new Set(prev);
      if (next.has(key)) {
        next.delete(key);
      } else {
        next.add(key);
      }
      return next;
    });
  }

  return (
    <div className="bz-tree">
      {data.map((node) => (
        <TreeNode
          key={readNodeKey(node, nodeKey)}
          node={node}
          level={0}
          expandedKeys={expandedKeys}
          nodeKey={nodeKey}
          propsMap={propsMap}
          defaultExpandAll={defaultExpandAll}
          onToggle={handleToggle}
          renderContent={children}
        />
      ))}
    </div>
  );
}
