import type { RequestOptions } from "@admin/shared/transport";

import { pageUserFeaturePackages } from "../api/client";
import type { UserFeaturePackageEntry } from "../model/types";

export type { UserFeaturePackageEntry } from "../model/types";

export async function listUserFeaturePackageCatalog(
  options?: Pick<RequestOptions, "signal">,
): Promise<UserFeaturePackageEntry[]> {
  const request = (pageNo: number) =>
    pageUserFeaturePackages(
      {
        page: { pageNo, pageSize: 100 },
        sort: { orders: [{ field: "packageName", direction: "ASC" }] },
      },
      options,
    );
  const firstPage = await request(1);
  const rows = [...firstPage.elements];
  for (let pageNo = 2; pageNo <= firstPage.totalPages; pageNo += 1) {
    rows.push(...(await request(pageNo)).elements);
  }
  return rows;
}
