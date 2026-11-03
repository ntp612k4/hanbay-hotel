import { Alert, Button, Card, Col, Flex, Layout, Row, Skeleton, Typography } from 'antd';
import { useState } from 'react';

import { MainFooter } from '../../../components/layout/MainFooter';
import { MainHeader } from '../../../components/layout/MainHeader';
import { BookingHistorySection } from '../../booking/pages/booking-list-page';
import { PersonalInformationForm } from '../components/PersonalInformationForm';
import { ProfileSidebar, type ProfileSection } from '../components/ProfileSidebar';
import { ProfileSidePanels } from '../components/ProfileSidePanels';
import { ProfileStatistics } from '../components/ProfileStatistics';
import { useCurrentProfile } from '../hooks/use-profile';
import { getProfileErrorMessage } from '../utils/profile-error-mapper';

import './user-page.css';

export function UserPage() {
  const [searchParams] = useState(() => new URLSearchParams(window.location.search));
  const [activeSection, setActiveSection] = useState<ProfileSection>(
    searchParams.get('section') === 'bookings' ? 'bookings' : 'personal',
  );
  const profileQuery = useCurrentProfile();

  return (
    <Layout className="profile-page">
      <MainHeader />
      <main className="profile-page__main">
        <div className="profile-page__container">
          <header className="profile-page__heading">
            <Typography.Title level={2}>Quản lý hồ sơ</Typography.Title>
            <Typography.Paragraph type="secondary">
              Cập nhật thông tin cá nhân và theo dõi lịch sử đặt phòng của bạn.
            </Typography.Paragraph>
          </header>

          {profileQuery.isLoading && <ProfileSkeleton />}
          {profileQuery.isError && (
            <Alert
              type="error"
              showIcon
              message="Không thể tải thông tin cá nhân"
              description={getProfileErrorMessage(profileQuery.error)}
              action={<Button onClick={() => void profileQuery.refetch()}>Thử lại</Button>}
            />
          )}

          {profileQuery.data && (
            <Row gutter={[20, 20]} align="top">
              <Col xs={24} lg={7} xl={6}>
                <ProfileSidebar
                  activeSection={profileQuery.data.roles.length ? 'personal' : activeSection}
                  onSectionChange={setActiveSection}
                  profile={profileQuery.data}
                />
              </Col>
              <Col xs={24} lg={17} xl={18}>
                <Flex vertical gap={20}>
                  <ProfileStatistics profile={profileQuery.data} />
                  {activeSection === 'personal' || profileQuery.data.roles.length > 0 ? (
                    <Row gutter={[20, 20]} align="top">
                      <Col xs={24} xl={16}>
                        <PersonalInformationForm profile={profileQuery.data} />
                      </Col>
                      <Col xs={24} xl={8}>
                        <ProfileSidePanels profile={profileQuery.data} />
                      </Col>
                    </Row>
                  ) : (
                    <Card title="Lịch sử đặt phòng" className="profile-booking-card">
                      <BookingHistorySection
                        reviewBookingCode={
                          searchParams.get('reviewCancellation') ??
                          searchParams.get('bookingCode') ??
                          undefined
                        }
                        openCancellation={searchParams.has('reviewCancellation')}
                      />
                    </Card>
                  )}
                </Flex>
              </Col>
            </Row>
          )}
        </div>
      </main>
      <MainFooter />
    </Layout>
  );
}

function ProfileSkeleton() {
  return (
    <Row gutter={[20, 20]}>
      <Col xs={24} lg={7} xl={6}>
        <Card>
          <Skeleton active avatar paragraph={{ rows: 4 }} />
        </Card>
      </Col>
      <Col xs={24} lg={17} xl={18}>
        <Card>
          <Skeleton active paragraph={{ rows: 10 }} />
        </Card>
      </Col>
    </Row>
  );
}
