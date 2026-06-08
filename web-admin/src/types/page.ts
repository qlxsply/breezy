export interface PageResult<T> {
  pageNo: number;
  pageSize: number;
  numberOfElements: number;
  totalPages: number;
  totalElements: number;
  elements: T[];
}

export interface SortSpec {
  field: string;
  direction: "ASC" | "DESC";
}

export interface PageRule {
  pageNo?: number;
  pageSize?: number;
}

export interface SortRule {
  orders?: SortSpec[];
}

export interface PageQuery {
  page?: PageRule;
  sort?: SortRule;
}
