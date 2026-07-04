# 🧊 냉큼 (Naeng-Keum)

> **"냉장고 속 남은 재료로 큼지막한 행복을!"**
>
> 1인 가구를 위한 스마트 식재료 관리 및 AI 맞춤 레시피 큐레이션 플랫폼

---

## 📌 프로젝트 소개

**냉큼(Naeng-Keum)** 은 바쁜 자취생들이 냉장고 속 식재료를 쉽게 관리하고, 유통기한이 임박한 재료를 활용해 최적의 요리를 할 수 있도록 돕는 **AI 기반 냉장고 파먹기 서비스**입니다.

기존 서비스가 유통기한을 일일이 수기로 입력해야 했던 반면, 냉큼은 **표준 식재료 사전 DB**로 보관법·기한을 자동 제안하고, **보유 재료로 만들 수 있는 공공 레시피를 우선 추천**하되 마땅한 게 없으면 **LLM이 냉장고에 맞춰 레시피를 창작**해 제공합니다.

> 스마트 식재료 관리 + (DB 우선 + AI 보완) 맞춤 레시피 + 자취 요리 커뮤니티 = **냉큼**

## 🎯 핵심 가치

| # | 가치 | 설명 |
| --- | --- | --- |
| 01 | **편의** | 식재료 사전 DB 연동으로 보관법·유통기한 자동 제안. 귀찮은 수기 입력 최소화. |
| 02 | **경제·환경** | 유통기한 임박 재료 우선 소진으로 식비 절약 및 음식물 쓰레기 감소. |
| 03 | **창의·소통** | DB 레시피 + AI 창작 레시피와 사용자 간의 리얼한 요리 후기 공유. |

---

## 🚀 구현 현황

> 상세 명세: [API 명세](java_seoul_16_jaeyoung_boyoung/naengkeum/src/docs/api.md) · [ERD](java_seoul_16_jaeyoung_boyoung/naengkeum/src/docs/erd.md)

### 백엔드
| 주차 | 범위 | 상태 |
| --- | --- | --- |
| **1주차** | DB 스키마(Flyway) · 식재료 사전 150종 · 공공 레시피 175건 적재 · 인증/예외/MyBatis 기반 | ✅ 완료 |
| **2주차** | 인증 4 · 회원 8 · 냉장고 6 · 식재료 사전 2 (총 **20종**) | ✅ 완료 |
| **3주차** | 레시피 4 · 리뷰 4 · 찜 5 · **AI 추천 1(SSE + 하이브리드)** (총 **14종**) | ✅ 완료 |
| **4주차** | 팔로우 2 · 챌린지 6 · **AI 코칭 1(SSE)** · **챌린지 진행률/배지 자동지급 1** (총 **10종**) | ✅ 완료 |
| **5주차** | **관리자 3(통계·사용자 목록·차단/해제)** · **레시피 사진 업로드 1(본인 검증)** · 레시피 담기/공개(from-ai·publish) | ✅ 완료 |
| **6주차+** | **소셜 로그인(구글·카카오 OAuth2)** · **조미료 보유관리** · **신고(레시피/리뷰)** · **관리자 확장(회원 삭제·역할변경·신고 처리)** · **공개 시 냉장고 재고 자동 차감(F33)** · 작성자 소감/후기 · **AWS 배포(EC2·Docker·HTTPS)** | ✅ 완료 |

**구현된 API 그룹** (인증/회원/냉장고/식재료/레시피/리뷰/찜/팔로우/챌린지/조미료/신고/관리자/AI): **기능 엔드포인트 68개** (+ 헬스·디버그 7개)

