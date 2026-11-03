export interface Amenity {
  id: string;
  name: string;
  icon: string;
}

export interface RoomTypeDetail {
  id: string;
  name: string;
  shortDescription: string | null;
  description: string;
  basePrice: number;
  maxGuests: number;
  capacityAdults: number;
  capacityChildren: number;
  images: string[];
  amenities: Amenity[];
}

export interface ApiEnvelope<T> {
  success: boolean;
  message: string;
  data: T;
}
