import styles from "./AdminTableTools.module.css";

export function AdminTableTools({
  queryPanelVisible,
  onToggleQueryPanel,
  onRefresh,
}: {
  queryPanelVisible: boolean;
  onToggleQueryPanel: () => void;
  onRefresh: () => void;
}) {
  return (
    <>
      <button
        className={[styles.button, queryPanelVisible ? styles.active : ""]
          .filter(Boolean)
          .join(" ")}
        type="button"
        title={queryPanelVisible ? "关闭搜索框" : "打开搜索框"}
        onClick={onToggleQueryPanel}
      >
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <circle
            cx="11"
            cy="11"
            r="8"
          />
          <path d="m21 21-4.35-4.35" />
        </svg>
      </button>
      <button
        className={styles.button}
        type="button"
        title="刷新列表"
        onClick={onRefresh}
      >
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <path d="M21.5 2v6h-6M2.5 12a9 9 0 0 1 15.46-6.35L21.5 8" />
          <path d="M2.5 22v-6h6M21.5 12a9 9 0 0 1-15.46 6.35L2.5 16" />
        </svg>
      </button>
    </>
  );
}