### 프론트엔드 (Vue 3)
| 화면 | 내용 | 상태 |
| --- | --- | --- |
| 기반 셋업 | Vite·Pinia·Router·Axios + 공통 레이아웃(다크 사이드바) + **JWT 인터셉터·role 가드** | ✅ |
| 인증 | 로그인 / 회원가입 · **구글·카카오 소셜 로그인** | ✅ |
| 냉장고 | 보관위치 탭·정렬·D-day 목록·CRUD + 식재료 자동완성·보관기한 제안 + **조미료 보유관리** | ✅ |
| 레시피 | 검색·자동완성·필터 · 상세(재료·단계·영양·작성자 카드) · **사진 업로드** · **신고** | ✅ |
| 리뷰·찜 | 평점 통계·작성/수정/삭제 · 찜 토글 · **리뷰 신고** | ✅ |
| **AI 추천** | **fetch SSE 스트리밍** · 출처 배지(DB/AI) · 찜 저장 · **내 레시피로 담기(소감 입력) → 공개** | ✅ |
| 마이페이지 | 내 정보·수정 · 찜·내 리뷰 · 내 레시피(**공개/비공개 전환·삭제**, 사진 필수 공개) · 프로필 사진 | ✅ |
| 유저·팔로우 | 타 유저 프로필(공개 레시피 그리드) · 팔로우 목록 | ✅ |
| **관리자** | 5탭(대시보드·회원·레시피·리뷰·신고) · 회원 차단/삭제/역할변경 · 신고 처리 | ✅ |
| 마감 | 토스트 알림 · 브랜드 로고(Lottie)·Paperlogy 폰트 셀프호스팅 · 파비콘 | ✅ |

---

## 🤖 AI 레시피 추천 (하이브리드)

`POST /api/ai/recommend` — **SSE 스트리밍**으로 추천 결과를 실시간 전송합니다.

```
냉장고 재료 ─▶ ① DB 175개 공공 레시피 매칭 (보유 ≥ 2 그리고 '사야 할 재료(기본양념 제외) ≤ 4')
                    ├─ 후보 충분        → 📚 DB 레시피 추천 (현실적이고 다양하게)
                    ├─ 후보 1~2개       → DB / AI 혼합 (AI엔 후보를 참고로 전달)
                    └─ 후보 없음        → 🤖 AI 생성 (LLM이 냉장고에 맞춰 창작)
```

- 응답 SSE 이벤트: `source → title → summary → ingredient* → step* → meta → done` (오류 시 `error`)
- `source` 이벤트로 **DB 레시피(recipeId)** 인지 **AI 생성**인지 구분
- 빈 냉장고 `400`, AI 키 미설정 `503`, 스트리밍 중 오류 `error` 이벤트로 처리
- 알레르기 반영 · 유통기한 임박 우선 · 다양성(temperature + 무작위 스타일 힌트) 옵션 지원

---

## ⚙️ 주요 기능

### ① 식재료 사전 기반 스마트 등록

- 재료명 자동완성(`/api/ingredients/autocomplete`)과 보관법·기한 제안(`/api/ingredients/suggest`).
- 사전에 없는 재료는 404 대신 **직접입력 폴백**으로 대응(커버리지 우선).

### ② AI 맞춤 냉파 레시피 (위 하이브리드 참고)

- 내 냉장고 재료로 **만들 수 있는 DB 레시피 우선**, 없으면 **AI가 창작**.

### ③ 소셜 리뷰 · 찜 · 대시보드

- **리뷰** : 레시피별 별점/후기 작성, 평점 통계(avg·분포) 제공.
- **찜하기** : 일반/AI 레시피 모두 보관, 통합 목록 조회.
- **대시보드** : 냉장고 요약 + 유통기한 D-3 임박 재료 알림.

### ④ 팔로우 · 챌린지 · AI 식재료 코칭

- **팔로우** : 자취 요리를 잘하는 사용자를 팔로우/언팔로우(자기 팔로우 차단, 중복 방지). 팔로워 수 즉시 반영.
- **냉파 챌린지** : "이번 주 식비 0원" 등 챌린지 목록/상세/참여/통계 조회. 참여 여부·진행률·달성 배지 표시.
- **배지 자동지급** : `PATCH /api/challenge/{id}/progress`로 진행률 갱신 → 100% 달성 시 보상 배지를 자동 지급(중복 방지). 응답의 `badgeEarned`로 "🎉 배지 획득!" 알림 트리거.
- **AI 식재료 코칭** : `POST /api/ai/coaching` — 애매하게 남은 재료의 보관법(storage)과 활용 조합(combo)을 LLM이 분석해 **SSE 스트리밍**으로 제공.

### ⑤ AI 레시피 담기 · 공개 · 사진 · 재고 자동 차감

