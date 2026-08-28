-- 지금까지 ddl-auto: update 가 만들어 온 스키마를 그대로 옮겨 적은 것이다.
-- 실제로 만들어진 MySQL 8.0 스키마를 mysqldump 로 뽑아서 표 사이 의존 순서만 정리했다.
--
-- 제약 이름이 FKa3ybpouwgm1qdt5vkjottnte6 처럼 읽기 어려운 것은 일부러 그대로 뒀다.
-- Hibernate 가 표·칸 이름에서 만들어 내는 이름이라 이미 돌고 있는 DB 에도 같은 이름으로 박혀 있고,
-- 여기서 이름을 예쁘게 바꾸면 새로 만든 DB 와 쓰던 DB 의 이름이 갈라져
-- 나중에 "이 제약을 떨어뜨려라" 하는 마이그레이션을 두 벌 써야 한다.
--
-- 이미 표가 있는 DB 는 baseline-on-migrate 로 이 파일을 건너뛰고 도장만 찍는다.

CREATE TABLE `users` (
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `nickname` varchar(20) NOT NULL,
  `email` varchar(100) NOT NULL,
  `public_name` varchar(100) DEFAULT NULL,
  `stream_key` varchar(100) DEFAULT NULL,
  `password` varchar(255) NOT NULL,
  `profile_image` varchar(255) DEFAULT NULL,
  `provider_id` varchar(255) DEFAULT NULL,
  `provider` enum('GOOGLE','KAKAO','LOCAL','NAVER') NOT NULL,
  `role` enum('ADMIN','USER') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK2ty1xmrrgtn89xt7kyxx6ta7h` (`nickname`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
  UNIQUE KEY `UKcppkpcylo20yd26ngpirqnjgv` (`public_name`),
  UNIQUE KEY `UKghsgu9lm2sc6t3nbfes5v8usp` (`stream_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `categories` (
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(30) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKt8o6pivur7nn124jehx7cygw5` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `streams` (
  `category_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `view_count` bigint NOT NULL,
  `title` varchar(100) NOT NULL,
  `description` text,
  `thumbnail_url` varchar(255) DEFAULT NULL,
  `video_url` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKs1cjwu0c7avccuhi9cyqcnj56` (`category_id`),
  KEY `FKa3ybpouwgm1qdt5vkjottnte6` (`user_id`),
  CONSTRAINT `FKa3ybpouwgm1qdt5vkjottnte6` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKs1cjwu0c7avccuhi9cyqcnj56` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `comments` (
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `parent_id` bigint DEFAULT NULL,
  `stream_id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `content` text NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_comments_stream_parent` (`stream_id`,`parent_id`),
  KEY `idx_comments_parent` (`parent_id`),
  KEY `FK8omq0tc18jd43bu5tjh6jvraq` (`user_id`),
  CONSTRAINT `FK8omq0tc18jd43bu5tjh6jvraq` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FK9cr9uxbigatqtxw77d2q6txyp` FOREIGN KEY (`stream_id`) REFERENCES `streams` (`id`),
  CONSTRAINT `FKlri30okf66phtcgbe5pok7cc0` FOREIGN KEY (`parent_id`) REFERENCES `comments` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `likes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `stream_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKr6fyvek687x9ws0bpfmm0b2mi` (`user_id`,`stream_id`),
  KEY `FK51tmkya31wrqtvk06xy65per1` (`stream_id`),
  CONSTRAINT `FK51tmkya31wrqtvk06xy65per1` FOREIGN KEY (`stream_id`) REFERENCES `streams` (`id`),
  CONSTRAINT `FKnvx9seeqqyy71bij291pwiwrg` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `subscriptions` (
  `channel_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `subscriber_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKcl91bj6bsg2kvi3eity8yc4hk` (`subscriber_id`,`channel_id`),
  KEY `FK6jw0fljimmo9qisnhob8eiv45` (`channel_id`),
  CONSTRAINT `FK6jw0fljimmo9qisnhob8eiv45` FOREIGN KEY (`channel_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKoodc4352epkjrvxx79odlxbji` FOREIGN KEY (`subscriber_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `blocks` (
  `blocked_id` bigint NOT NULL,
  `blocker_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKo6p3yjxo8qvcqrxt673wxsx63` (`blocker_id`,`blocked_id`),
  KEY `FKjcracc6rem4ddb6gtkaxhfsvu` (`blocked_id`),
  CONSTRAINT `FKjcracc6rem4ddb6gtkaxhfsvu` FOREIGN KEY (`blocked_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKl05bxnwmimx5n5tsgrgnguvu6` FOREIGN KEY (`blocker_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `refresh_tokens` (
  `created_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `token_hash` varchar(64) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKo2mlirhldriil2y7krapq4frt` (`token_hash`),
  KEY `FK1lih5y2npsf8u5o3vhdb9y0os` (`user_id`),
  CONSTRAINT `FK1lih5y2npsf8u5o3vhdb9y0os` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `reports` (
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `reporter_id` bigint NOT NULL,
  `target_id` bigint NOT NULL,
  `reason` varchar(500) NOT NULL,
  `status` enum('PENDING','REJECTED','RESOLVED') NOT NULL,
  `target_type` enum('COMMENT','LIVE_STREAM','STREAM','USER') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKd3qiw2om5d2oh5xb7fbdcq225` (`reporter_id`),
  CONSTRAINT `FKd3qiw2om5d2oh5xb7fbdcq225` FOREIGN KEY (`reporter_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `notifications` (
  `is_read` bit(1) NOT NULL,
  `channel_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `recipient_id` bigint NOT NULL,
  `target_id` bigint DEFAULT NULL,
  `type` varchar(30) NOT NULL,
  `message` varchar(200) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_notifications_recipient` (`recipient_id`,`is_read`),
  CONSTRAINT `FKqqnsjxlwleyjbxlmm213jaj3f` FOREIGN KEY (`recipient_id`) REFERENCES `users` (`id`),
  CONSTRAINT `notifications_chk_1` CHECK ((`type` in ('LIVE_START','STREAM_COMMENT','COMMENT_REPLY')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `live_settings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `title` varchar(100) NOT NULL,
  `description` text,
  `thumbnail_url` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKfami3g3tgw0jvwg686hnk5fqo` (`user_id`),
  CONSTRAINT `FK2068t3hxlegis7we95xn0rife` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `live_streams` (
  `ended_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `peak_viewer_count` bigint NOT NULL,
  `started_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `stream_name` varchar(100) NOT NULL,
  `title` varchar(100) NOT NULL,
  `description` text,
  `thumbnail_url` varchar(255) DEFAULT NULL,
  `status` enum('ENDED','LIVE') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_live_streams_status` (`status`),
  KEY `idx_live_streams_user` (`user_id`),
  CONSTRAINT `FK9715d2g2cjn93ahw2m2ej6n0v` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `chat_messages` (
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `live_stream_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `content` varchar(500) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_chat_messages_live` (`live_stream_id`,`id`),
  KEY `FK6f0y4l43ihmgfswkgy9yrtjkh` (`user_id`),
  CONSTRAINT `FK6f0y4l43ihmgfswkgy9yrtjkh` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKq4y9cxm4bo67h9jxopsw56g0` FOREIGN KEY (`live_stream_id`) REFERENCES `live_streams` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `channel_intros` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `headline` varchar(60) DEFAULT NULL,
  `greeting` text,
  `video_url` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKdro1s9xwidfhkya9gbb1c37nr` (`user_id`),
  CONSTRAINT `FK20jeuoahvwsfjs8ciod2sdw7w` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `intro_impressions` (
  `channel_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `action` varchar(20) NOT NULL,
  `viewer_key` varchar(120) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKkoh8ismsk0vd91wanmodqqwjb` (`viewer_key`,`channel_id`),
  KEY `idx_intro_impressions_viewer` (`viewer_key`,`action`),
  KEY `FKj3qflsnkl9vfqp6yj54t2xsv9` (`channel_id`),
  CONSTRAINT `FKj3qflsnkl9vfqp6yj54t2xsv9` FOREIGN KEY (`channel_id`) REFERENCES `users` (`id`),
  CONSTRAINT `intro_impressions_chk_1` CHECK ((`action` in ('SKIP','WATCHED','PASS')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `channel_profiles` (
  `debut_on` date DEFAULT NULL,
  `graduated_on` date DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `fan_name` varchar(30) DEFAULT NULL,
  `oshi_mark_url` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK1jt1bku6rhni9s06q036icc5` (`user_id`),
  CONSTRAINT `FK4r335vrlqq7xq335b6ijb6jgd` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `model_credits` (
  `position` int NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `role` varchar(30) NOT NULL,
  `name` varchar(60) NOT NULL,
  `link` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_model_credits_user` (`user_id`,`position`),
  CONSTRAINT `FKjunvyj1ph0ipdfafejcvuk0sn` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `model_credits_chk_1` CHECK ((`role` in ('ILLUSTRATOR','RIGGER','MODELER_3D','LOGO','BGM','OTHER')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
