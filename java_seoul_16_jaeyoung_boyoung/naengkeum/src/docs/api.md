# 🥬 냉장고 파머 — API 명세서

> **연관 문서**: 🥬 냉장고 파머 — User Flow 기획서
> **Base URL**: `https://api.fridgefamer.com` (dev: `http://localhost:8080`)
> **인증 방식**: JWT Bearer Token (`Authorization: Bearer {token}`)
> **콘텐츠 타입**: `application/json`
> **에러 공통 포맷**: `{ "code": "ERROR_CODE", "message": "설명" }`
> **프레임워크**: Spring Boot 4 · MyBatis · MySQL 8

---

## 1. 인증 (Auth)

### POST `/api/auth/login` — 로그인 `F14`

| 항목 | 내용 | 필수 | 비고 |
| --- | --- | --- | --- |
| 인증 | 불필요 | - | - |
| Request Body | `email` (string) | ✅ | - |
| Request Body | `password` (string) | ✅ | - |
| Request Body | `rememberMe` (boolean) | ❌ | true시 토큰 30일 유지 |

**Response 200**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "nickname": "냉파왕",
  "memberId": 1
}
```

**Error** `401` 이메일 또는 비밀번호 불일치 · `403` 탈퇴(is_active=0) 회원

---

### POST `/api/auth/register` — 회원가입 `F10`

| 항목 | 내용 | 필수 | 비고 |
| --- | --- | --- | --- |
| 인증 | 불필요 | - | - |
| Request Body | `email` (string) | ✅ | - |
| Request Body | `password` (string) | ✅ | 영문+숫자+특수문자 8자 이상 |
| Request Body | `nickname` (string) | ✅ | 2~10자 |
| Request Body | `allergies` (string[]) | ❌ | - |
| Request Body | `marketingAgree` (boolean) | ❌ | - |

**Response 201** `{ "memberId": 1 }`

**Error** `409` 이메일/닉네임 중복 · `400` 유효성 오류

---

### GET `/api/auth/check-email` — 이메일 중복 확인 `F10`

**Query** `?email=test@email.com`

**Response 200** `{ "available": true }` 또는 `{ "available": false }` · **Error** `400` 유효성 오류

> 💡 *2026-05-29 변경*: 중복 시에도 200 응답 (RESTful). 4xx는 진짜 에러일 때만.

---

### GET `/api/auth/verify` — 토큰 유효성 확인 (Splash)

**Header** `Authorization: Bearer {token}` · **Response 200** `{ "valid": true }` · `401`

---

## 2. 회원 (Member)

### GET `/api/member/me` — 마이페이지 조회 `F11`

**인증** 필요

**Response 200**

```json
{
  "memberId": 1,
  "nickname": "냉파왕",
  "email": "te***@email.com",
  "allergies": ["계란","우유"],
  "stats": {
    "fridgeCount": 13,
    "reviewCount": 8,
    "wishlistCount": 21,
    "followerCount": 24,
    "followingCount": 18
  }
}
```

---

### PUT `/api/member/me` — 회원정보 수정 `F12`

**인증** 필요

| 필드 | 타입 | 필수 | 비고 |
| --- | --- | --- | --- |
| nickname | string | ❌ | - |
| currentPassword | string | 비밀번호 변경 시 ✅ | - |
| newPassword | string | ❌ | - |
| allergies | string[] | ❌ | - |

**Response 200** 수정된 회원 정보 · **Error** `401` 현재 비밀번호 불일치

---

### DELETE `/api/member/me` — 회원 탈퇴 `F13`

**인증** 필요 · **Request Body** `{ "password": "..." }`

**Response 200** `{ "message": "탈퇴 완료" }` · **Error** `401` 비밀번호 불일치

---

### GET `/api/member/me/reviews` — 마이 리뷰 목록 `F11`

**인증** 필요 · **Query** `?page=0&size=10`

**Response 200** `{ "content": [...], "totalElements": 8 }`

---

### GET `/api/member/me/following` · `/me/followers` — 팔로우 목록 `F16`

**인증** 필요 · **Response 200** `[{ "memberId", "nickname", "reviewCount", "isFollowing" }]`

---

### GET `/api/member/me/badges` — 배지 목록 `F18`

**인증** 필요 · **Response 200** `[{ "badgeId", "name", "iconUrl", "earnedAt" }]`

---

### GET `/api/member/{userId}/profile` — 타 유저 프로필 `F16`

**인증** 불필요 · **Response 200** `{ "memberId", "nickname", "reviewCount", "followerCount", "isFollowing" }`

---

## 3. 냉장고 (Fridge)

### GET `/api/fridge` — 냉장고 재료 조회 `F02`

**인증** 필요

| Query Param | 설명 | 기본값 |
| --- | --- | --- |
| storageType | FRIDGE \| FREEZER \| ROOM_TEMP \| ALL | ALL |
| sort | EXPIRY_ASC \| CREATED_DESC \| NAME_ASC | EXPIRY_ASC |

**Response 200**

```json
{
  "items": [
    {
      "fridgeItemId": 1,
      "name": "계란",
      "qty": 6,
      "unit": "개",
      "storageType": "FRIDGE",
      "expiryDate": "2026-05-16",
      "dDay": -1,
      "memo": null
    }
  ],
  "summary": { "fridgeCount": 8, "freezerCount": 3, "roomTempCount": 2 }
}
```

---

### POST `/api/fridge` — 재료 등록 `F01`

**인증** 필요

| 필드 | 타입 | 필수 |
| --- | --- | --- |
| name | string | ✅ |
| qty | number | ✅ |
| unit | string | ✅ |
| storageType | FRIDGE \| FREEZER \| ROOM_TEMP | ✅ |
| expiryDate | date (YYYY-MM-DD) | ✅ |
| memo | string | ❌ |

**Response 201** 등록된 재료 정보 · **Error** `400` 유효성 오류

---

### PUT `/api/fridge/{fridgeItemId}` — 재료 수정 `F03`

**인증** 필요 · **Request Body** 등록과 동일

**Response 200** 수정된 재료 · **Error** `403` 권한 없음 · `404` 재료 없음

---

### DELETE `/api/fridge/{fridgeItemId}` — 재료 삭제 `F04`

**인증** 필요 · **Response 200** `{ "message": "삭제 완료" }` · **Error** `403` `404`

---

### GET `/api/fridge/dashboard` — 냉장고 대시보드 `F17`

**인증** 필요 · 냉장고 요약 + D-3 이내 임박 목록 한번에 응답

---

### GET `/api/fridge/match` — 레시피 재료 매칭 `F02`

**인증** 필요 · **Query** `?recipeId=1`

**Response 200** `{ "ownedIngredientIds": [1, 3, 5] }` — 보유 재료명 배열 반환

---

## 4. 레시피 (Recipe)

### GET `/api/recipe` — 레시피 검색 `F05`

**인증** 불필요

| Query Param | 설명 | 기본값 |
| --- | --- | --- |
| keyword | 레시피명 또는 재료명 | - |
| sort | LATEST \| POPULAR \| RATING \| COOK_TIME | LATEST |
| ingredients | 콤마 구분 재료명 목록 | - |
| maxCookTime | 최대 조리시간(분) | - |
| difficulty | EASY \| MEDIUM \| HARD (콤마 다중) | - |
| page | 0부터 시작 | 0 |
| size | 페이지당 개수 | 12 |

**Response 200**

```json
{
  "content": [
    {
      "recipeId": 1,
      "title": "계란볶음",
      "thumbnailUrl": "https://...",
      "cookTime": 20,
      "difficulty": "EASY",
      "avgRating": 4.3,
      "reviewCount": 12,
      "isWishlisted": false,
      "mainIngredients": ["계란","당면","양파"]
    }
  ],
  "totalElements": 120,
  "totalPages": 10
}
```

---

### GET `/api/recipe/autocomplete` — 자동완성 `F05`

**Query** `?keyword=계란` (3자 이상)

**Response 200** `[{ "recipeId": 1, "title": "계란볶음" }]` 최대 5개

---

### GET `/api/recipe/popular` — 인기 레시피 `F05`

**Query** `?limit=8` · **Response 200** 인기순 Top N 레시피 목록

---

### GET `/api/recipe/{recipeId}` — 레시피 상세 `F05`

**인증** 불필요

**Response 200**

```json
{
  "recipeId": 1,
  "title": "계란볶음",
  "description": "...",
  "cookTime": 20,
  "difficulty": "EASY",
  "servings": 2,
  "thumbnailUrl": "https://...",
  "avgRating": 4.3,
  "reviewCount": 12,
  "isWishlisted": false,
  "author": { "memberId": 5, "nickname": "작성자" },
  "tags": ["반찬","간단요리"],
  "ingredients": [
    { "name": "계란", "qty": 2, "unit": "개", "sortOrder": 1 }
  ],
  "steps": [
    { "stepNumber": 1, "description": "팬에 기름을 두릅니다.", "imageUrl": null }
  ]
}
```

**Error** `404` 레시피 없음

---

## 5. 리뷰 (Review)

### GET `/api/review` — 리뷰 목록 조회 `F07`

**인증** 불필요 · **Query** `?recipeId=1&page=0&size=10`

**Response 200**

```json
{
  "content": [
    {
      "reviewId": 10,
      "memberId": 3,
      "nickname": "닉네임",
      "rating": 4,
      "content": "맛있어요!",
      "isOwner": false,
      "createdAt": "2026-05-14T10:30:00"
    }
  ],
  "totalElements": 12,
  "ratingStats": { "avg": 4.3, "dist": {"5":9,"4":2,"3":1} }
}
```

---

### POST `/api/review` — 리뷰 작성 `F06`

**인증** 필요 · **Request Body** `{ "recipeId": 1, "rating": 5, "content": "맛있어요!" }`

**Response 201** 생성된 리뷰 · **Error** `409` 이미 작성 · `400` 유효성 오류

---

### PUT `/api/review/{reviewId}` — 리뷰 수정 `F08`

**인증** 필요 · **Request Body** `{ "rating": 4, "content": "수정 내용" }`

**Response 200** 수정된 리뷰 · **Error** `403` 본인이 아님

---

### DELETE `/api/review/{reviewId}` — 리뷰 삭제 `F09`

**인증** 필요 · **Response 200** `{ "message": "삭제 완료" }` · **Error** `403` `404`

---

## 6. 찜 (Wishlist)

### GET `/api/wishlist/me` — 찜 목록 조회 `F15`

**인증** 필요 · **Query** `?page=0&size=12&limit=3`

**Response 200** `{ "content": [...], "totalElements": 21 }`

---

### POST `/api/wishlist/{recipeId}` — 찜 등록 `F15`

**인증** 필요 · **Response 200** `{ "wishlisted": true }` · **Error** `409` 이미 찜함

---

### DELETE `/api/wishlist/{recipeId}` — 찜 해제 `F15`

**인증** 필요 · **Response 200** `{ "wishlisted": false }`

---

### POST `/api/wishlist/ai` — AI 레시피 찜 저장 `F19`

**인증** 필요

**Request Body** `{ "title", "summary", "ingredientsJson": [...], "stepsJson": [...], "cookTime" }`

**Response 201** `{ "aiRecipeId": 5 }`

---

### DELETE `/api/wishlist/ai/{aiRecipeId}` — AI 레시피 찜 해제 `F19` ✨ *신규 추가*

**인증** 필요 · **Response 200** `{ "wishlisted": false }` · **Error** `401` 인증 실패 · `403` 본인 아님 · `404` 찜 기록 없음

> 💡 *2026-05-29 신규*: 원본 명세에 누락되어 있던 AI 찜 해제 엔드포인트. 잘못 찜한 경우 영원히 해제 불가하던 Critical 이슈 해결.

---

## 7. 팔로우 (Follow)

### POST `/api/follow/{userId}` — 팔로우 `F16`

**인증** 필요 · **Response 200** `{ "following": true }` · **Error** `400` 자기 자신 · `409` 이미 팔로우 중 · `404` 사용자 없음

---

### DELETE `/api/follow/{userId}` — 언팔로우 `F16`

**인증** 필요 · **Response 200** `{ "following": false }`

---

## 8. 코스 (Challenge)

### GET `/api/challenge` — 코스 목록 `F18`

**인증** 불필요 · **Query** `?status=active|ended`

**Response 200**

```json
[
  {
    "challengeId": 1,
    "title": "식비 0원 코스",
    "description": "...",
    "startDate": "2026-05-13",
    "endDate": "2026-05-19",
    "dDay": 3,
    "participantCount": 142,
    "badge": { "badgeId": 1, "name": "식비 0원", "iconUrl": "..." },
    "myStatus": "JOINED",
    "myProgress": 40
  }
]
```

---

### GET `/api/challenge/my` — 내가 참여 중인 코스 `F18`

**인증** 필요 · **Response 200** 코스 목록 + 달성률 포함

---

### GET `/api/challenge/{challengeId}` — 코스 상세 `F18`

**인증** 불필요 · **Response 200** 코스 상세 + 내 참여 여부 + 진행률

---

### POST `/api/challenge/{challengeId}/join` — 참여 `F18`

**인증** 필요 · **Response 201** `{ "joined": true }` · **Error** `409` 이미 참여 중

---

### DELETE `/api/challenge/{challengeId}/join` — 언조인 `F18`

**인증** 필요 · **Response 200** `{ "joined": false }`

---

### GET `/api/challenge/stats` — 활성 사용자 통계 `F18`

**인증** 불필요 · **Response 200** `{ "activeParticipants": 240 }`

---

## 9. AI (AI 추천 / 코칭)

### POST `/api/ai/recommend` — AI 레시피 추천 `F19`

**인증** 필요 · **Content-Type** `text/event-stream` (SSE)

**Request Body**

```json
{
  "prioritizeExpiry": true,
  "useAllFridge": false,
  "applyAllergy": true
}
```

**SSE 응답 포맷**

```
data: {"type":"title","value":"계란볶음"}
data: {"type":"summary","value":"평소에는..."}
data: {"type":"ingredient","value":{"name":"계란","qty":2,"unit":"개","owned":true}}
data: {"type":"step","value":{"stepNumber":1,"description":"팬에 기름을..."}}
data: {"type":"meta","value":{"cookTime":20,"difficulty":"EASY","servings":2}}
data: {"type":"error","value":"LLM API timeout","retryable":true}
data: {"type":"done"}
```

**Error** `400` 냉장고 재료 없음 · `503` LLM API 오류 (연결 전) · SSE `type:error` (스트리밍 도중)

---

### POST `/api/ai/coaching` — AI 식재료 코칭 `F20`

**인증** 필요 · **Content-Type** `text/event-stream` (SSE)

**Request Body** `{ "fridgeItemId": 1, "ingredientName": "계란" }`

**SSE 응답**

```
data: {"type":"storage","value":"계란은 실온 보관시..."}
data: {"type":"combo","value":"편의점 장조림과의 꿀조합은..."}
data: {"type":"error","value":"LLM API timeout","retryable":true}
data: {"type":"done"}
```

---

## 10. 공통 에러 코드

| HTTP 코드 | 에러 코드 | 상황 |
| --- | --- | --- |
| 400 | VALIDATION_ERROR | 입력값 유효성 실패 |
| 400 | BAD_REQUEST | 잘못된 요청 (자기 팔로우, 빈 냉장고 AI 호출 등) |
| 401 | UNAUTHORIZED | 토큰 없음 / 형식 오류 |
| 401 | TOKEN_EXPIRED | 토큰 만료 (프론트 자동 처리) |
| 401 | INVALID_TOKEN | 토큰 위조 (서명 불일치) |
| 403 | FORBIDDEN | 권한 없음 (타인 리소스) |
| 404 | NOT_FOUND | 리소스 없음 |
| 409 | CONFLICT | 이미 존재 (중복 이메일, 중복 리뷰 등) |
| 500 | INTERNAL_ERROR | 서버 내부 오류 |
| 503 | AI_SERVICE_ERROR | LLM API 호출 실패 |

---

## 11. Spring Boot Controller 매핑 요약

```
com.fridgefamer
├── controller/
│   ├── AuthController.java          # /api/auth/**
│   ├── MemberController.java        # /api/member/**
│   ├── FridgeController.java        # /api/fridge/**
│   ├── RecipeController.java        # /api/recipe/**
│   ├── ReviewController.java        # /api/review/**
│   ├── WishlistController.java      # /api/wishlist/**
│   ├── FollowController.java        # /api/follow/**
│   ├── ChallengeController.java     # /api/challenge/**
│   └── AiController.java            # /api/ai/**
├── service/
│   └── (Controller마다 대응)
├── mapper/
│   └── (MyBatis XML Mapper 매핑)
├── dto/
│   ├── request/   # @RequestBody DTO
│   └── response/  # @ResponseBody DTO
├── config/
│   ├── SecurityConfig.java          # Spring Security + JWT 필터
│   └── JwtProvider.java             # JWT 생성/검증
└── exception/
    └── GlobalExceptionHandler.java  # @RestControllerAdvice