- **내 레시피로 담기** : AI 추천 결과를 개인 보관함에 담기(`POST /api/recipe/from-ai`). 처음엔 비공개이며, 담을 때 **본인 한마디(소감, `author_note`)** 를 선택 입력.
- **사진 업로드** : 본인 레시피에 직접 만든 음식 사진 첨부(`POST /api/recipe/{id}/image`). 소유 검증(공공/타인 403), jpg/png/webp + 5MB.
- **공개하기 + 재고 자동 차감(F33)** : `PATCH /api/recipe/{id}/publish` (본인만, **사진 필수**). 공개 시 레시피에 쓴 **정량 재료를 냉장고에서 자동 차감**(이름 일치 + 단위 호환, 조미료·비정량 제외) → 응답 `consumed[]`. **최초 1회만** 차감(`ingredients_consumed` 플래그로 재공개 중복 차감 방지).
- **비공개 전환 · 삭제** : `PATCH /api/recipe/{id}/unpublish`(다시 비공개), `DELETE /api/recipe/{id}`(본인 또는 관리자).
- **작성자 후기** : 상세 페이지에서 본인이 만들어 본 후기(`author_review`)를 작성/수정(`PATCH /api/recipe/{id}/review`).

### ⑥ 소셜 로그인 · 조미료 · 신고

- **소셜 로그인(OAuth2)** : 구글·카카오 로그인(`/oauth2/authorization/{provider}`). 백엔드 OAuth2 Client가 회원을 `social_provider`+`social_id`로 매핑(없으면 자동 가입)하고 **우리 JWT를 발급**해 프론트로 리다이렉트(STATELESS, 인가요청은 쿠키 저장).
- **조미료 보유관리** : 마스터 조미료 목록 + 보유 여부 토글(`/api/seasonings`). AI 추천 시 보유 조미료는 "사야 할 재료"에서 제외.
- **신고** : 부적절한 레시피/리뷰를 신고(`POST /api/report`, 레시피·리뷰 XOR, 중복 409) → 관리자 신고 탭에 누적.

### ⑦ 관리자 (ADMIN)

- **권한 분리** : 회원 역할(role)을 JWT에 실어 `ROLE_ADMIN`/`ROLE_USER`로 매핑. 프론트는 토큰 디코드로 메뉴·라우트 게이팅, `/api/admin/**`는 백엔드에서 ROLE_ADMIN만 접근.
- **5탭 대시보드** : 통계(`/stats`) · 회원(검색·차단/해제·**삭제**·**역할 변경**) · 레시피·리뷰(모더레이션) · **신고 처리**(`/reports`, resolve).
- **운영자 보호** : 관리자 계정은 차단·삭제 불가, 강등은 운영자만(본인 강등 방지).

---

## 🗄️ 데이터베이스 (실제 스키마)

Flyway 마이그레이션(`java_seoul_16_jaeyoung_boyoung/naengkeum/src/main/resources/db/migration`)으로 관리합니다.

총 **17개 테이블**.

| 테이블 | 설명 |
| --- | --- |
| `member` | 회원 (이메일·비밀번호·닉네임·알레르기 CSV·**역할 role**·**프로필 사진**·**소셜 provider/id**) |
| `fridge_item` | 내 냉장고 재료 (보관위치·유통기한) |
| `ingredient_dictionary` | 식재료 사전 (보관법·권장기한, V16에서 확장) |
| `seasoning` / `member_seasoning` | 조미료 마스터 / 회원 보유 조미료 (V15) |
| `recipe` / `recipe_ingredient` / `recipe_step` | 레시피 본문·재료·조리단계 (**author_id·is_public·image_url·author_note·author_review·ingredients_consumed**) |
| `review` | 레시피 리뷰 (별점 1~5, UNIQUE(member,recipe)) |
| `wishlist` / `ai_recipe` | 찜 (일반·AI 레시피 XOR) / AI 생성 레시피(JSON) |
| `report` | 레시피/리뷰 신고 (XOR, 누적·처리 상태) (V10) |
| `follow` / `challenge` / `challenge_participant` / `badge` / `member_badge` | 팔로우 · 챌린지 · 참여 · 배지 · 회원 배지 |

| 마이그레이션 | 내용 |
| --- | --- |
| `V1`~`V2` | 전체 테이블 · 인덱스 |
| `V3`·`V4` | 마스터 데이터 · 공공 레시피 175건 |
| `V5`·`V16` | 식재료 사전 적재 및 확장 |
| `V6`·`V7` | 레시피 소유권(author_id·is_public) · 영양정보 백필 |
| `V8`·`V9`·`V14` | 회원 역할(role) · 운영자 ADMIN 지정 |
| `V10` | 신고(report) 테이블 |
| `V11`·`V13` | 프로필 사진 · 소셜 로그인 컬럼 |
| `V12` | 시드 레시피 대표 이미지 |
| `V15` | 조미료(seasoning/member_seasoning) |
| `V17`·`V18` | 작성자 소감(author_note) · 후기(author_review) |
| `V19` | 공개 시 재고 차감 플래그(ingredients_consumed) |

