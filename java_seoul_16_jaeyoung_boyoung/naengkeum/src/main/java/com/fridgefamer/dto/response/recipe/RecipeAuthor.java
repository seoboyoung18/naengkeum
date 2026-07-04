package com.fridgefamer.dto.response.recipe;

/**
 * 레시피 작성자 요약 — 상세 응답에서 "이 레시피를 공개한 사람"을 표시하고
 * 프로필 페이지로 이동(팔로우 동선)하기 위한 최소 정보.
 *
 * <p>공공(시드) 레시피는 author_id가 NULL이라 이 객체 전체가 null로 내려간다.
 * profileImageUrl이 '/'로 시작하면 백엔드 업로드 경로(프론트에서 호스트 prefix를 붙임).</p>
 */
public record RecipeAuthor(
        Long memberId,
        String nickname,
        String profileImageUrl
) {}
