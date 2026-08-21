import { get, getRuntimeConfig, type RequestOptions } from "@admin/shared/transport";

interface ResourceBootstrapPayload {
  resources?: unknown;
}

export async function fetchResourceBootstrap(options?: RequestOptions): Promise<unknown> {
  const payload = await get<ResourceBootstrapPayload>(getRuntimeConfig().bootstrapPath, options);
  return payload.resources;
}