---

## 🛠️ 기술 스택

| 구분 | 기술 |
| --- | --- |
| **Backend** | Spring Boot 4 (Spring 7), **MyBatis** |
| **Database** | MySQL 8+, **Flyway** (마이그레이션/시드) |
| **AI 연동** | **SSAFY GMS (OpenAI 호환 프록시)** · 모델 `gpt-4.1-mini` · SSE 스트리밍 |
| **인증** | JWT (Bearer) · **OAuth2 Client (구글·카카오)** |
| **JSON** | Jackson 3 (`tools.jackson`) |
| **빌드/버전관리** | Maven (`mvnw`) · Git / GitLab |
| **Frontend** | **Vue 3** (Vite) · Pinia · Vue Router · Axios |
| **배포/인프라** | **AWS EC2** · **Docker Compose**(nginx + app + MySQL) · **Let's Encrypt(HTTPS)** · DuckDNS |

### 시스템 아키텍처

```text
[Vue.js Frontend (nginx 정적 서빙)]
      │  REST API (Axios) · SSE · OAuth2 redirect
      ▼
[Spring Boot Backend]  ──(① DB 레시피 매칭)──▶ [MySQL]
      │                                         (식재료 사전 / 레시피 / 유저 데이터)
      └──(② 부족 시 프롬프트 전송, SSE)──▶ [SSAFY GMS · gpt-4.1-mini]

운영: EC2 1대 · Docker Compose(nginx 443 TLS · app · mysql) · HTTPS(Let's Encrypt)
```

---

## 📁 프로젝트 구조 (실제)

**백엔드** (`java_seoul_16_jaeyoung_boyoung/naengkeum`)
```text
naengkeum/
├── src/main/java/com/fridgefamer/
│   ├── config/        # SecurityConfig(JWT·CORS), JwtProvider, JwtAuthenticationFilter
│   ├── controller/    # Auth/Member/Fridge/Ingredient/Recipe/Review/Wishlist/Follow/Challenge/Ai/Health
│   ├── service/       # 도메인별 비즈니스 로직 (AiRecommendService=하이브리드 추천, AiCoachingService=식재료 코칭)
│   ├── mapper/        # MyBatis 매퍼 인터페이스 (fridge/member/recipe/review/wishlist/follow/challenge/ai/...)
│   ├── dto/           # request / response DTO (record)
│   └── exception/     # GlobalExceptionHandler, ApiException, ErrorCode(9종)
├── src/main/resources/
│   ├── mapper/        # MyBatis XML (도메인별)
│   ├── db/migration/  # Flyway V1~V19
│   ├── db/fixtures/   # dev 시드(D1)
│   └── application*.yml
└── src/docs/          # erd.md, api.md (설계/명세)
```

**프론트엔드** (`java_seoul_16_jaeyoung_boyoung/frontend`, Vue 3)
```text
frontend/src/
├── api/          # http(Axios+JWT 인터셉터) / auth·member·fridge·ingredients·recipe·review·wishlist
├── stores/       # auth (Pinia, localStorage 동기화)
├── router/       # 라우트 + 인증 가드
├── layouts/      # DefaultLayout (Header + 하단 네비)
├── components/   # FridgeItemForm·ReviewSection·AiRecipeModal·ToastContainer ...
├── composables/  # useToast
└── views/        # Login·Register·Home·Fridge·Recipe(+Detail·Publish)·AiRecommend·Challenge·Follow·UserProfile·MyPage
```

---

## ▶️ 실행 방법 (Getting Started)

### 사전 준비
- JDK 17 (프로젝트 빌드 기준)
- MySQL 8+ 실행, DB `naengkeum` 및 계정 `ssafy` / `ssafy`

```sql
CREATE DATABASE IF NOT EXISTS naengkeum CHARACTER SET utf8mb4;
CREATE USER IF NOT EXISTS 'ssafy'@'localhost' IDENTIFIED BY 'ssafy';
GRANT ALL PRIVILEGES ON naengkeum.* TO 'ssafy'@'localhost';
```

