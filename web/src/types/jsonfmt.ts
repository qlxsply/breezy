export interface JsonFmtRecordListItem {
  id: string;
  name: string;
  orderNo: number;
  updatedAt: string;
}

export interface JsonFmtRecordDetail {
  id: string;
  name: string;
  content: string;
  orderNo: number;
  createdAt: string;
  updatedAt: string;
}

export interface JsonFmtRecordSaveRequest {
  id?: string;
  name?: string;
  content: string;
}

export interface JsonFmtRecordRenameRequest {
  name: string;
}

export interface JsonFmtRecordReorderRequest {
  orderedIds: string[];
}

export interface JsonFmtRecordBatchDeleteRequest {
  recordIds: string[];
}
