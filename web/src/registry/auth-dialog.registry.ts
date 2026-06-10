import { ref } from "vue";

const loginDialogOpen = ref(false);
const loginDialogError = ref("");

export function useLoginDialogOpen() {
  return loginDialogOpen;
}

export function useLoginDialogError() {
  return loginDialogError;
}

export function openLoginDialog(error = "登录已失效，请重新登录"): void {
  loginDialogError.value = error;
  loginDialogOpen.value = true;
}

export function closeLoginDialog(): void {
  loginDialogOpen.value = false;
  loginDialogError.value = "";
}

export function setLoginDialogError(error: string): void {
  loginDialogError.value = error;
}
