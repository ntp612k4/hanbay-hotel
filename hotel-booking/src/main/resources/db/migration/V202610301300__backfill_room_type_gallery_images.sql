-- Management and detail pages read the gallery, while the catalog can use a
-- thumbnail. Populate a missing gallery from the existing room type thumbnail.
INSERT INTO room_type_images (
    reference_id, room_type_id, image_url, sort_order, created_at, updated_at
)
SELECT gen_random_uuid(), room_type.id, room_type.thumbnail_url, 0,
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM room_types room_type
WHERE NULLIF(BTRIM(room_type.thumbnail_url), '') IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM room_type_images existing
      WHERE existing.room_type_id = room_type.id
  );

-- Also fill a missing catalog thumbnail from the first gallery image.
WITH first_images AS (
    SELECT DISTINCT ON (room_type_id) room_type_id, image_url
    FROM room_type_images
    ORDER BY room_type_id, sort_order, id
)
UPDATE room_types room_type
SET thumbnail_url = first_images.image_url,
    updated_at = CURRENT_TIMESTAMP
FROM first_images
WHERE room_type.id = first_images.room_type_id
  AND NULLIF(BTRIM(room_type.thumbnail_url), '') IS NULL;
