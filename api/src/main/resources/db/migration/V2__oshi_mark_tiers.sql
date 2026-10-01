-- 오시마크 확장: 유료 구독자용 마크, 구독 등급, 시청자가 고르는 마크 표시 여부.

-- 채널이 유료 구독자에게 따로 줄 오시마크. 비워 두면 유료 구독자도 일반 오시마크를 단다.
ALTER TABLE `channel_profiles`
  ADD COLUMN `paid_oshi_mark_url` varchar(255) DEFAULT NULL;

-- 기존 구독은 모두 일반(BASIC) 구독이고, 마크는 기존처럼 보인다(mark_visible = 1).
ALTER TABLE `subscriptions`
  ADD COLUMN `tier` enum('BASIC','PAID') NOT NULL DEFAULT 'BASIC',
  ADD COLUMN `mark_visible` bit(1) NOT NULL DEFAULT b'1';
