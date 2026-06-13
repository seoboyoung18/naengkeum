package com.fridgefamer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * 업로드된 레시피 이미지를 정적으로 서빙한다.
 *
 * <p>application.yml의 spring.web.resources.add-mappings=false로 자동 매핑을 꺼두었으므로,
 * 업로드 디렉터리({@code app.upload.dir})를 공개 경로({@code app.upload.public-path},
 * 예: /images)로 명시적으로 매핑한다. 예) /images/recipe/uuid.jpg →
 * file:./uploads/recipe/uuid.jpg</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String uploadDir;
    private final String publicPath;

    public WebConfig(@Value("${app.upload.dir}") String uploadDir,
                     @Value("${app.upload.public-path}") String publicPath) {
        this.uploadDir = uploadDir;
        this.publicPath = publicPath;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 절대경로 + file: 접두사. 끝에 '/' 필수 (디렉터리 매핑).
        String location = "file:" + Paths.get(uploadDir).toAbsolutePath() + "/";
        registry.addResourceHandler(publicPath + "/**")
                .addResourceLocations(location);
    }
}