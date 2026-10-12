-- 업로드 파일(영상·썸네일·이미지)의 사본을 DB 에 둔다.
-- Render 무료 서버는 디스크가 임시라서 재시작·재배포·잠들었다 깨는 순간 /app/uploads 가 비워진다.
-- 파일은 지금처럼 디스크에서 서빙하되, 없어지면 이 표에서 되살린다.
--
-- 한 줄이 TiDB 의 한 줄 크기 제한(기본 6MB)을 넘지 않도록 파일을 1MB 조각으로 나눠 담는다.
-- stored_files 줄은 조각을 다 넣은 뒤 마지막에 넣는다(이 줄이 있으면 조각이 온전하다는 표시).
-- 테스트(H2)가 이 파일을 그대로 실행하므로 IF NOT EXISTS 를 쓰고 MySQL·H2 공통 문법만 쓴다.

CREATE TABLE IF NOT EXISTS stored_files (
    name        VARCHAR(120) NOT NULL,
    size_bytes  BIGINT       NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (name)
);

CREATE TABLE IF NOT EXISTS stored_file_chunks (
    file_name VARCHAR(120) NOT NULL,
    seq       INT          NOT NULL,
    data      LONGBLOB     NOT NULL,
    PRIMARY KEY (file_name, seq)
);
