import type { UserEntry, UserType } from "../model/types";

export interface UserPayload {
  id: string;
  username: string;
  nickname?: string;
  userType?: UserType;
  status: UserEntry["status"];
  createdBy?: string;
  createdAt?: string;
  updatedBy?: string;
  updatedAt?: string;
}

export interface UserCreatePayload {
  username: string;
  nickname: string;
  password: string;
  roleIds?: number[];
}

export interface UserUpdatePayload {
  nickname: string;
  status: UserEntry["status"];
}

export interface AssignableRolePayload {
  id: string;
  code: string;
  name: string;
  enabled: boolean;
}
