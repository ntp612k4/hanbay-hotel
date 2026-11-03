import { useMutation } from '@tanstack/react-query';
import { App, Form } from 'antd';
import { useMemo } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

import { MainFooter } from '@components/layout/MainFooter';
import { MainHeader } from '@components/layout/MainHeader';
import { searchAvailableRoomTypes } from '../api/available-room-search-api';
import { AvailableRoomResults } from './available-room-results';
import { RoomAvailabilityForm } from './room-availability-form';
import { RoomSearchHero } from './room-search-hero';
import { getInitialSearchValues, type SearchFormValues } from './room-search-utils';
import './room-search-page.css';

const PAGE_SIZE = 6;

interface AvailableRoomSearchVariables {
  values: SearchFormValues;
  page: number;
}

export function RoomSearchPage() {
  const { message } = App.useApp();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [form] = Form.useForm<SearchFormValues>();

  const initialValues = useMemo(() => getInitialSearchValues(searchParams), [searchParams]);
  const watchedValues = Form.useWatch([], form) as SearchFormValues | undefined;
  const currentValues = watchedValues ?? initialValues;

  const searchMutation = useMutation({
    mutationFn: ({ values, page }: AvailableRoomSearchVariables) => {
      const [searchCheckIn, searchCheckOut] = values.stayDates;
      return searchAvailableRoomTypes({
        checkIn: searchCheckIn.format('YYYY-MM-DD'),
        checkOut: searchCheckOut.format('YYYY-MM-DD'),
        adults: values.adults,
        children: values.children,
        page,
        size: PAGE_SIZE,
      });
    },
    onSuccess: (rooms, variables) => {
      if (variables.page !== 0) return;
      if (rooms.totalElements) {
        void message.success('Đã cập nhật danh sách hạng phòng còn trống.');
      } else {
        void message.info('Không tìm thấy hạng phòng phù hợp với tiêu chí đã chọn.');
      }
    },
    onError: (error) => {
      const errorMessage = error instanceof Error ? error.message : 'Không thể tìm phòng trống.';
      void message.error(errorMessage);
    },
  });
  const resultValues = searchMutation.variables?.values ?? currentValues;
  const [checkIn, checkOut] = resultValues.stayDates;
  const nights = Math.max(checkOut.diff(checkIn, 'day'), 1);

  const handleBookRoom = (roomTypeId: string) => {
    const values = searchMutation.variables?.values ?? form.getFieldsValue();
    const [bookingCheckIn, bookingCheckOut] = values.stayDates;
    const params = new URLSearchParams({
      roomTypeId,
      checkIn: bookingCheckIn.format('YYYY-MM-DD'),
      checkOut: bookingCheckOut.format('YYYY-MM-DD'),
      adults: String(values.adults),
      children: String(values.children),
    });

    navigate(`/bookings/new?${params.toString()}`);
  };

  return (
    <div className="room-search-page">
      <MainHeader />

      <main>
        <RoomSearchHero />
        <RoomAvailabilityForm
          form={form}
          initialValues={initialValues}
          searchLoading={searchMutation.isPending}
          onSearch={(values) => searchMutation.mutate({ values, page: 0 })}
        />

        <AvailableRoomResults
          availableRooms={searchMutation.data ?? null}
          currentValues={resultValues}
          loading={searchMutation.isPending}
          nights={nights}
          onBookRoom={handleBookRoom}
          onPageChange={(page) => {
            const values = searchMutation.variables?.values ?? form.getFieldsValue();
            searchMutation.mutate({ values, page: page - 1 });
          }}
        />
      </main>
      <MainFooter />
    </div>
  );
}
