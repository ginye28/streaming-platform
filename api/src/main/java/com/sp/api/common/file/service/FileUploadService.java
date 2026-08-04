package com.sp.api.common.file.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileUploadService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    public String upload(MultipartFile file) throws IOException {

        Path path = Paths.get(uploadDir);

        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }

        String extension =
                StringUtils.getFilenameExtension(file.getOriginalFilename());

        String fileName =
                UUID.randomUUID() + "." + extension;

        Path target = path.resolve(fileName);

        System.out.println("저장 경로: " + target.toAbsolutePath());

        file.transferTo(target);

        return "/uploads/" + fileName;
    }
}