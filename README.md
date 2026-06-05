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
| **4주차** | 팔로우 2 · 챌린지 6 · **AI 코칭 1(SSE)** (총 **9종**) | ✅ 완료 |

**구현된 API 그룹** (인증/회원/냉장고/식재료/레시피/리뷰/찜/AI추천/팔로우/챌린지/AI코칭): 약 **44개 엔드포인트**

### 프론트엔드 (Vue 3)
| 화면 | 내용 | 상태 |
| --- | --- | --- |
| 기반 셋업 | Vite·Pinia·Router·Axios + 공통 레이아웃 + **JWT 인터셉터** | ✅ |
| 인증 | 로그인 / 회원가입(이메일 중복확인·자동로그인) | ✅ |
| 냉장고 | 보관위치 탭·정렬·D-day 목록·CRUD + 식재료 자동완성·**보관기한 자동 제안** | ✅ |
| 레시피 | 검색·자동완성·필터(조리시간/내 재료) · 상세(재료·단계·영양) | ✅ |
| 리뷰·찜 | 평점 통계·작성/수정/삭제 · 찜 하트 토글 | ✅ |
| **AI 추천** | **fetch SSE 실시간 스트리밍** · 출처 배지(DB/AI) · 찜 저장 | ✅ |
| 마이페이지 | 내 정보·수정 · 찜 목록(AI 상세 모달) · 내 리뷰 | ✅ |
| 마감 | 토스트 알림 · 라우트 전환 폴리싱 | ✅ |

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
- **AI 식재료 코칭** : `POST /api/ai/coaching` — 애매하게 남은 재료의 보관법(storage)과 활용 조합(combo)을 LLM이 분석해 **SSE 스트리밍**으로 제공.

---

## 🗄️ 데이터베이스 (실제 스키마)

Flyway 마이그레이션(`java_seoul_16_jaeyoung_boyoung/naengkeum/src/main/resources/db/migration`)으로 관리합니다.

| 테이블 | 설명 |
| --- | --- |
| `member` | 회원 (이메일·비밀번호·닉네임·알레르기 CSV) |
| `fridge_item` | 내 냉장고 재료 (보관위치·유통기한) |
| `ingredient_dictionary` | 식재료 사전 150종 (보관법·권장기한) |
| `recipe` / `recipe_ingredient` / `recipe_step` | 공공 레시피 본문·재료·조리단계 |
| `review` | 레시피 리뷰 (별점 1~5, UNIQUE(member,recipe)) |
| `wishlist` / `ai_recipe` | 찜 (일반·AI 레시피 XOR) / AI 생성 레시피(JSON) |
| `follow` / `challenge` / `challenge_participant` / `badge` | 팔로우 · 챌린지 · 챌린지 참여 · 뱃지 (4주차 구현 완료) |

| 마이그레이션 | 내용 |
| --- | --- |
| `V1__schema.sql` | 전체 테이블 |
| `V2__indexes.sql` | 인덱스 |
| `V3__master_data.sql` · `V4__public_recipes.sql` | 마스터/공공 레시피 |
| `V5__ingredient_dictionary.sql` | 식재료 사전 150종 |

---

## 🛠️ 기술 스택

| 구분 | 기술 |
| --- | --- |
| **Backend** | Spring Boot 4 (Spring 7), **MyBatis** |
| **Database** | MySQL 8+, **Flyway** (마이그레이션/시드) |
| **AI 연동** | **SSAFY GMS (OpenAI 호환 프록시)** · 모델 `gpt-4.1-mini` · SSE 스트리밍 |
| **인증** | JWT (Bearer) |
| **JSON** | Jackson 3 (`tools.jackson`) |
| **빌드/버전관리** | Maven (`mvnw`) · Git / GitLab |
| **Frontend** | **Vue 3** (Vite) · Pinia · Vue Router · Axios |

### 시스템 아키텍처

```text
[Vue.js Frontend]
      │  REST API (Axios) · SSE
      ▼
[Spring Boot Backend]  ──(① DB 레시피 매칭)──▶ [MySQL]
      │                                         (식재료 사전 / 레시피 / 유저 데이터)
      └──(② 부족 시 프롬프트 전송, SSE)──▶ [SSAFY GMS · gpt-4.1-mini]
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
│   ├── db/migration/  # Flyway V1~V5
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
└── views/        # Login·Register·Home·Fridge·Recipe(+Detail)·AiRecommend·MyPage
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

# AI 식재료 코칭 (SSE) — storage/combo 스트리밍
curl -N -X POST localhost:8080/api/ai/coaching \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"ingredientName":"egg"}'
```

---

## 🌐 활용 API 및 외부 연동

| API명 | 활용 방식 |
| --- | --- |
| **식약처 조리식품 레시피 API** | 앱 초기 구동 시 검색 가능한 공공 레시피 데이터 적재용(175건) |
| **SSAFY GMS (OpenAI 호환)** | 냉장고 데이터 기반 레시피 생성/코칭. JSON 모드 + SSE 스트리밍 |

---

## 📅 향후 확장 로드맵

- **Phase 2** : 영수증 OCR 등록 · 동네 식재료 쉐어링 · 챌린지 배지 자동 지급(진행률 100% 달성 시) · 챌린지/배지 UI

---

## 👨‍💻 팀 정보

- SSAFY 15기 · seoul_16 · 김재영 / 서보영