```

---

> ✅ **API 명세서 완료** — 인증(4) + 회원(8) + 냉장고(6) + 레시피(4) + 리뷰(4) + 찜(5) + 팔로우(2) + 코스(6) + AI(2) — **총 41개 엔드포인트** ✨ *2026-05-29 1주차 검토 후 정정*
> **다음 단계 권장**: Spring Boot 개발 (Controller → Service → Mapper 순서)

---

# 🆕 2026-05-29 1주차 진행 중 변경사항

> WBS-⑤ API 명세서 검토 작업 중 결정된 API 관련 변경 사항입니다.
> 원본 명세서 본문의 해당 부분을 이 내용으로 갱신해주세요.

## 🔢 엔드포인트 수 정정

| 항목 | Before | After |
| --- | --- | --- |
| 전체 엔드포인트 수 | **36개** (원본 표기) | **41개** (실제 카운트) |

**그룹별 정정**:

| 그룹 | 원본 카운트 | 실제 카운트 | 차이 |
| --- | --- | --- | --- |
| 인증 (Auth) | 2 | **4** | login, register, check-email, verify |
| 회원 (Member) | 6 | **8** | me 3 + me/reviews + following + followers + badges + profile |
| 냉장고 (Fridge) | 6 | 6 | 동일 |
| 레시피 (Recipe) | 4 | 4 | 동일 |
| 리뷰 (Review) | 4 | 4 | 동일 |
| **찜 (Wishlist)** | 4 | **5** | **AI 찜 해제 신규 추가** |
| 팔로우 (Follow) | 2 | 2 | 동일 |
| 코스 (Challenge) | 6 | 6 | 동일 |
| AI | 2 | 2 | 동일 |

## 🆕 신규 엔드포인트 추가

### DELETE /api/wishlist/ai/{aiRecipeId}

```
DELETE /api/wishlist/ai/{aiRecipeId}
Authorization: Bearer {token}

