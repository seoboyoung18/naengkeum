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

> 백엔드 기준. 상세 명세: [API 명세](java_seoul_16_jaeyoung_boyoung/naengkeum/src/docs/api.md) · [ERD](java_seoul_16_jaeyoung_boyoung/naengkeum/src/docs/erd.md)

| 주차 | 범위 | 상태 |
| --- | --- | --- |
| **1주차** | DB 스키마(Flyway) · 식재료 사전 150종 · 공공 레시피 175건 적재 · 인증/예외/MyBatis 기반 | ✅ 완료 |
| **2주차** | 인증 4 · 회원 8 · 냉장고 6 · 식재료 사전 2 (총 **20종**) | ✅ 완료 |
| **3주차** | 레시피 4 · 리뷰 4 · 찜 5 · **AI 추천 1(SSE + 하이브리드)** (총 **14종**) | ✅ 완료 |
| **4주차** | 팔로우 2 · 챌린지 6 · AI 코칭 1 (총 9종) | ⏳ 예정 |

**구현된 API 그룹** (인증/회원/냉장고/식재료/레시피/리뷰/찜/AI추천): 약 **34개 엔드포인트**

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
| `follow` / `challenge` / `badge` | 팔로우 · 챌린지 · 뱃지 (4주차 예정) |

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
| **Frontend** | Vue.js (예정) |

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

## 📁 백엔드 구조 (실제)

```text
naengkeum/
├── src/main/java/com/fridgefamer/
│   ├── config/        # SecurityConfig(JWT·CORS), JwtProvider, JwtAuthenticationFilter
│   ├── controller/    # Auth/Member/Fridge/Ingredient/Recipe/Review/Wishlist/Ai/Health
│   ├── service/       # 도메인별 비즈니스 로직 (AiRecommendService = 하이브리드 추천)
│   ├── mapper/        # MyBatis 매퍼 인터페이스 (fridge/member/recipe/review/wishlist/ai/...)
│   ├── dto/           # request / response DTO (record)
│   └── exception/     # GlobalExceptionHandler, ApiException, ErrorCode(9종)
├── src/main/resources/
│   ├── mapper/        # MyBatis XML (도메인별)
│   ├── db/migration/  # Flyway V1~V5
│   ├── db/fixtures/   # dev 시드(D1)
│   └── application*.yml
└── src/docs/          # erd.md, api.md (설계/명세)
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

### 실행
```bash
cd java_seoul_16_jaeyoung_boyoung/naengkeum
OPENAI_API_KEY=<발급키> ./mvnw spring-boot:run   # dev 프로파일, 포트 8080
# 최초 구동 시 Flyway가 스키마 + 식재료 150종 + 공공 레시피 175건 자동 적재
```

### 빠른 확인
```bash
# 식재료 자동완성
curl -G localhost:8080/api/ingredients/autocomplete --data-urlencode "keyword=양"

# 인증이 필요한 API는 임시 토큰 발급(dev) 후 사용
TOKEN=$(curl -s "localhost:8080/api/test/token?memberId=1" | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")
curl -N -X POST localhost:8080/api/ai/recommend \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"prioritizeExpiry":true,"applyAllergy":true}'
```

---

## 🌐 활용 API 및 외부 연동

| API명 | 활용 방식 |
| --- | --- |
| **식약처 조리식품 레시피 API** | 앱 초기 구동 시 검색 가능한 공공 레시피 데이터 적재용(175건) |
| **SSAFY GMS (OpenAI 호환)** | 냉장고 데이터 기반 레시피 생성/코칭. JSON 모드 + SSE 스트리밍 |

---

## 📅 향후 확장 로드맵

- **4주차** : 팔로우 · 챌린지(게이미피케이션) · AI 식재료 코칭(SSE)
- **Phase 2** : 영수증 OCR 등록 · 동네 식재료 쉐어링 · 냉파 챌린지 뱃지

---

## 👨‍💻 팀 정보

- SSAFY 15기 · seoul_16 · 김재영 / 서보영
