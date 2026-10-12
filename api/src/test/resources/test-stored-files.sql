-- 테스트(H2)용 업로드 파일 사본 표. 운영 스키마의 원본은 db/migration/V7__stored_files.sql 이다.
-- @DataJpaTest 는 MySQL 모드가 아닌 H2 를 따로 띄워서 DATETIME 을 못 읽으므로 같은 모양을 H2 문법으로 둔다.
-- (마이그레이션과 어긋나지 않는지는 SchemaMigrationTest 가 진짜 MySQL 로 확인한다.)

CREATE TABLE IF NOT EXISTS stored_files (
    name        VARCHAR(120) NOT NULL,
    size_bytes  BIGINT       NOT NULL,
    created_at  TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (name)
);

CREATE TABLE IF NOT EXISTS stored_file_chunks (
    file_name VARCHAR(120) NOT NULL,
    seq       INT          NOT NULL,
    data      BLOB         NOT NULL,
    PRIMARY KEY (file_name, seq)
);
