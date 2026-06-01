# 🧊 냉큼 (Naeng-Keum)

> **"냉장고 속 남은 재료로 큼지막한 행복을!"**
>
> 1인 가구를 위한 스마트 식재료 관리 및 AI 맞춤 레시피 큐레이션 플랫폼

---

## 📌 프로젝트 소개

**냉큼(Naeng-Keum)**은 바쁜 자취생들이 냉장고 속 식재료를 쉽게 관리하고, 유통기한이 임박한 재료들을 활용해 최적의 요리를 할 수 있도록 돕는 **AI 기반 냉장고 파먹기 서비스**입니다.

기존 서비스들이 유통기한을 일일이 수기로 입력해야 하는 번거로움이 있었던 반면,
냉큼은 **'표준 식재료 사전 DB'**를 통해 날짜 입력을 자동화하고, **LLM(대형 언어 모델)**을 활용해 내 냉장고 상태에 완벽히 맞춰진 레시피를 창작하여 제공합니다.


스마트 식재료 관리 + AI 맞춤 레시피 + 자취 요리 커뮤니티 = 냉큼 (Naeng-Keum)


## 🎯 핵심 가치

| # | 가치 | 설명 |
| --- | --- | --- |
| 01 | **편의** | 식재료 사전 DB 연동으로 유통기한 자동 계산. 귀찮은 수기 입력 최소화. |
| 02 | **경제·환경** | 유통기한 임박 재료 우선 소진 알고리즘으로 식비 절약 및 음식물 쓰레기 감소. |
| 03 | **창의·소통** | AI가 제안하는 기발한 냉파 레시피와 사용자 간의 리얼한 요리 후기 공유. |

---

## 👥 타겟 사용자

### 🧑‍🍳 1인 가구 및 자취생

매일 배달 음식에 의존하기엔 식비가 부담스럽고, 막상 요리를 하려니 냉장고에 남은 애매한 재료들로 무엇을 만들어야 할지 막막한 청년층.

**Pain Point**

* 식재료를 사두고 유통기한이 지나서 버리는 일이 빈번함
* 앱에 유통기한을 일일이 타이핑해서 기록하는 것이 귀찮음
* 한두 가지 남은 재료만으로는 어떤 요리를 할 수 있을지 아이디어 부족
* 매일 똑같은 레시피에 질림

---

## ⚙️ 주요 기능

### 기능 ① 식재료 사전 기반 스마트 등록 (입력 자동화)

* 사용자가 재료명(예: "우유")을 검색 및 선택하면, 백엔드에 구축된 '식재료 사전 DB'를 조회.
* 해당 재료의 표준 보관법(냉장/냉동)과 권장 유통기한(예: +10일)을 **자동으로 계산**하여 화면에 세팅.
* 터치 두세 번만으로 내 냉장고에 식재료 등록 완료.

### 기능 ② AI 맞춤 냉파 레시피 큐레이션

| 단계 | 주체 | 내용 |
| --- | --- | --- |
| 재료 스캔 | 시스템 | 사용자의 냉장고 DB를 분석하여 유통기한 3일 이내의 임박 재료 추출 |
| AI 분석 | LLM | 임박 재료를 필수로 포함하고, 기본 조미료로만 만들 수 있는 레시피 프롬프트 전송 |
| 레시피 제안 | 시스템 | 요리명, 소요 시간, 조리 순서, 추천 이유가 포함된 맞춤형 JSON 데이터 렌더링 |
| 생존 요리 모드 | LLM | 재료가 케첩, 단무지뿐인 극단적인 상황 시 '편의점 꿀조합'으로 우회하여 코칭 |

### 기능 ③ 소셜 리뷰 및 마이페이지

* **리뷰 시스템** : AI 추천 레시피나 기존 공공 레시피를 따라 해본 후 별점과 "파 대신 양파 넣어도 맛있어요" 같은 꿀팁 후기 작성.
* **찜하기 (스크랩)** : 마음에 드는 레시피를 내 보관함에 저장.
* **알림 대시보드** : 홈 화면 접속 시 유통기한 D-3 식재료 알림 시각화.

