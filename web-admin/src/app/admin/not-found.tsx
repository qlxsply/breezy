import { AdminRouteFeedback } from "@admin/shared/ui/admin/AdminRouteFeedback";
import buttonStyles from "@admin/shared/ui/bz/BzButton.module.css";
import Link from "next/link";

export default function AdminNotFound() {
  return (
    <AdminRouteFeedback
      badge="提示 / 页面不存在"
      title="页面不存在"
      description="当前地址没有对应的后台页面。"
      action={
        <Link
          className={`${buttonStyles.button} ${buttonStyles.primary}`}
          href="/admin"
        >
          <span className={buttonStyles.label}>返回工作台</span>
        </Link>
      }
    />
  );
}
