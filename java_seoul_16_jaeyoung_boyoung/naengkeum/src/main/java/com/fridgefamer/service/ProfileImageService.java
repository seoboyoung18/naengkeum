package com.fridgefamer.service;

import com.fridgefamer.exception.ApiException;
import com.fridgefamer.exception.ErrorCode;
import com.fridgefamer.mapper.member.MemberMapper;
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
 * 회원 프로필 사진 업로드 — 본인 아바타를 올린다.
 *
 * <p>RecipeImageService와 동일한 방식(로컬 디스크 + /images 정적 서빙). 차이는
 * 소유 검증이 필요 없다는 점 — 항상 "현재 로그인 회원 본인"의 아바타만 갱신한다.</p>
 */
@Service
public class ProfileImageService {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_TYPE = Set.of("image/jpeg", "image/png", "image/webp");

    private final MemberMapper memberMapper;
    private final String uploadDir;
    private final String publicPath;

    public ProfileImageService(MemberMapper memberMapper,
                               @Value("${app.upload.dir}") String uploadDir,
                               @Value("${app.upload.public-path}") String publicPath) {
        this.memberMapper = memberMapper;
        this.uploadDir = uploadDir;
        this.publicPath = publicPath;
    }

    /**
     * 프로필 사진 업로드. 항상 본인(memberId) 아바타에만 적용.
     * @return 저장된 이미지의 공개 URL (예: /images/profile/uuid.jpg)
     */
    @Transactional
    public String upload(Long memberId, MultipartFile file) {
        // 1. 파일 검증
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "업로드할 이미지가 없습니다");
        }
        String ext = extensionOf(file.getOriginalFilename());
        if (!ALLOWED_EXT.contains(ext) || !ALLOWED_TYPE.contains(file.getContentType())) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "jpg, png, webp 이미지만 업로드할 수 있습니다");
        }

        // 2. 저장 (uploads/profile/{uuid}.{ext})
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            Path dir = Paths.get(uploadDir, "profile");
            Files.createDirectories(dir);
            Path target = dir.resolve(filename);
            file.transferTo(target.toAbsolutePath());
        } catch (IOException e) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "이미지 저장에 실패했습니다");
        }

        // 3. profile_image_url 갱신 → 공개 URL 반환
        String publicUrl = publicPath + "/profile/" + filename;
        memberMapper.updateProfileImage(memberId, publicUrl);
        return publicUrl;
    }

    /** 파일명에서 소문자 확장자 추출. 없으면 빈 문자열. */
    private String extensionOf(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        return (dot < 0) ? "" : name.substring(dot + 1).toLowerCase();
    }
}
