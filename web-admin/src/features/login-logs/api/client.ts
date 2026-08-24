import { post, type RequestOptions } from "@admin/shared/transport";
import type { PageResult } from "@admin/shared/types/pagination";

import type { LoginLogEntry } from "../model/types";
import type { LoginLogPageRequest } from "./payload";

const BASE = "/sys/login-logs";

export function pageLoginLogs(
  req: LoginLogPageRequest,
  options?: Pick<RequestOptions, "signal">,
): Promise<PageResult<LoginLogEntry>> {
  return post<PageResult<LoginLogEntry>>(`${BASE}/page`, req, options);
}