### 환경변수 (AI 추천용)
| 변수 | 기본값 | 설명 |
| --- | --- | --- |
| `OPENAI_API_KEY` | (없음) | **필수** — 없으면 AI 추천이 503. Git 커밋 금지. |
| `OPENAI_BASE_URL` | `https://gms.ssafy.io/gmsapi/api.openai.com/v1` | SSAFY GMS(OpenAI 호환) |
| `OPENAI_MODEL` | `gpt-4.1-mini` | 사용 모델 |

### 백엔드 실행
```bash
cd java_seoul_16_jaeyoung_boyoung/naengkeum
OPENAI_API_KEY=<발급키> ./mvnw spring-boot:run   # dev 프로파일, 포트 8080
# 최초 구동 시 Flyway가 스키마 + 식재료 150종 + 공공 레시피 175건 자동 적재
```
> ⚠️ **그냥 `./mvnw spring-boot:run`만 실행하면 `.env`를 읽지 않아 AI가 503/500**이 납니다.
> 키를 위처럼 직접 주입하거나, VSCode F5(launch.json envFile=.env), 또는 `./run.sh`(`.env`의 키를 export 후 실행)를 사용하세요.

### 프론트엔드 실행
```bash
cd java_seoul_16_jaeyoung_boyoung/frontend
npm install
npm run dev     # http://localhost:5173 (백엔드 CORS가 5173 허용)
```
> 백엔드 주소는 기본 `http://localhost:8080`. 바꾸려면 `VITE_API_BASE_URL` 환경변수 사용.

### 빠른 확인
```bash
# 식재료 자동완성
curl -G localhost:8080/api/ingredients/autocomplete --data-urlencode "keyword=양"

# 인증이 필요한 API는 임시 토큰 발급(dev) 후 사용
TOKEN=$(curl -s "localhost:8080/api/test/token?memberId=1" | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")
curl -N -X POST localhost:8080/api/ai/recommend \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"prioritizeExpiry":true,"applyAllergy":true}'

# 챌린지 목록 (공개)
curl localhost:8080/api/challenge

# 챌린지 참여 → 진행률 100% → 배지 자동 획득
curl -X POST localhost:8080/api/challenge/1/join -H "Authorization: Bearer $TOKEN"
curl -X PATCH localhost:8080/api/challenge/1/progress \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"progress":100}'
# → {"progress":100,"achieved":true,"badgeEarned":true}

# AI 식재료 코칭 (SSE) — storage/combo 스트리밍
curl -N -X POST localhost:8080/api/ai/coaching \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"ingredientName":"egg"}'
```

---

## ☁️ 배포 (운영)

**AWS EC2 단일 인스턴스 + Docker Compose**로 운영합니다.

```text
EC2 (Docker Compose)
 ├─ nginx  : 443 TLS(Let's Encrypt) · Vue 빌드 정적 서빙 · /api·/images·/oauth2 프록시
 ├─ app    : Spring Boot(prod 프로파일) · Flyway 자동 마이그레이션
 └─ mysql  : 내부 네트워크
 volumes  : DB 영속화 · 업로드 파일(`uploads/`) 영속화
```

- **무중단 재배포** : 서버에서 `git pull && docker compose up -d --build` (Flyway가 새 마이그레이션 자동 적용).
- **HTTPS** : DuckDNS 도메인 + certbot(Let's Encrypt). 소셜 로그인 운영 리다이렉트(https) 충족.
- **시크릿** : DB·JWT·`OPENAI_API_KEY`·OAuth 키는 서버 `.env`로만 주입(git 커밋 금지).
- 상세 절차: [`DEPLOY.md`](java_seoul_16_jaeyoung_boyoung/DEPLOY.md)

---

## 🌐 활용 API 및 외부 연동

| API명 | 활용 방식 |
| --- | --- |
| **식약처 조리식품 레시피 API** | 앱 초기 구동 시 검색 가능한 공공 레시피 데이터 적재용(175건) |
| **SSAFY GMS (OpenAI 호환)** | 냉장고 데이터 기반 레시피 생성/코칭. JSON 모드 + SSE 스트리밍 |

---

## 📅 향후 확장 로드맵

- **Phase 2** : 영수증 OCR 등록 · 동네 식재료 쉐어링 · AI 레시피 공유 게시판(댓글·좋아요) · 챌린지 조건별 진행률 자동 추적(현재는 진행률 직접 갱신 방식) · 팔로우 레시피 알림 · 업로드 파일 S3 전환

---

## 👨‍💻 팀 정보

- SSAFY 15기 · seoul_16 · 김재영 / 서보영
