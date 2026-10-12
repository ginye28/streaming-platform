package com.sp.api.common.file.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 업로드한 파일의 사본을 DB 에 둔다.
 *
 * 무료 서버는 디스크가 임시라 재시작하면 업로드 폴더가 비워진다. 그러면 DB 에는 주소가 남아 있는데
 * 파일이 없어서 영상·썸네일·오시마크 이미지가 깨진다. 그래서 올릴 때 DB 에도 담아 두고,
 * 디스크에 없으면 {@link #restore} 로 되살린다(서빙은 계속 디스크에서 하므로 Range 요청도 그대로 된다).
 *
 * 파일을 1MB 조각으로 나눠 조각마다 따로 저장한다. 한 번에 한 조각만 메모리에 올라온다.
 */
@Slf4j
@Component
public class StoredFileBackup {

    static final int CHUNK_SIZE = 1024 * 1024;

    private final JdbcTemplate jdbc;

    public StoredFileBackup(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 파일을 DB 에 담는다. 같은 이름이 이미 있으면 새로 덮어쓴다. 마지막에 넣는 stored_files 줄이 완료 표시다. */
    public void save(String name, Path file) throws IOException {

        long size = Files.size(file);

        jdbc.update("delete from stored_files where name = ?", name);
        jdbc.update("delete from stored_file_chunks where file_name = ?", name);

        try (InputStream in = Files.newInputStream(file)) {

            byte[] buffer = new byte[CHUNK_SIZE];
            int seq = 0;
            int read;

            while ((read = in.readNBytes(buffer, 0, CHUNK_SIZE)) > 0) {
                byte[] data = read == CHUNK_SIZE ? buffer : Arrays.copyOf(buffer, read);

                jdbc.update(
                        "insert into stored_file_chunks (file_name, seq, data) values (?, ?, ?)",
                        name, seq++, data
                );
            }
        }

        jdbc.update(
                "insert into stored_files (name, size_bytes, created_at) values (?, ?, ?)",
                name, size, Timestamp.valueOf(LocalDateTime.now())
        );

        log.debug("업로드 파일을 DB 에 담았습니다: {} ({} bytes)", name, size);
    }

    /** DB 에 담긴 사본이 있으면 target 으로 되살리고 true. 없거나 조각이 모자라면 false. */
    public boolean restore(String name, Path target) throws IOException {

        List<Long> sizes = jdbc.query(
                "select size_bytes from stored_files where name = ?",
                (rs, i) -> rs.getLong(1),
                name
        );

        if (sizes.isEmpty()) {
            return false;
        }

        Files.createDirectories(target.getParent());

        // 다 쓴 다음에 제자리로 옮긴다. 쓰는 도중의 반쪽 파일이 서빙되지 않게 하려는 것이다.
        Path temp = target.resolveSibling(name + ".restoring-" + UUID.randomUUID());

        try {
            try (OutputStream out = Files.newOutputStream(temp)) {
                for (int seq = 0; ; seq++) {
                    List<byte[]> rows = jdbc.query(
                            "select data from stored_file_chunks where file_name = ? and seq = ?",
                            (rs, i) -> rs.getBytes(1),
                            name, seq
                    );

                    if (rows.isEmpty()) {
                        break;
                    }

                    out.write(rows.get(0));
                }
            }

            if (Files.size(temp) != sizes.get(0)) {
                log.warn("DB 에 담긴 파일이 온전하지 않아 되살리지 못했습니다: {}", name);
                return false;
            }

            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);

            log.info("지워진 업로드 파일을 DB 에서 되살렸습니다: {}", name);

            return true;
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
