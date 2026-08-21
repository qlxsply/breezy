import { get, type RequestOptions } from "@admin/shared/transport";

import type { DictionaryItem, PublicDictionaryItem } from "./types";

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
): Promise<Record<string, DictionaryItem[]>> {
  const uniqueCodes = [...new Set(codes.map((code) => code.trim()).filter(Boolean))];
  return Promise.all(
    uniqueCodes.map(async (code) => {
      const items = await listPublicDictionaryOptions(code, options);
      return [
        code,
        items.map((item, index) => ({
          id: `${code}:${item.itemCode}`,
          dictTypeId: code,
          parentItemId: null,
          itemCode: item.itemCode,
          itemLabel: item.itemLabel,
          itemValue: item.itemValue,
          sortNo: index + 1,
          enabled: true,
          defaultItem: false,
          tagColor: item.tagColor,
          tagType: item.tagType,
          extraJson: null,
          description: null,
        })),
      ] as const;
    }),
  ).then((entries) => Object.fromEntries(entries));
}
