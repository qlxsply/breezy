import { KNOWN_ADMIN_SLUGS } from "@admin/components/admin-pages/page-map";
import { AdminRouteViewport } from "@admin/components/admin-shell/AdminRouteViewport";

export function generateStaticParams() {
  return KNOWN_ADMIN_SLUGS.map((slug) => ({ slug: slug.split("/") }));
}

export default function AdminCatchAllPage() {
  return <AdminRouteViewport />;
}