---

## 🗺️ 앱 구조 (Information Architecture)

| 탭 | 주요 화면 |
| --- | --- |
| **홈 (대시보드)** | 유통기한 임박 재료 경고 표시 · 오늘의 추천 AI 레시피 배너 |
| **내 냉장고** | 보유 중인 전체 식재료 리스트 (D-Day순 정렬) · 재료 추가/수정/삭제 폼 |
| **레시피 탐색** | 카테고리별 공공 레시피 검색 · 상세 레시피 조회 · 사용자 요리 후기(리뷰) 목록 |
| **마이페이지** | 내 정보 및 알레르기 설정 · 내가 찜한 레시피 목록 · 내가 작성한 리뷰 관리 |

---

## 🗄️ 데이터베이스 설계 (ERD 주요 테이블)

```sql
-- 사용자
User (user_id, email, password, nickname, allergy_info)

-- 식재료 사전 (유통기한 자동화의 핵심)
Ingredient_Dict (dict_id, name, default_storage, default_exp_days, category)

-- 내 냉장고 (사용자 소유 재료)
My_Refrigerator (ref_id, user_id, dict_id, purchase_date, expire_date, quantity)

-- 레시피 (공공데이터 + AI 생성)
Recipe (recipe_id, title, content, main_ingredients, is_ai_generated)

-- 리뷰 및 후기
Review (review_id, user_id, recipe_id, rating, comment)

-- 레시피 스크랩 (찜하기)
Scrap (scrap_id, user_id, recipe_id)

```

---

## 🌐 활용 API 및 외부 연동

| API명 | 활용 방식 |
| --- | --- |
| **식약처 조리식품 레시피 API** | 앱 초기 구동 시 검색 가능한 기본 공공 레시피 데이터(Dummy) 적재용 |
| **Gemini API (또는 Claude API)** | 사용자의 냉장고 데이터를 기반으로 최적의 레시피를 조합 및 생성하여 응답 (JSON 파싱) |

---

## 🛠️ 기술 스택

| 구분 | 기술 |
| --- | --- |
| **Frontend** | Vue.js (또는 React) |
| **Backend** | Spring Boot, Spring Data JPA (또는 MyBatis) |
| **Database** | MySQL |
| **AI 연동** | Gemini API / Prompt Engineering |
| **인증** | JWT (JSON Web Token) |
| **버전 관리** | Git, GitLab / GitHub |

### 시스템 아키텍처

```text
[Vue.js Frontend]
       ↓  REST API (Axios)
[Spring Boot Backend]
       ↓  (Prompt 전송 및 결과 수신)  →  [Gemini LLM API]
[MySQL Database]
 (식재료 사전 / 유저 데이터)

```

---

## 📁 프로젝트 구조