Response 200
{ "wishlisted": false }

Error
- 401 UNAUTHORIZED  : 인증 실패
- 403 FORBIDDEN     : 타인의 찜 해제 시도
- 404 NOT_FOUND     : 찜 기록 없음
```

**추가 이유**: 원본 명세에 AI 레시피 찜 **등록**(POST)은 있으나 **해제**(DELETE) 누락. 사용자가 잘못 찜할 경우 영원히 해제 불가 → Critical 이슈.

## 🔄 응답 코드 변경

### GET /api/auth/check-email — 응답 코드 수정

| 케이스 | Before | After |
| --- | --- | --- |
| 사용 가능 | 200 + `{"available": true}` | 200 + `{"available": true}` |
| 사용 불가 (중복) | **409 (에러로 처리됨)** | **200 + `{"available": false}`** |
| 유효성 오류 | - | 400 + `{"code":"VALIDATION_ERROR"}` |

**변경 이유**: `409 Conflict`는 요청 실패 의미 → 단순 조회 응답으로 부적절. Axios 등 HTTP 클라이언트가 4xx를 자동으로 catch 블록으로 보내는 비효율 발생. 항상 200으로 응답하고 boolean으로 구분하는 것이 RESTful.

## ➕ 에러 코드 세분화

### JWT 401 응답 세분화

| Before | After |
| --- | --- |
| `401 UNAUTHORIZED` (단일) | `UNAUTHORIZED`, `TOKEN_EXPIRED`, `INVALID_TOKEN` (3종) |

**세부 명세**:

```json
// 토큰 부재 또는 형식 오류
{ "code": "UNAUTHORIZED", "message": "인증 정보가 필요합니다" }

