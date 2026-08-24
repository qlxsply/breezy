import { AdminRouteFeedback } from "@admin/shared/ui/admin/AdminRouteFeedback";

export default function AdminLoading() {
  return (
    <AdminRouteFeedback
      badge="系统 / 加载中"
      title="正在加载页面"
      description="页面资源正在准备，请稍候。"
    />
  );
}
