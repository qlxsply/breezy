import { post, type RequestOptions } from "@admin/shared/transport";

interface SseTicketResponse {
  ticket: string;
  expiresAtEpochMillis: number;
}

export function createSseTicket(options?: RequestOptions): Promise<SseTicketResponse> {
  return post<SseTicketResponse>("/sse/ticket", {}, options);
}