// 토큰 만료 (프론트가 자동 갱신 또는 로그인 페이지 리다이렉트)
{ "code": "TOKEN_EXPIRED", "message": "세션이 만료되었습니다" }

// 토큰 위조 (서명 불일치)
{ "code": "INVALID_TOKEN", "message": "유효하지 않은 토큰입니다" }
```

**변경 이유**: 프론트가 토큰 만료 / 위조 / 부재를 구분 가능 → 자동 갱신 vs 로그인 페이지 리다이렉트 분기 가능.

### 최종 9종 공통 에러 코드

| HTTP | code | 발생 상황 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 입력값 유효성 실패 (rating 6, qty 음수 등) |
| 400 | `BAD_REQUEST` | 잘못된 요청 (자기 팔로우, 빈 냉장고 AI 호출 등) |
| 401 | `UNAUTHORIZED` | 토큰 없음 / 형식 오류 |
| 401 | `TOKEN_EXPIRED` | 토큰 만료 (프론트가 자동 처리) |
| 401 | `INVALID_TOKEN` | 토큰 위조 (서명 불일치) |
| 403 | `FORBIDDEN` | 권한 없음 (타인 리소스 수정 시도) |
| 404 | `NOT_FOUND` | 리소스 없음 |
| 409 | `CONFLICT` | 이미 존재 (중복 이메일/닉네임/리뷰/찜/팔로우/챌린지 참여) |
| 500 | `INTERNAL_ERROR` | 서버 내부 오류 |
| 503 | `AI_SERVICE_ERROR` | LLM API 호출 실패 |

> ℹ️ 원문 본문 §10 표에는 `INVALID_TOKEN`이 포함되어 10행으로 표기되어 있습니다. "9종"은 `INVALID_TOKEN`을 `UNAUTHORIZED` 계열로 묶은 카운트 기준이며, 실제 enum은 위 표대로 등록합니다.

## 📡 SSE 이벤트 추가

### POST /api/ai/recommend, POST /api/ai/coaching — error 이벤트 신규 추가

```
data: {"type":"title","value":"계란볶음"}
data: {"type":"ingredient","value":{...}}
data: {"type":"step","value":{...}}
data: {"type":"meta","value":{...}}
data: {"type":"error","value":"LLM API timeout","retryable":true}    ← 신규 추가
data: {"type":"done"}
```

### HTTP 503 vs SSE error 구분

| 시점 | 응답 | 의미 |
| --- | --- | --- |
| 연결 시작 전 | `HTTP 503 AI_SERVICE_ERROR` | LLM API 자체 죽음 → 재시도 가능 |
| 스트리밍 도중 | `data: {"type":"error","retryable":true}` | 도중 끊김 → 부분 데이터 활용 가능 |
| 정상 종료 | `data: {"type":"done"}` | 완료 |

**변경 이유**: 원본 명세는 HTTP 503만 정의 → 스트리밍 도중 에러를 클라이언트가 어떻게 받는지 불명확.

## 📐 페이징 응답 표준화

### Before (들쭉날쭉)

- 페이징 리뷰: `{ "content": [...], "totalElements": 12, "ratingStats": {...} }`
- 페이징 찜: `{ "content": [...], "totalElements": 21 }`
- 단순 팔로워 목록: `[ {...}, {...} ]` (배열만)
- 챌린지 목록: `[ {...} ]` (배열만)

### After (표준화)

```json
{
  "content": [...],
  "page": 0,
  "size": 12,
  "totalElements": 120,
  "totalPages": 10
}
```

**메타데이터 추가가 필요한 경우 같은 레벨에 추가**:

```json
{
  "content": [...],
  "page": 0,
  "size": 10,
  "totalElements": 12,
  "totalPages": 2,
  "ratingStats": { "avg": 4.3, "dist": {...} }
}
```

단일 객체나 작은 배열은 그대로 (`{}` 또는 `[]`).

## ✏️ 누락된 응답 코드 명시

### POST /api/follow/{userId} — 409 응답 추가

```
Response 200 { "following": true }
Error 400 CONFLICT_SELF_FOLLOW   자기 자신 팔로우 시도
Error 409 CONFLICT               이미 팔로우 중       ← 신규 명시
Error 404 NOT_FOUND              존재하지 않는 사용자  ← 신규 명시
```

**변경 이유**: `uq_follow (follower_id, followee_id)` UNIQUE 제약이 DB에 있는데 API 응답 명세에 누락되어 있었음.

### 모든 GET /{id} 엔드포인트 — 404 일괄 명시

영향받는 엔드포인트:

- GET /api/member/{userId}/profile
- GET /api/challenge/{challengeId}
- (기타 GET 단일 조회 엔드포인트 전체)

```
Error 404 NOT_FOUND { "code": "NOT_FOUND", "message": "리소스를 찾을 수 없습니다" }
```

## ⛔ 의식적 미적용 (결정 사항 기록)

다음 항목들은 검토했으나 **현 스코프에서는 의도적으로 적용하지 않음**:

| 항목 | 미적용 이유 |
| --- | --- |
| POST /api/auth/logout 엔드포인트 추가 | JWT stateless 특성 + 4주 PoC 스코프. **클라이언트(Pinia store)에서 토큰 삭제로 단독 처리** |
| API 버전 prefix (`/api/v1/...`) | 4주 PoC에서 불필요. 추후 v2 출시 시 함께 도입 |
| /me/following + /me/followers 통합 | 응답 의미가 미세하게 달라질 가능성. 현 명세 유지 |
| POST /wishlist 응답을 201로 변경 | 토글 동작 의도로 200 유지. README에 명시 예정 |
| POST /follow 응답을 201로 변경 | 토글 동작 의도로 200 유지. README에 명시 예정 |

## 📅 주차별 개발 일정 매핑

| 주차 | 구현 엔드포인트 # | 비고 |
| --- | --- | --- |
| **2주차** (6/3~6/9) | 1~18 (총 18개) | Auth + Member + Fridge |
| **3주차** (6/10~6/16) | 19~31, 40 (총 14개) | Recipe + Review + Wishlist + AI 추천 |
| **4주차** (6/17~6/24) | 32~39, 41 (총 9개) | Follow + Challenge + AI 코칭 |

→ 총 **41개 엔드포인트**

---

# 📌 추가 반영 메모 (2026-06-01, WBS-④ 확장 — 식재료 사전 API)

> 위 본문은 1주차 검토 기준 **41개**입니다. 이후 식재료 사전(`ingredient_dictionary`, V5)이 추가되며 엔드포인트 2개가 신설되어 **현재 총 43개**입니다.

## 12. 식재료 사전 (Ingredient) — 신규

### GET `/api/ingredients/autocomplete` — 식재료 자동완성 `F21`

**인증** 불필요 · **Query** `?keyword=양` (부분일치)

**Response 200** `[{ "ingredientDictId": 12, "name": "양파", "category": "채소" }]` 최대 N개

---

### GET `/api/ingredients/suggest` — 보관기한/보관법 제안 `F22`

**인증** 불필요 · **Query** `?name=양파`

**Response 200**

```json
{
  "name": "양파",
  "category": "채소",
  "defaultStorageType": "ROOM_TEMP",
  "fridgeDays": 60,
  "freezerDays": 180,
  "roomTempDays": 14,
  "storageTip": "..."
}
```

> 💡 사전에 없으면 404 대신 **"직접입력 모드" 폴백 응답**을 반환한다. (종 수보다 폴백이 커버리지 핵심.)
> 신규 컨트롤러 `IngredientController.java` (`/api/ingredients/**`) 추가 → 컨트롤러 총 **10개**.

## 🔢 최종 엔드포인트 카운트

| 그룹 | 개수 |
| --- | --- |
| 인증 / 회원 / 냉장고 / 레시피 / 리뷰 / 찜 / 팔로우 / 코스 / AI | 4 / 8 / 6 / 4 / 4 / 5 / 2 / 6 / 2 |
| **식재료 (신규)** | **2** |
| **합계** | **43** |

→ 2주차 구현 범위도 기존 18개 + 식재료 2개 = **20개**로 갱신.
