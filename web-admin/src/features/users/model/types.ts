export type UserStatus = "ENABLED" | "DISABLED";
export type UserType = "SYSTEM" | "ADMIN" | "USER" | "GUEST";

export interface AssignableRole {
  id: string;
  code: string;
  name: string;
  enabled: boolean;
}

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
  roleIds?: string[];
}

export interface UserUpdateRequest {
  nickname: string;
  status: UserStatus;
}
