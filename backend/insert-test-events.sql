-- Тестові дані для event_db: кінотеатри, зали, місця та фільми
-- Запускати після того як event-service створить таблиці (Flyway V1)

DO $$
DECLARE
    v1 UUID := '11111111-1111-1111-1111-111111111111';
    v2 UUID := '22222222-2222-2222-2222-222222222222';
    h1 UUID := 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';
    h2 UUID := 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb';
    e1 UUID := 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee';
    e2 UUID := 'ffffffff-ffff-ffff-ffff-ffffffffffff';
    e3 UUID := 'cccccccc-cccc-cccc-cccc-cccccccccccc';
    organizer UUID := '68e3a800-4be2-4817-8d80-cd7478cb8952';
    new_seat_id UUID;
    row_num INT;
    seat_num INT;
    seat_sector TEXT;
    seat_price DECIMAL;
    seat_rec RECORD;
BEGIN
    -- 1. Кінотеатри (venues)
    INSERT INTO venues (id, name, city, address, created_at, updated_at) VALUES
        (v1, 'Любава', 'Черкаси', 'бул. Шевченка, 208/1', NOW(), NOW()),
        (v2, 'Дніпро Плаза', 'Черкаси', 'вул. Припортова, 34', NOW(), NOW())
    ON CONFLICT (id) DO NOTHING;

    -- 2. Зали (halls)
    INSERT INTO halls (id, name, rows_count, seats_per_row, venue_id, created_at, updated_at) VALUES
        (h1, 'Зал 1', 5, 8, v1, NOW(), NOW()),
        (h2, 'Зал 1', 5, 8, v2, NOW(), NOW())
    ON CONFLICT (id) DO NOTHING;

    -- 3. Місця для залу 1 (Любава) — 5 рядів x 8 місць
    FOR row_num IN 1..5 LOOP
        FOR seat_num IN 1..8 LOOP
            new_seat_id := gen_random_uuid();
            IF row_num <= 2 THEN seat_sector := 'VIP'; seat_price := 250.00;
            ELSIF row_num <= 4 THEN seat_sector := 'A'; seat_price := 180.00;
            ELSE seat_sector := 'B'; seat_price := 120.00;
            END IF;

            INSERT INTO seats (id, row_number, seat_number, sector, hall_id, created_at, updated_at)
            VALUES (new_seat_id, row_num, seat_num, seat_sector, h1, NOW(), NOW())
            ON CONFLICT (hall_id, row_number, seat_number) DO NOTHING;
        END LOOP;
    END LOOP;

    -- 4. Місця для залу 2 (Дніпро Плаза) — 5 рядів x 8 місць
    FOR row_num IN 1..5 LOOP
        FOR seat_num IN 1..8 LOOP
            new_seat_id := gen_random_uuid();
            IF row_num <= 2 THEN seat_sector := 'VIP'; seat_price := 250.00;
            ELSIF row_num <= 4 THEN seat_sector := 'A'; seat_price := 180.00;
            ELSE seat_sector := 'B'; seat_price := 120.00;
            END IF;

            INSERT INTO seats (id, row_number, seat_number, sector, hall_id, created_at, updated_at)
            VALUES (new_seat_id, row_num, seat_num, seat_sector, h2, NOW(), NOW())
            ON CONFLICT (hall_id, row_number, seat_number) DO NOTHING;
        END LOOP;
    END LOOP;

    -- 5. Фільми (events), статус PUBLISHED
    INSERT INTO events (id, title, description, poster_url, starts_at, ends_at, status, organizer_id, hall_id, created_at, updated_at) VALUES
        (e1, 'Дюна: Частина друга',
         'Епічне продовження пригод Пола Атріда на пустельній планеті Арракіс.',
         'https://placehold.co/300x450/e53e3e/ffffff?text=Dune+2',
         NOW() + INTERVAL '1 day' + TIME '18:00',
         NOW() + INTERVAL '1 day' + TIME '21:00',
         'PUBLISHED', organizer, h1, NOW(), NOW()),

        (e2, 'Годзілла vs Конг',
         'Легендарні титани зійдуться у вирішальній битві за панування над Землею.',
         'https://placehold.co/300x450/e53e3e/ffffff?text=Godzilla+Kong',
         NOW() + INTERVAL '1 day' + TIME '20:00',
         NOW() + INTERVAL '1 day' + TIME '22:30',
         'PUBLISHED', organizer, h1, NOW(), NOW()),

        (e3, 'Котячі справи',
         'Захоплива анімаційна пригода кота-детектива у великому місті.',
         'https://placehold.co/300x450/e53e3e/ffffff?text=Cat+Detective',
         NOW() + INTERVAL '2 days' + TIME '16:00',
         NOW() + INTERVAL '2 days' + TIME '18:00',
         'PUBLISHED', organizer, h2, NOW(), NOW())
    ON CONFLICT (id) DO NOTHING;

    -- 6. Event seats для фільму 1 (зал 1)
    FOR seat_rec IN SELECT id, sector FROM seats WHERE hall_id = h1 LOOP
        IF seat_rec.sector = 'VIP' THEN seat_price := 250.00;
        ELSIF seat_rec.sector = 'A' THEN seat_price := 180.00;
        ELSE seat_price := 120.00;
        END IF;

        INSERT INTO event_seats (id, price, status, event_id, seat_id, created_at, updated_at)
        VALUES (gen_random_uuid(), seat_price, 'FREE', e1, seat_rec.id, NOW(), NOW())
        ON CONFLICT (event_id, seat_id) DO NOTHING;
    END LOOP;

    -- 7. Event seats для фільму 2 (зал 1)
    FOR seat_rec IN SELECT id, sector FROM seats WHERE hall_id = h1 LOOP
        IF seat_rec.sector = 'VIP' THEN seat_price := 250.00;
        ELSIF seat_rec.sector = 'A' THEN seat_price := 180.00;
        ELSE seat_price := 120.00;
        END IF;

        INSERT INTO event_seats (id, price, status, event_id, seat_id, created_at, updated_at)
        VALUES (gen_random_uuid(), seat_price, 'FREE', e2, seat_rec.id, NOW(), NOW())
        ON CONFLICT (event_id, seat_id) DO NOTHING;
    END LOOP;

    -- 8. Event seats для фільму 3 (зал 2)
    FOR seat_rec IN SELECT id, sector FROM seats WHERE hall_id = h2 LOOP
        IF seat_rec.sector = 'VIP' THEN seat_price := 250.00;
        ELSIF seat_rec.sector = 'A' THEN seat_price := 180.00;
        ELSE seat_price := 120.00;
        END IF;

        INSERT INTO event_seats (id, price, status, event_id, seat_id, created_at, updated_at)
        VALUES (gen_random_uuid(), seat_price, 'FREE', e3, seat_rec.id, NOW(), NOW())
        ON CONFLICT (event_id, seat_id) DO NOTHING;
    END LOOP;
END $$;
