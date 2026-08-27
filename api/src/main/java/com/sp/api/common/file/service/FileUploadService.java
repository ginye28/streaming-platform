package com.sp.api.common.file.service;

import com.sp.api.common.exception.BadRequestException;
import com.sp.api.common.file.dto.UploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class FileUploadService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "mp4", "mov", "webm", "m4v",
            "jpg", "jpeg", "png", "webp", "gif"
    );

    private final Path uploadRoot;
    private final VideoThumbnailService thumbnailService;

    public FileUploadService(
            @Value("${file.upload-dir}") String uploadDir,
            VideoThumbnailService thumbnailService
    ) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.thumbnailService = thumbnailService;
    }

    public UploadResponse upload(MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("업로드할 파일이 비어 있습니다.");
        }

        String extension = extractExtension(file.getOriginalFilename());

        Files.createDirectories(uploadRoot);

        String baseName = UUID.randomUUID().toString();
        String fileName = baseName + "." + extension;

        Path target = uploadRoot.resolve(fileName).normalize();

        // 저장 경로가 업로드 디렉터리를 벗어나지 않는지 확인한다.
        if (!target.startsWith(uploadRoot)) {
            throw new BadRequestException("잘못된 파일 이름입니다.");
        }

        file.transferTo(target);

        log.debug("파일 업로드 완료: {}", fileName);

        return new UploadResponse("/uploads/" + fileName, thumbnailUrl(target, baseName, extension));
    }

    /** 영상이면 첫 장면을 뽑아 둔다. 못 뽑아도 업로드 자체는 성공이다. */
    private String thumbnailUrl(Path video, String baseName, String extension) {

        if (!thumbnailService.isVideo(extension)) {
            return null;
        }

        String thumbnailName = thumbnailService.generate(video, baseName);

        return thumbnailName == null ? null : "/uploads/" + thumbnailName;
    }

    private String extractExtension(String originalFilename) {

        String extension = StringUtils.getFilenameExtension(originalFilename);

        if (extension == null || extension.isBlank()) {
            throw new BadRequestException("확장자가 없는 파일은 업로드할 수 없습니다.");
        }

        extension = extension.toLowerCase(Locale.ROOT);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException(
                    "지원하지 않는 형식입니다. 허용 확장자: " + String.join(", ", ALLOWED_EXTENSIONS)
            );
        }

        return extension;
    }
}
