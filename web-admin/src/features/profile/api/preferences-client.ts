import { get, put, type RequestOptions } from "@admin/shared/transport";
import type { UserConfigItem } from "@admin/shared/types/user-config";

type Options = Pick<RequestOptions, "signal">;
const BASE = "/sys/configs/my";

export function getMyConfigs(options?: Options): Promise<UserConfigItem[]> {
  return get<UserConfigItem[]>(BASE, options);
}

export function updateMyConfig(code: string, value: string): Promise<boolean> {
  return put<boolean>(`${BASE}/${encodeURIComponent(code)}`, { value });
}
