-- 운영 사이트가 비어 보이지 않도록 데모 채널·영상을 넣는다.
-- 데모임을 숨기지 않는다: 닉네임은 "데모" 로 시작하고, 영상 설명에 데모라고 밝힌다.
-- 영상은 자유 이용이 허락된 공개 영상(Blender Foundation 오픈 무비 CC BY, MDN CC0)을 원본 주소 그대로 건다.
-- 시청 수·좋아요·댓글·구독자는 지어내지 않는다(0 그대로).
-- 데모 계정은 비밀번호가 BCrypt 값이 아니라서 로그인할 수 없다. 지우려면 docs/06-deployment.md 의 "데모 콘텐츠 지우기".
-- MySQL/TiDB 모두 돌아가는 평범한 INSERT 만 쓴다.
--
-- 서브쿼리(파생 테이블)의 글자와 테이블 열을 비교할 때 TiDB 는 두 쪽 정렬 규칙이 다르면
-- "Illegal mix of collations (utf8mb4_unicode_ci,IMPLICIT) and (utf8mb4_0900_ai_ci,IMPLICIT)" 로 거절한다.
-- (MySQL 은 리터럴 글자를 더 약하게 취급해서 같은 SQL 이 통과한다.) 그래서 비교하는 쪽에 COLLATE 를 명시했다.
-- 중간에 실패해도 다시 돌려도 중복되지 않게 모든 INSERT 에 "이미 있으면 넣지 않는다" 조건을 달았다.

INSERT INTO `categories` (`created_at`, `name`)
SELECT NOW(6), v.name FROM (
  SELECT '애니메이션' AS name UNION ALL SELECT '단편 영화' UNION ALL SELECT '자연'
) v
WHERE NOT EXISTS (SELECT 1 FROM `categories` c WHERE c.name = v.name COLLATE utf8mb4_unicode_ci);

INSERT INTO `users` (`created_at`, `updated_at`, `nickname`, `email`, `public_name`, `password`, `provider`, `role`)
SELECT NOW(6), NOW(6), '데모 블렌더스튜디오', 'demo-blender@demo.invalid', '데모 블렌더스튜디오', '!demo-no-login', 'LOCAL', 'USER'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `users` WHERE `email` = 'demo-blender@demo.invalid');

INSERT INTO `users` (`created_at`, `updated_at`, `nickname`, `email`, `public_name`, `password`, `provider`, `role`)
SELECT NOW(6), NOW(6), '데모 시네마룸', 'demo-cinema@demo.invalid', '데모 시네마룸', '!demo-no-login', 'LOCAL', 'USER'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `users` WHERE `email` = 'demo-cinema@demo.invalid');

INSERT INTO `users` (`created_at`, `updated_at`, `nickname`, `email`, `public_name`, `password`, `provider`, `role`)
SELECT NOW(6), NOW(6), '데모 자연기록', 'demo-nature@demo.invalid', '데모 자연기록', '!demo-no-login', 'LOCAL', 'USER'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `users` WHERE `email` = 'demo-nature@demo.invalid');

INSERT INTO `streams` (`created_at`, `updated_at`, `user_id`, `category_id`, `view_count`, `title`, `description`, `thumbnail_url`, `video_url`)
SELECT v.created_at, v.created_at, u.id, c.id, 0, v.title, v.description, v.thumbnail_url, v.video_url
FROM (
  SELECT 'demo-blender@demo.invalid' AS email, '애니메이션' AS category, TIMESTAMPADD(HOUR, -30, NOW(6)) AS created_at,
         'Big Buck Bunny 공식 예고편 (1080p)' AS title,
         '[데모 영상] 사이트 시연용 채널입니다. Blender Foundation 의 오픈 무비 "Big Buck Bunny" 예고편이며 CC BY 3.0 으로 공개돼 있습니다. (c) Blender Foundation | peach.blender.org' AS description,
         'https://commons.wikimedia.org/wiki/Special:FilePath/Big_buck_bunny_poster_big.jpg?width=640' AS thumbnail_url,
         'https://download.blender.org/peach/trailer/trailer_1080p.mov' AS video_url
  UNION ALL
  SELECT 'demo-blender@demo.invalid', '애니메이션', TIMESTAMPADD(HOUR, -80, NOW(6)),
         'Sintel 예고편 — 용과 소녀의 여정',
         '[데모 영상] 사이트 시연용 채널입니다. Blender Foundation 의 오픈 무비 "Sintel" 예고편이며 CC BY 3.0 으로 공개돼 있습니다. (c) Blender Foundation | durian.blender.org',
         'https://commons.wikimedia.org/wiki/Special:FilePath/Sintel_poster.jpg?width=640',
         'https://download.blender.org/durian/trailer/sintel_trailer-480p.mp4'
  UNION ALL
  SELECT 'demo-cinema@demo.invalid', '단편 영화', TIMESTAMPADD(HOUR, -52, NOW(6)),
         '[데모] Sintel — 극장 예고편으로 보는 판타지 단편',
         '[데모 영상] 사이트 시연용 채널입니다. 같은 원본 영상을 다른 채널·카테고리에서 재생해 보는 용도입니다. Blender Foundation, CC BY 3.0.',
         'https://commons.wikimedia.org/wiki/Special:FilePath/Sintel_poster.jpg?width=640',
         'https://download.blender.org/durian/trailer/sintel_trailer-480p.mp4'
  UNION ALL
  SELECT 'demo-cinema@demo.invalid', '단편 영화', TIMESTAMPADD(HOUR, -140, NOW(6)),
         '[데모] 토끼와 친구들, 숲속 하루 (예고편)',
         '[데모 영상] 사이트 시연용 채널입니다. Blender Foundation 의 "Big Buck Bunny" 예고편을 재사용했습니다. CC BY 3.0.',
         'https://commons.wikimedia.org/wiki/Special:FilePath/Big_buck_bunny_poster_big.jpg?width=640',
         'https://download.blender.org/peach/trailer/trailer_1080p.mov'
  UNION ALL
  SELECT 'demo-nature@demo.invalid', '자연', TIMESTAMPADD(HOUR, -12, NOW(6)),
         '[데모] 꽃 클로즈업 — 짧은 자연 영상',
         '[데모 영상] 사이트 시연용 채널입니다. MDN Web Docs 의 CC0(저작권 포기) 샘플 영상입니다.',
         NULL,
         'https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4'
  UNION ALL
  SELECT 'demo-nature@demo.invalid', '자연', TIMESTAMPADD(HOUR, -200, NOW(6)),
         '[데모] 봄꽃 한 송이 — 1분 안 짧은 영상',
         '[데모 영상] 사이트 시연용 채널입니다. MDN Web Docs 의 CC0 샘플 영상을 재사용했습니다.',
         NULL,
         'https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4'
) v
JOIN `users` u ON u.email = v.email COLLATE utf8mb4_unicode_ci
JOIN `categories` c ON c.name = v.category COLLATE utf8mb4_unicode_ci
WHERE NOT EXISTS (
  SELECT 1 FROM `streams` s
  WHERE s.user_id = u.id
    AND s.video_url = v.video_url COLLATE utf8mb4_unicode_ci
    AND s.title = v.title COLLATE utf8mb4_unicode_ci
);
