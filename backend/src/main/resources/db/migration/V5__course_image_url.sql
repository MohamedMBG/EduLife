ALTER TABLE courses ADD COLUMN image_url TEXT;

UPDATE courses SET image_url = 'https://images.unsplash.com/photo-1635070041078-e363dbe005cb?w=800&q=80'
WHERE id = '11111111-1111-1111-1111-111111111111';

UPDATE courses SET image_url = 'https://images.unsplash.com/photo-1636466497217-26a8cbeaf0aa?w=800&q=80'
WHERE id = '22222222-2222-2222-2222-222222222222';

UPDATE courses SET image_url = 'https://images.unsplash.com/photo-1503676260728-1c00da094a0b?w=800&q=80'
WHERE id = '33333333-3333-3333-3333-333333333333';

UPDATE courses SET image_url = 'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=800&q=80'
WHERE id = '44444444-4444-4444-4444-444444444444';

UPDATE courses SET image_url = 'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=800&q=80'
WHERE id = '55555555-5555-5555-5555-555555555555';
