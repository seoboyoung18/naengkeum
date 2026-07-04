# 🥬 냉큼(Naeng-Keum) — API 명세서 (현행)

> [!NOTE]
> **이 문서는 실제 컨트롤러 구현 기준으로 동기화된 현행본입니다.** — 기능 엔드포인트 **68개** (+ 헬스/디버그 7개).
> 2026-06-23 갱신: `PATCH /api/recipe/{id}/publish` 공개 시 **재고 자동 차감(F33)** 응답(`consumed[]`) 반영.
> 노션 기획 미러: [요구사항 정의서](https://app.notion.com/p/3888d73381d18183a2bbd778f0e2dafd) · [Use-Case](https://app.notion.com/p/3888d73381d181979bf3dde272cd50d9) · [최종 보고서](https://app.notion.com/p/3888d73381d18168839cd40314466e39)

> **Base URL**: `https://api.fridgefamer.com` (dev: `http://localhost:8080`)
> **인증**: JWT Bearer (`Authorization: Bearer {token}`) · **콘텐츠 타입**: `application/json` (이미지 업로드는 `multipart/form-data`)
> **에러 공통 포맷**: `{ "code": "ERROR_CODE", "message": "설명" }`
> **프레임워크**: Spring Boot 4 · Spring Security(JWT) · MyBatis · MySQL 8

**인증 레벨 범례** — 🌐 공개(인증 불필요) · 🔒 인증(로그인 필요) · 🛡️ 관리자(ROLE_ADMIN)

---

## 1. 인증 (Auth) — `/api/auth`

| 메서드 | 경로 | 인증 | 설명 | 요구사항 |
| --- | --- | --- | --- | --- |
| POST | `/api/auth/register` | 🌐 | 회원가입 (이메일·비밀번호·닉네임) | F10 |
| POST | `/api/auth/login` | 🌐 | 로그인 → JWT 발급 (role claim 포함) | F14 |
| GET | `/api/auth/check-email` | 🌐 | 이메일 중복 확인 | F10 |
| GET | `/api/auth/verify` | 🔒 | 토큰 유효성 검증 | F14 |

**소셜 로그인 (OAuth2, F21)** — Spring Security가 처리하는 표준 경로:
`GET /oauth2/authorization/{google\|kakao}` (시작·인가 리다이렉트) → `GET /login/oauth2/code/{provider}` (콜백·코드 교환) → 회원 매핑(`social_provider`+`social_id`) 후 JWT 발급. 두 경로 모두 🌐 공개.

---

## 2. 회원 (Member) — `/api/member`

| 메서드 | 경로 | 인증 | 설명 | 요구사항 |
| --- | --- | --- | --- | --- |
| GET | `/api/member/me` | 🔒 | 내 프로필 조회 | F11 |
| PUT | `/api/member/me` | 🔒 | 정보 수정 (닉네임·비밀번호·알레르기) | F12 |
| DELETE | `/api/member/me` | 🔒 | 회원 탈퇴 (`is_active=0` 소프트 삭제) | F13 |
| POST | `/api/member/me/avatar` | 🔒 | 프로필 이미지 업로드 (multipart) | F22 |
| GET | `/api/member/me/reviews` | 🔒 | 내가 쓴 리뷰 | F11 |
| GET | `/api/member/me/following` | 🔒 | 팔로잉 목록 | F16 |
| GET | `/api/member/me/followers` | 🔒 | 팔로워 목록 | F16 |
| GET | `/api/member/me/badges` | 🔒 | 내 배지 | F18 |
| GET | `/api/member/{userId}/profile` | 🌐 | 타 유저 공개 프로필 | F16 |
| GET | `/api/member/{userId}/recipes` | 🌐 | 타 유저 공개 레시피 목록 | F24 |

---

## 3. 냉장고 (Fridge) — `/api/fridge`

| 메서드 | 경로 | 인증 | 설명 | 요구사항 |
| --- | --- | --- | --- | --- |
| GET | `/api/fridge` | 🔒 | 재료 목록 | F02 |
| POST | `/api/fridge` | 🔒 | 재료 등록 | F01 |
| PUT | `/api/fridge/{fridgeItemId}` | 🔒 | 재료 수정 | F03 |
| DELETE | `/api/fridge/{fridgeItemId}` | 🔒 | 재료 삭제 | F04 |
| GET | `/api/fridge/dashboard` | 🔒 | 유통기한 임박(D-3) 대시보드 | F17 |
| GET | `/api/fridge/match` | 🔒 | 보유 재료 기반 레시피 매칭(추천 보조) | F05·F19 |

---

## 4. 레시피 (Recipe) — `/api/recipe`

| 메서드 | 경로 | 인증 | 설명 | 요구사항 |
| --- | --- | --- | --- | --- |
| GET | `/api/recipe` | 🌐 | 검색·필터·정렬(인기/시간/재료) | F05 |
| GET | `/api/recipe/autocomplete` | 🌐 | 레시피명 자동완성 | F05 |
| GET | `/api/recipe/popular` | 🌐 | 인기순(리뷰/찜) | F05 |
| GET | `/api/recipe/{recipeId}` | 🌐 | 상세(재료·단계·영양·후기) | F05 |
| POST | `/api/recipe/from-ai` | 🔒 | AI 콘텐츠를 내 레시피로 담기(비공개) | F23 |
| POST | `/api/recipe/from-ai/{aiRecipeId}` | 🔒 | 저장된 AI 레시피를 담기 | F23 |
| PATCH | `/api/recipe/{recipeId}/publish` | 🔒 | **공개 + 재고 자동 차감(F33)** | F24·F33 |
| PATCH | `/api/recipe/{recipeId}/unpublish` | 🔒 | 비공개 전환 (재고 회복 없음) | F24 |
| PATCH | `/api/recipe/{recipeId}/review` | 🔒 | 작성자 후기(`author_review`) 작성/수정 | F06 |
| POST | `/api/recipe/{recipeId}/image` | 🔒 | 레시피 대표 사진 업로드 (multipart) | F25 |
| GET | `/api/recipe/mine` | 🔒 | 내가 담은/작성한 레시피 | F11 |
| DELETE | `/api/recipe/{recipeId}` | 🔒 | 삭제 (본인 또는 관리자) | F24 |

---

## 5. 식재료 사전 (Ingredients) — `/api/ingredients`

| 메서드 | 경로 | 인증 | 설명 | 요구사항 |
| --- | --- | --- | --- | --- |
| GET | `/api/ingredients/autocomplete` | 🌐 | 식재료명 자동완성(사전 150종+) | F32 |
| GET | `/api/ingredients/suggest` | 🌐 | 보관 위치·기한·보관법 제안 | F32·F20 |

---

## 6. 리뷰 (Review) — `/api/review`

| 메서드 | 경로 | 인증 | 설명 | 요구사항 |
| --- | --- | --- | --- | --- |
| GET | `/api/review` | 🌐 | 레시피 리뷰 목록 | F07 |
| POST | `/api/review` | 🔒 | 리뷰 작성 (중복 시 409) | F06 |
| PUT | `/api/review/{reviewId}` | 🔒 | 본인 리뷰 수정 | F08 |
| DELETE | `/api/review/{reviewId}` | 🔒 | 본인 리뷰 삭제 | F09 |

---

## 7. 찜 (Wishlist) — `/api/wishlist`

| 메서드 | 경로 | 인증 | 설명 | 요구사항 |
| --- | --- | --- | --- | --- |
| GET | `/api/wishlist/me` | 🔒 | 내 찜 목록(일반+AI) | F11·F15 |
| POST | `/api/wishlist/{recipeId}` | 🔒 | 일반 레시피 찜 (중복 409) | F15 |
| DELETE | `/api/wishlist/{recipeId}` | 🔒 | 일반 레시피 찜 해제 | F15 |
| POST | `/api/wishlist/ai` | 🔒 | AI 레시피 찜(스냅샷 저장) | F15 |
| GET | `/api/wishlist/ai/{aiRecipeId}` | 🔒 | AI 찜 상세 | F15 |
| DELETE | `/api/wishlist/ai/{aiRecipeId}` | 🔒 | AI 찜 해제 | F15 |

---

## 8. 팔로우 (Follow) — `/api/follow`

| 메서드 | 경로 | 인증 | 설명 | 요구사항 |
| --- | --- | --- | --- | --- |
| POST | `/api/follow/{followeeId}` | 🔒 | 팔로우 (자기 자신 차단) | F16 |
| DELETE | `/api/follow/{followeeId}` | 🔒 | 언팔로우 | F16 |

---

## 9. 챌린지 (Challenge) — `/api/challenge`

| 메서드 | 경로 | 인증 | 설명 | 요구사항 |
| --- | --- | --- | --- | --- |
| GET | `/api/challenge` | 🌐 | 챌린지 목록 | F18 |
| GET | `/api/challenge/my` | 🔒 | 내 챌린지(참여·진행률) | F18 |
| GET | `/api/challenge/stats` | 🌐 | 챌린지 통계 | F18 |
| GET | `/api/challenge/{challengeId}` | 🌐 | 챌린지 상세 | F18 |
| POST | `/api/challenge/{challengeId}/join` | 🔒 | 참여 (중복 409) | F18 |
| DELETE | `/api/challenge/{challengeId}/join` | 🔒 | 참여 취소 | F18 |
| PATCH | `/api/challenge/{challengeId}/progress` | 🔒 | 진행률 갱신(100% 시 배지 자동 지급) | F18 |

---

## 10. 조미료 (Seasoning) — `/api/seasonings`

| 메서드 | 경로 | 인증 | 설명 | 요구사항 |
| --- | --- | --- | --- | --- |
| GET | `/api/seasonings` | 🔒 | 보유 조미료 목록(마스터+보유여부) | F31 |
| PUT | `/api/seasonings` | 🔒 | 보유 조미료 일괄 저장(토글) | F31 |

---

## 11. 신고 (Report) — `/api/report`

| 메서드 | 경로 | 인증 | 설명 | 요구사항 |
| --- | --- | --- | --- | --- |
| POST | `/api/report` | 🔒 | 레시피/리뷰 신고(레시피·리뷰 XOR, 중복 409) | F26 |

---

## 12. 관리자 (Admin) — `/api/admin` 🛡️ 전 구간 ROLE_ADMIN

| 메서드 | 경로 | 설명 | 요구사항 |
| --- | --- | --- | --- |
| GET | `/api/admin/stats` | 대시보드 통계(회원·레시피·리뷰·챌린지·신고 수) | F28 |
| GET | `/api/admin/users` | 회원 검색·목록 | F29 |
| GET | `/api/admin/recipes` | 레시피 목록(모더레이션) | F30 |
| GET | `/api/admin/reviews` | 리뷰 목록(모더레이션) | F30 |
| GET | `/api/admin/reports` | 신고 목록(누적순) | F30 |
| PATCH | `/api/admin/reports/recipe/{recipeId}/resolve` | 레시피 신고 처리(RESOLVED) | F30 |
| PATCH | `/api/admin/reports/review/{reviewId}/resolve` | 리뷰 신고 처리(RESOLVED) | F30 |
| PATCH | `/api/admin/users/{memberId}/active` | 회원 차단/해제 | F29 |
| DELETE | `/api/admin/users/{memberId}` | 회원 삭제 | F29 |
| PATCH | `/api/admin/users/{memberId}/role` | 권한 변경(USER/ADMIN, 관리자 계정 보호) | F27·F29 |

---

## 13. AI (LLM, SSE) — `/api/ai`

| 메서드 | 경로 | 인증 | 설명 | 요구사항 |
| --- | --- | --- | --- | --- |
| POST | `/api/ai/recommend` | 🔒 | 냉장고 기반 맞춤 레시피 추천 (SSE 스트리밍) | F19 |
| POST | `/api/ai/coaching` | 🔒 | 식재료 보관/활용 코칭 (SSE 스트리밍) | F20 |

**추천 요청 예시**
```json
POST /api/ai/recommend
{ "prioritizeExpiry": true, "applyAllergy": true }
```
**SSE 응답(`text/event-stream`)** — `title` → `ingredient` → `step` → `meta` 순으로 토큰 단위 점진 전송, 스트리밍 중 오류는 `error` 이벤트.
사전 차단: 빈 냉장고 `400`, AI 키 미설정/외부 장애 `503`(연결 전 동기 차단). 하이브리드 전략 — DB 레시피 우선 매칭(보유≥2, 부족 재료≤기준) → 후보 빈약 시에만 LLM(JSON 모드) 호출.

---

## 14. 핵심 플로우 — AI 레시피 담기 → 공개 → 재고 차감 (F23·F24·F33)

```
[AI 추천(SSE)] → POST /api/recipe/from-ai            : 내 보관함에 담기 (is_public=FALSE)
              → POST /api/recipe/{id}/image          : (선택) 대표 사진 업로드
              → PATCH /api/recipe/{id}/publish       : 공개 + 사용 재료 냉장고 차감(최초 1회)
              → PATCH /api/recipe/{id}/unpublish      : 비공개 전환 (재고 회복 없음)
```

**공개 응답 — `PATCH /api/recipe/{recipeId}/publish`**
```json
{
  "recipeId": 123,
  "isPublic": true,
  "consumed": [
    { "name": "양파",   "used": 1,  "unit": "개", "removed": false },
    { "name": "대파",   "used": 50, "unit": "g",  "removed": true  }
  ]
}
```
- `consumed[]` — 이번 공개에서 실제 차감된 재료. `used` 차감량, `unit` 단위, `removed` 차감 후 수량 0이 되어 삭제됐는지.
- 차감 규칙: 이름 정확 일치 + 단위 호환(동일 단위 또는 g↔kg·ml↔L)인 **정량** 재료만. 조미료(조미료 마스터 보유)·비정량("적당량")·단위 불일치(개↔g)·미보유는 **스킵**(응답에서 제외).
- `recipe.ingredients_consumed` 플래그로 **최초 공개 1회만** 차감 → 공개→비공개→재공개 반복 시 중복 차감 방지(되돌릴 수 없는 차감이므로 정책상 재고 회복 없음). 이미 차감된 레시피의 재공개 응답은 `consumed: []`.

---

## 15. 공통 에러 코드

| 상황 | HTTP | 처리 |
| --- | --- | --- |
| 빈 냉장고로 AI 추천 | 400 | 연결 전 동기 차단 |
| 검증 실패(필수값·형식) | 400 | GlobalExceptionHandler 일관 포맷 |
| JWT 만료/무효 | 401 | 로그인 리다이렉트(원래 경로 보존) |
| 권한 없음(비관리자의 `/api/admin/**` 등) | 403 | 일관 응답 |
| 리소스 없음(GET `/{id}`) | 404 | 일괄 명시 |
| 중복(이메일·리뷰·찜·신고·챌린지·소셜) | 409 | 중복 안내(UNIQUE 트리거) |
| AI 키 미설정·외부 LLM 장애 | 503 | 연결 전 차단 / 스트리밍 중은 `error` 이벤트 |
| 잘못된 값(음수 수량·평점·XOR) | — | DB CHECK 제약으로 원천 차단 |

---

## 부록. 헬스 · 디버그 엔드포인트 (공개 API 아님)

| 메서드 | 경로 | 인증 | 용도 |
| --- | --- | --- | --- |
| GET | `/health`, `/actuator/health` | 🌐 | 컨테이너/모니터링 헬스체크 |
| GET | `/api/test/token` | 🌐 | 임시 JWT 발급(개발) |
| GET | `/api/test/me` | 🔒 | 토큰 클레임 확인(개발) |
| GET | `/api/test/error/api` | 🌐 | ApiException 처리 테스트 |
| POST | `/api/test/error/validation` | 🌐 | 검증(400) 처리 테스트 |
| GET | `/api/test/error/path/{id}` | 🌐 | 타입 미스매치 처리 테스트 |
| GET | `/api/test/error/unexpected` | 🌐 | 미처리 예외(500) 테스트 |

> 이 부록 항목은 GlobalExceptionHandler/배포 점검용 스캐폴딩으로, 공개 API 표면(68개)에는 포함하지 않습니다.