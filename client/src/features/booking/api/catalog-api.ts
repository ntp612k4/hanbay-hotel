import { apiClient } from '../../../lib/axios';
import type { ApiResponse } from '../../../types/api';
import type { AddOnCatalogItem } from '../types/booking';

export async function getAddOns(): Promise<AddOnCatalogItem[]> {
  const response = await apiClient.get<ApiResponse<AddOnCatalogItem[]>>('/add-ons');
  return response.data.data;
}
