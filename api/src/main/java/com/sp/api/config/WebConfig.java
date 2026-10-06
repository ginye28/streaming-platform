package com.sp.api.config;

import com.sp.api.live.ingest.IngestProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
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
    private final String hlsLocation;

    public WebConfig(@Value("${file.upload-dir}") String uploadDir, IngestProperties ingestProperties) {

        // 아직 없는 폴더의 URI 는 끝에 슬래시가 안 붙는다. 정적 자원 위치는 슬래시로 끝나야 한다.
        String hlsUri = ingestProperties.getHlsDir().toUri().toString();
        this.hlsLocation = hlsUri.endsWith("/") ? hlsUri : hlsUri + "/";

        Path path = Paths.get(uploadDir).toAbsolutePath().normalize();

        this.uploadLocation = path.toUri().toString();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadLocation);

        // 브라우저에서 방송한 영상(HLS). 재생목록은 계속 바뀌므로 캐시하지 않는다.
        registry.addResourceHandler("/live-hls/**")
                .addResourceLocations(hlsLocation)
                .setCacheControl(CacheControl.noCache());
    }
}
