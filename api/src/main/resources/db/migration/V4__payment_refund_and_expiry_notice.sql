-- 결제 취소(환불) 기록, 유료 만료 임박 알림.

-- 결제 상태에 CANCELED 를 더한다. ENUM 은 끝에 덧붙여야 기존 값이 그대로 남는다.
ALTER TABLE `payments`
  MODIFY COLUMN `status` enum('DONE','FAILED','READY','CANCELED') NOT NULL;

ALTER TABLE `payments`
  ADD COLUMN `canceled_at` datetime(6) DEFAULT NULL,
  ADD COLUMN `cancel_reason` varchar(200) DEFAULT NULL;

-- 이 만료 시각에 대해 이미 "곧 끝납니다" 알림을 보냈는지. 같은 만료로 두 번 알리지 않는다.
-- 연장해서 만료 시각이 바뀌면 값이 달라지므로 다음 만료 때 다시 알린다.
ALTER TABLE `subscriptions`
  ADD COLUMN `expiry_notified_for` datetime(6) DEFAULT NULL;

-- 알림 종류를 늘린다. MySQL 은 이 제약이 값을 막고, TiDB 는 CHECK 를 켜 두지 않아 경고만 내고 지나간다.
ALTER TABLE `notifications` DROP CHECK `notifications_chk_1`;

ALTER TABLE `notifications`
  ADD CONSTRAINT `notifications_chk_1`
  CHECK ((`type` in ('LIVE_START','STREAM_COMMENT','COMMENT_REPLY','PAID_EXPIRING')));
