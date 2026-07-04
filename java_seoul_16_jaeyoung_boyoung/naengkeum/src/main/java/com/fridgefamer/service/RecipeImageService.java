package com.fridgefamer.service;

import com.fridgefamer.dto.response.recipe.RecipeOwnerRow;
import com.fridgefamer.exception.ApiException;
import com.fridgefamer.exception.ErrorCode;
import com.fridgefamer.mapper.recipe.RecipeMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

/**
 * 레시피 대표 이미지 업로드 — 사용자가 직접 만든 음식 사진을 올린다.
 *
 * <p>흐름: 본인 레시피 검증 → 파일 형식/크기 검증 → 로컬 디스크 저장
 * → recipe.image_url 갱신 → 공개 URL 반환.</p>
 *
 * <p>저장 방식은 로컬 디스크(app.upload.dir). 운영 배포 시 외부 스토리지(S3 등)로
 * 교체 가능하나, PoC 범위에서는 로컬 + 정적 서빙으로 충분.</p>
 */
@Service
public class RecipeImageService {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_TYPE = Set.of("image/jpeg", "image/png", "image/webp");

    private final RecipeMapper recipeMapper;
    private final String uploadDir;
    private final String publicPath;

    public RecipeImageService(RecipeMapper recipeMapper,
                              @Value("${app.upload.dir}") String uploadDir,
                              @Value("${app.upload.public-path}") String publicPath) {
        this.recipeMapper = recipeMapper;
        this.uploadDir = uploadDir;
        this.publicPath = publicPath;
    }

    /**
     * 레시피 사진 업로드. 본인이 등록한 레시피에만 가능.
     * @return 저장된 이미지의 공개 URL (예: /images/recipe/uuid.jpg)
     */
    @Transactional
    public String upload(Long memberId, Long recipeId, MultipartFile file) {
        // 1. 소유 검증 — publish와 동일한 패턴 (공공/타인 레시피엔 업로드 불가)
        RecipeOwnerRow row = recipeMapper.selectRecipeOwner(recipeId);
        if (row == null) {
            throw new ApiException(ErrorCode.NOT_FOUND, "레시피를 찾을 수 없습니다");
        }
        if (row.authorId() == null || !row.authorId().equals(memberId)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "본인이 등록한 레시피에만 사진을 올릴 수 있습니다");
        }

        // 2. 파일 검증
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "업로드할 이미지가 없습니다");
        }
        String ext = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXT.contains(ext) || !ALLOWED_TYPE.contains(file.getContentType())) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "jpg, png, webp 이미지만 업로드할 수 있습니다");
        }

        // 3. 저장 (uploads/recipe/{uuid}.{ext})
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            Path dir = Paths.get(uploadDir, "recipe");
            Files.createDirectories(dir);
            Path target = dir.resolve(filename);
            file.transferTo(target.toAbsolutePath());
        } catch (IOException e) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "이미지 저장에 실패했습니다");
        }

        // 4. image_url 갱신 → 공개 URL
        String publicUrl = publicPath + "/recipe/" + filename;
        recipeMapper.updateImageUrl(recipeId, publicUrl);
        return publicUrl;
    }

    /** 파일명에서 소문자 확장자 추출. 없으면 빈 문자열. */
    private String extensionOf(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        return (dot < 0) ? "" : name.substring(dot + 1).toLowerCase();
    }
}