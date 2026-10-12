package com.sp.api.common.file.service;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * /uploads/{이름} 을 요청받았는데 디스크에 파일이 없으면 DB 사본에서 되살린 뒤 평소처럼 서빙하게 한다.
 * 무료 서버가 재시작해서 업로드 폴더가 비워져도 영상·이미지가 깨지지 않게 하는 장치다.
 */
@Slf4j
@Component
public class UploadRestoreFilter extends OncePerRequestFilter {

    private static final String PREFIX = "/uploads/";

    /** 업로드 서비스가 만드는 이름(uuid + 확장자)만 통과시킨다. 경로 이동 문자는 여기서 걸러진다. */
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,119}");

    private final Path uploadRoot;
    private final StoredFileBackup backup;
    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    public UploadRestoreFilter(@Value("${file.upload-dir}") String uploadDir, StoredFileBackup backup) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.backup = backup;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();

        return !("GET".equals(method) || "HEAD".equals(method))
                || !request.getRequestURI().startsWith(PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String name = request.getRequestURI().substring(PREFIX.length());

        if (NAME.matcher(name).matches()) {
            restoreIfMissing(name);
        }

        chain.doFilter(request, response);
    }

    private void restoreIfMissing(String name) {

        Path target = uploadRoot.resolve(name).normalize();

        if (!target.startsWith(uploadRoot) || Files.exists(target)) {
            return;
        }

        // 같은 파일을 동시에 여러 번 되살리지 않게 이름별로 한 번에 하나만.
        synchronized (locks.computeIfAbsent(name, key -> new Object())) {
            try {
                if (!Files.exists(target)) {
                    backup.restore(name, target);
                }
            } catch (Exception e) {
                // 되살리지 못하면 평소처럼 404 로 이어진다.
                log.warn("업로드 파일을 되살리지 못했습니다: {}", name, e);
            } finally {
                locks.remove(name);
            }
        }
    }
}
