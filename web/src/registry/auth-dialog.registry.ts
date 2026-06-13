import { ref } from "vue";

const loginDialogOpen = ref(false);
const loginDialogMode = ref<"login" | "register">("login");
const loginDialogMessage = ref("");
const loginDialogMessageType = ref<"error" | "success">("error");
const loginDialogRedirectPath = ref("");

export function useLoginDialogOpen() {
  return loginDialogOpen;
}

export function useLoginDialogError() {
  return loginDialogMessage;
}

export function useLoginDialogMode() {
  return loginDialogMode;
}

export function useLoginDialogMessageType() {
  return loginDialogMessageType;
}

export function getLoginDialogRedirectPath(): string {
  return loginDialogRedirectPath.value;
}

export function openAuthDialog(
  mode: "login" | "register",
  options?: {
    message?: string;
    messageType?: "error" | "success";
    redirectPath?: string;
  },
): void {
  loginDialogMode.value = mode;
  loginDialogMessage.value = options?.message ?? "";
  loginDialogMessageType.value = options?.messageType ?? "error";
  loginDialogRedirectPath.value = options?.redirectPath ?? "";
  loginDialogOpen.value = true;
}

export function openLoginDialog(error = "登录已失效，请重新登录", redirectPath = ""): void {
  openAuthDialog("login", {
    message: error,
    messageType: "error",
    redirectPath,
  });
}

export function openRegisterDialog(redirectPath = ""): void {
  openAuthDialog("register", { redirectPath });
}

export function closeLoginDialog(): void {
  loginDialogOpen.value = false;
  loginDialogMode.value = "login";
  loginDialogMessage.value = "";
  loginDialogMessageType.value = "error";
  loginDialogRedirectPath.value = "";
}

export function setLoginDialogError(error: string): void {
  loginDialogMessage.value = error;
  loginDialogMessageType.value = "error";
}

export function setLoginDialogSuccess(message: string): void {
  loginDialogMessage.value = message;
  loginDialogMessageType.value = "success";
}

export function setLoginDialogMode(mode: "login" | "register"): void {
  loginDialogMode.value = mode;
  loginDialogMessage.value = "";
  loginDialogMessageType.value = "error";
}
