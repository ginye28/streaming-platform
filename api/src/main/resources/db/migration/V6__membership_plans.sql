-- 유료 구독 기간 상품(1·3·6·12개월). 결제마다 그 결제로 늘어난 기간(일)을 남긴다.
-- 환불은 이 값만큼 되돌린다. 기존 결제는 모두 한 달(30일)짜리였다.
-- 운영 DB(TiDB)에서도 돌도록 컬럼 추가 한 문장, 값 채우기 한 문장으로 나눈다.

ALTER TABLE payments ADD COLUMN period_days int NULL;

UPDATE payments SET period_days = 30 WHERE kind = 'SUBSCRIPTION' AND period_days IS NULL;
