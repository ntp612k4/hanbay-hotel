import { Empty, Image } from 'antd';

interface RoomGalleryProps {
  images: string[];
  roomName: string;
}

export function RoomGallery({ images, roomName }: RoomGalleryProps) {
  if (images.length === 0) return <Empty description="Loại phòng chưa có hình ảnh" />;

  return (
    <Image.PreviewGroup>
      <div className="room-gallery" aria-label={`Hình ảnh ${roomName}`}>
        {images.slice(0, 3).map((image, index) => (
          <Image
            key={image}
            src={image}
            alt={`${roomName} - ảnh ${index + 1}`}
            fallback="/room-placeholder.svg"
          />
        ))}
      </div>
    </Image.PreviewGroup>
  );
}
