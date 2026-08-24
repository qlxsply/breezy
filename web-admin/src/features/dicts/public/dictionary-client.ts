import { get, type RequestOptions } from "@admin/shared/transport";

import type { PublicDictionaryItem } from "./types";

const PUBLIC_QUERY_BASE = "/public/dicts";

export function listPublicDictionaryOptions(
  code: string,
  options?: Pick<RequestOptions, "signal">,
): Promise<PublicDictionaryItem[]> {
  return get<PublicDictionaryItem[]>(
    `${PUBLIC_QUERY_BASE}/${encodeURIComponent(code)}/items`,
    options,
  );
}

export function batchListDictionaryOptions(
  codes: string[],
  options?: Pick<RequestOptions, "signal">,
): Promise<Record<string, PublicDictionaryItem[]>> {
  const uniqueCodes = [...new Set(codes.map((code) => code.trim()).filter(Boolean))];
  return Promise.all(
    uniqueCodes.map(async (code) => {
      const items = await listPublicDictionaryOptions(code, options);
      return [code, items] as const;
    }),
  ).then((entries) => Object.fromEntries(entries));
}
