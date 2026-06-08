import { post } from "@admin/api/http";

export interface PreviewMsgPushReq {
  msgType: string;
  route: string;
  priority: string;
  sseEnabled: boolean;
  webPushEnabled: boolean;
  panelAutoOpen: boolean;
  osNotificationEnabled: boolean;
}

export function previewMsgPush(req: PreviewMsgPushReq): Promise<string> {
  return post<string>("/sys/configs/preview/msg-push", req);
}
