export interface RoomTypeCatalogItem {
  id: string;
  name: string;
  description: string | null;
  capacityAdults: number;
  capacityChildren: number;
  basePrice: number;
  thumbnailUrl: string | null;
}

export interface RoomTypeCatalogPage {
  content: RoomTypeCatalogItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export type RoomTypeSortField = 'name' | 'basePrice';
export type SortDirection = 'asc' | 'desc';

export interface RoomTypeCatalogParams {
  page: number;
  size: number;
  keyword?: string;
  sortBy: RoomTypeSortField;
  direction: SortDirection;
}
