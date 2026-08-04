package com.sp.api.common.file.controller;

import com.sp.api.common.file.service.FileUploadService;
import com.sp.api.common.file.dto.UploadResponse;
import com.sp.api.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UploadResponse>> upload(
            @RequestParam("file") MultipartFile file
    ) throws Exception {

        String url = fileUploadService.upload(file);

        return ResponseEntity.ok(
                new ApiResponse<>(true, new UploadResponse(url))
        );
    }
}