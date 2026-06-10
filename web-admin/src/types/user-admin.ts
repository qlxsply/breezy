// /src/types/user-admin.ts

export type UserStatus = "ENABLED" | "DISABLED";
export type UserType = "SYSTEM" | "INTERNAL" | "EXTERNAL" | "GUEST";

export interface UserEntry {
  id: string;
  username: string;
  account?: string;
  nickname: string;
  userType: UserType;
  status: UserStatus;
  createdBy?: string;
  createdAt?: string;
  updatedBy?: string;
  updatedAt?: string;
}

export interface UserCreateRequest {
  username: string;
  nickname: string;
  password: string;
}

export interface UserUpdateRequest {
  nickname: string;
  status: UserStatus;
}