```text
naeng-keum/
├── frontend/                          # 💻 Vue.js 프론트엔드
│   ├── src/
│   │   ├── assets/                    # 이미지, 폰트 등 정적 리소스
│   │   ├── api/                       # Axios API 통신 모듈
│   │   ├── store/                     # Pinia / Vuex 상태 관리
│   │   ├── router/                    # Vue 라우터 설정
│   │   ├── views/                     # 📺 화면(Page) 진입점
│   │   │   ├── SplashView.vue         # 스플래시 스크린
│   │   │   ├── LandingView.vue        # 랜딩 페이지
│   │   │   ├── LoginView.vue          # 로그인 페이지
│   │   │   ├── RegisterView.vue       # 회원가입 페이지
│   │   │   ├── HomeDashboardView.vue  # /dashboard 라우터 진입점
│   │   │   ├── SearchView.vue         # /search 레시피 검색
│   │   │   ├── RecipeDetailView.vue   # /recipe/:id 상세 조회
│   │   │   ├── FridgeView.vue         # /fridge 냉장고 관리
│   │   │   ├── AiRecommendView.vue    # /ai-recommend AI 추천
│   │   │   ├── ChallengeView.vue      # /challenge 챌린지 목록
│   │   │   ├── ChallengeDetailView.vue# /challenge/:id 상세
│   │   │   └── MyPageView.vue         # /mypage 마이페이지
│   │   │
│   │   └── components/                # 🧩 도메인/화면별 컴포넌트
│   │       ├── layout/                # 공통 레이아웃 (Header, BottomNav)
│   │       ├── auth/                  # 로그인, 회원가입 폼 및 UI
│   │       ├── landing/               # 랜딩 페이지용 배너 및 카드
│   │       ├── dashboard/             # 대시보드 위젯 (배너, 임박알림 등)
│   │       ├── search/                # 검색바, 필터, 자동완성
│   │       ├── recipe/                # 레시피 상세 UI, 탭 영역
│   │       ├── review/                # 리뷰 리스트 및 폼
│   │       ├── fridge/                # 냉장고 보관 탭, 그리드, 추가 모달
│   │       ├── ai/                    # AI 로딩 상태, 결과 카드
│   │       ├── challenge/             # 챌린지 카드, 프로그레스바
│   │       └── mypage/                # 활동 탭, 찜 목록, 설정 드로어
│   └── package.json
│
├── backend/                           # ⚙️ Spring Boot 백엔드
│   └── src/main/java/com/ssafy/naengkeum/
│       ├── config/                    # Security, Web(CORS), Swagger 설정
│       ├── security/                  # JWT 토큰 생성 및 필터 처리
│       ├── exception/                 # 전역 에러/예외 처리 (GlobalExceptionHandler)
│       ├── domain/                    # DB Entity (User, Fridge, Recipe 등)
│       ├── dto/                       # Request / Response 데이터 객체
│       ├── repository/                # JPA / MyBatis DB 접근 계층
│       ├── service/                   # 비즈니스 로직 (AiService 분리)
│       └── controller/                # REST API 엔드포인트
│           ├── AuthController.java
│           ├── DictController.java
│           ├── RefrigeratorController.java
│           ├── RecipeController.java
│           ├── AiController.java
│           ├── ReviewController.java
│           └── ChallengeController.java
│
├── database/
│   ├── schema.sql                     # 초기 DB 테이블 생성 스크립트 (ERD)
│   └── data.sql                       # 식재료 사전 & 공공 레시피 Dummy Data
│
├── docker-compose.yml                 # 인프라 배포 설정 (DB, Redis 등)
└── README.md

```

---

## 🚀 개발 우선순위

* [ ] **1단계 (기반 구축)** — DB ERD 설계, 식재료 사전 및 공공 레시피 더미 데이터 MySQL 적재, 회원가입/로그인 API 구현.
* [ ] **2단계 (코어 구현)** — 식재료 사전 API를 활용한 '내 냉장고' 재료 CRUD API 및 프론트엔드 UI 화면 연동 (유통기한 자동화).
* [ ] **3단계 (AI 연동)** — 유통기한 임박 재료 데이터를 LLM API에 전달(프롬프트 엔지니어링)하여 맞춤형 레시피 결과를 화면에 렌더링.
* [ ] **4단계 (확장 및 안정)** — 레시피 리뷰/별점 작성 기능, 마이페이지 찜 목록 구현 및 전체 시스템 예외 처리.

---

## 📅 향후 확장 로드맵 (Phase 2)

* **영수증 OCR 등록** : 마트 영수증을 사진으로 찍으면 식재료가 자동으로 냉장고에 등록되는 기능.
* **동네 쉐어링** : 혼자 다 먹지 못하는 대용량 식재료(예: 양배추 한 통)를 근처 자취생과 교환하거나 나누는 당근마켓형 커뮤니티 보드.
* **냉파 챌린지** : "이번 주 식비 0원 달성" 등의 게이미피케이션 요소를 도입하여 뱃지 부여.

---

## 👨‍💻 팀 정보


```

```