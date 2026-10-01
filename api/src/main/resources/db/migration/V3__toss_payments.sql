-- 유료 구독 결제: 결제 기록과 유료 구독 만료 시각.

-- 유료 구독이 끝나는 때. 결제로 늘어난다. NULL 이면 기한이 없다(결제가 꺼져 있을 때의 전환).
ALTER TABLE `subscriptions`
  ADD COLUMN `paid_until` datetime(6) DEFAULT NULL;

-- 결제 한 건. 주문(READY)을 만들고, 승인되면 DONE 이 된다. 돈이 오간 기록이라 지우지 않는다.
CREATE TABLE `payments` (
  `amount` int NOT NULL,
  `approved_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `channel_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `failure_code` varchar(100) DEFAULT NULL,
  `failure_message` varchar(500) DEFAULT NULL,
  `method` varchar(50) DEFAULT NULL,
  `order_id` varchar(64) NOT NULL,
  `payment_key` varchar(200) DEFAULT NULL,
  `receipt_url` varchar(500) DEFAULT NULL,
  `status` enum('DONE','FAILED','READY') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_payments_order_id` (`order_id`),
  UNIQUE KEY `UK_payments_payment_key` (`payment_key`),
  KEY `idx_payments_user` (`user_id`,`id`),
  KEY `FK_payments_channel` (`channel_id`),
  CONSTRAINT `FK_payments_channel` FOREIGN KEY (`channel_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FK_payments_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
