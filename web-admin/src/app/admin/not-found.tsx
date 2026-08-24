import { AdminRouteFeedback } from "@admin/shared/ui/admin/AdminRouteFeedback";
import { BzButtonLink } from "@admin/shared/ui/bz/BzButtonLink";

export default function AdminNotFound() {
  return (
    <AdminRouteFeedback
      badge="提示 / 页面不存在"
      title="页面不存在"
      description="当前地址没有对应的后台页面。"
      action={
        <BzButtonLink
          buttonType="primary"
          href="/admin"
        >
          返回工作台
        </BzButtonLink>
      }
    />
  );
}
