package com.sp.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * FileUploadService 가 반환하는 "/uploads/{파일명}" URL 을 실제로 서빙한다.
 * 이 설정이 없으면 업로드는 성공하지만 재생 시 404 가 난다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String uploadLocation;

    public WebConfig(@Value("${file.upload-dir}") String uploadDir) {

        Path path = Paths.get(uploadDir).toAbsolutePath().normalize();

        this.uploadLocation = path.toUri().toString();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadLocation);
    }
}
