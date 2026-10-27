-- 올린 영상의 길이(초). 목록 카드의 썸네일 오른쪽 아래 배지(03:45)에 쓴다.
-- 모르는 영상(이미 올라간 영상, ffprobe 가 없는 서버)은 null 로 두고 화면에서 배지를 숨긴다.
ALTER TABLE streams ADD COLUMN duration_seconds INT NULL;

-- 데모 영상(V8)의 길이. ffprobe 로 원본 주소를 직접 재서 반올림한 값이다(추측이 아니다).
UPDATE streams SET duration_seconds = 33 WHERE video_url = 'https://download.blender.org/peach/trailer/trailer_1080p.mov';
UPDATE streams SET duration_seconds = 52 WHERE video_url = 'https://download.blender.org/durian/trailer/sintel_trailer-480p.mp4';
UPDATE streams SET duration_seconds = 5 WHERE video_url = 'https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4';
