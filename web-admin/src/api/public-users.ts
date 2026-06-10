import { post } from "./http";

export function registerExternalUser(payload: {
  username: string;
  nickname?: string;
  password: string;
}): Promise<boolean> {
  return post<boolean>("/users/register", payload).then(() => true);
}
