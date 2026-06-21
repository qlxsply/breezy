import { AuthLoginPageCard } from "@admin/components/auth/AuthLoginPageCard";

export default function AdminLoginPage() {
  return (
    <AuthLoginPageCard
      scope="internal"
      formTitle="后台登录"
      returnLabel="返回首页"
      returnTo="/"
    />
  );
}
