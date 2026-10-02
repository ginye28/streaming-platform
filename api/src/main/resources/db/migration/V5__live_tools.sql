-- 라이브 도구: 후원(슈퍼챗), 채팅 운영, 다시보기, 방송 예약, 멤버십 전용 방송·채팅.
--
-- 새 종류 값(후원/방송 대상 등)은 ENUM 이 아니라 varchar 로 둔다. 종류를 더 늘릴 때 표를 고치지 않아도 되도록.
-- 이미 있는 표에 칸을 더할 때는 DEFAULT 를 붙여, 지금 있는 행이 그대로 유효하게 한다.

-- ① 결제: 구독 결제와 후원 결제를 한 표에 둔다. 후원은 어느 방송에 보냈는지와 함께 남긴 말을 기록한다.
ALTER TABLE `payments` ADD COLUMN `kind` varchar(20) NOT NULL DEFAULT 'SUBSCRIPTION';
ALTER TABLE `payments` ADD COLUMN `live_stream_id` bigint DEFAULT NULL;
ALTER TABLE `payments` ADD COLUMN `donation_message` varchar(100) DEFAULT NULL;

CREATE INDEX `idx_payments_live` ON `payments` (`live_stream_id`);

-- ② 채팅: 후원 금액(후원 메시지만), 어떤 결제에서 나왔는지, 삭제 표시.
--    지운 메시지는 행을 남기고 deleted 만 켠다. 다시보기와 신고 기록이 어긋나지 않게 하려는 것이다.
ALTER TABLE `chat_messages` ADD COLUMN `donation_amount` int DEFAULT NULL;
ALTER TABLE `chat_messages` ADD COLUMN `donation_payment_id` bigint DEFAULT NULL;
ALTER TABLE `chat_messages` ADD COLUMN `deleted` bit(1) NOT NULL DEFAULT b'0';

-- ③ 방송: 누가 볼 수 있는지, 누가 채팅할 수 있는지, 슬로우 모드, 고정 메시지, 다시보기 여부.
ALTER TABLE `live_streams` ADD COLUMN `audience` varchar(20) NOT NULL DEFAULT 'ALL';
ALTER TABLE `live_streams` ADD COLUMN `chat_audience` varchar(20) NOT NULL DEFAULT 'ALL';
ALTER TABLE `live_streams` ADD COLUMN `slow_mode_seconds` int NOT NULL DEFAULT 0;
ALTER TABLE `live_streams` ADD COLUMN `pinned_message_id` bigint DEFAULT NULL;
ALTER TABLE `live_streams` ADD COLUMN `vod_available` bit(1) NOT NULL DEFAULT b'0';

-- ④ 다음 방송에 쓸 기본값에도 같은 설정을 둔다.
ALTER TABLE `live_settings` ADD COLUMN `audience` varchar(20) NOT NULL DEFAULT 'ALL';
ALTER TABLE `live_settings` ADD COLUMN `chat_audience` varchar(20) NOT NULL DEFAULT 'ALL';
ALTER TABLE `live_settings` ADD COLUMN `slow_mode_seconds` int NOT NULL DEFAULT 0;

-- ⑤ 채널 매니저: 채널 주인이 채팅 운영을 맡기는 사람.
CREATE TABLE `channel_moderators` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `channel_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_channel_moderators` (`channel_id`,`user_id`),
  KEY `FK_channel_moderators_user` (`user_id`),
  CONSTRAINT `FK_channel_moderators_channel` FOREIGN KEY (`channel_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FK_channel_moderators_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ⑥ 채팅 제한: 일시 정지(restricted_until 이 있음) 또는 강퇴(restricted_until 이 NULL). 채널마다 한 사람에 한 행.
CREATE TABLE `chat_restrictions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `channel_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `restricted_until` datetime(6) DEFAULT NULL,
  `reason` varchar(100) DEFAULT NULL,
  `created_by` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_chat_restrictions` (`channel_id`,`user_id`),
  KEY `FK_chat_restrictions_user` (`user_id`),
  CONSTRAINT `FK_chat_restrictions_channel` FOREIGN KEY (`channel_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FK_chat_restrictions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ⑦ 금칙어: 채널마다 채팅에서 막을 단어.
CREATE TABLE `channel_banned_words` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `channel_id` bigint NOT NULL,
  `word` varchar(30) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_channel_banned_words` (`channel_id`,`word`),
  CONSTRAINT `FK_channel_banned_words_channel` FOREIGN KEY (`channel_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ⑧ 방송 예약: 방송 전에 올려 두는 예정. 방송이 시작되면 live_stream_id 로 이어진다.
CREATE TABLE `live_schedules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `title` varchar(100) NOT NULL,
  `description` text,
  `thumbnail_url` varchar(255) DEFAULT NULL,
  `scheduled_at` datetime(6) NOT NULL,
  `audience` varchar(20) NOT NULL DEFAULT 'ALL',
  `chat_audience` varchar(20) NOT NULL DEFAULT 'ALL',
  `status` varchar(20) NOT NULL,
  `live_stream_id` bigint DEFAULT NULL,
  `reminder_sent_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_live_schedules_status` (`status`,`scheduled_at`),
  KEY `idx_live_schedules_user` (`user_id`,`status`),
  CONSTRAINT `FK_live_schedules_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ⑨ 알림 종류를 늘린다. MySQL 은 이 제약이 값을 막고, TiDB 는 CHECK 를 켜 두지 않아 경고만 내고 지나간다.
ALTER TABLE `notifications` DROP CHECK `notifications_chk_1`;

ALTER TABLE `notifications`
  ADD CONSTRAINT `notifications_chk_1`
  CHECK ((`type` in ('LIVE_START','STREAM_COMMENT','COMMENT_REPLY','PAID_EXPIRING',
                     'DONATION','LIVE_SCHEDULED','LIVE_REMINDER')));
