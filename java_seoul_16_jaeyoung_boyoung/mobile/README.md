# 냉큼 — React Native (Expo) 버전

이 브랜치(`react-native`)는 냉큼의 **React Native(Expo) 모바일 포팅** 버전입니다.
백엔드(Spring Boot, `../naengkeum`)는 **Vue 웹 버전과 동일한 REST API를 그대로 재사용**합니다.

> Vue 웹 버전은 `vue` 브랜치를 참고하세요. (`java_seoul_16_jaeyoung_boyoung/frontend`)

## 진행 상태
- [x] Expo 스캐폴드 · 브랜드 테마(디자인 토큰 포팅)
- [x] 네비게이션 (React Navigation — 하단 탭 5 + 스택)
- [x] API 클라이언트 12종 · 인증(JWT) 스토어 (zustand + SecureStore)
- [x] 화면 포팅 13종: 로그인·회원가입 / 홈 / 냉장고 / 레시피 목록·상세 / 마이 / 챌린지·상세 / 유저프로필·팔로우 / AI 추천 / 관리자
- [x] 냉장고 재료 CRUD(추가·수정·삭제) + 조미료 선택
- [x] 레시피 공개/비공개·삭제·대표사진 업로드·작성자 후기, 리뷰·신고
- [x] 소셜 로그인(구글·카카오) — 앱 스킴 리다이렉트 *(dev-build/실기기에서 동작, 아래 참고)*
- [ ] 실기기 최종 QA · 스토어 배포용 빌드(EAS)

## 실행
```bash
cd java_seoul_16_jaeyoung_boyoung/mobile
npm install
npx expo start
```
- API base URL은 `src/config.js`(`API_BASE`)에서 설정 — 기본값은 배포 도메인 `https://naengkeum.duckdns.org`.
- 실기기/에뮬레이터에서 로컬 백엔드로 붙이려면 `localhost` 대신 PC IP(실기기) 또는 `10.0.2.2:8080`(안드로이드 에뮬)로 바꾸세요.

### Expo Go vs dev-build
- **Expo Go**: 화면·냉장고 CRUD 등 일반 기능 확인용. (Expo Go가 이 프로젝트 SDK를 지원해야 함)
- **dev-build**(`npx expo run:ios` / `run:android` 또는 EAS Build): **소셜 로그인**은 커스텀 스킴(`naengkeum://`) 복귀가 필요해 dev-build/독립 실행에서 가장 안정적으로 동작합니다.

## 구조
```
mobile/
  App.js              # 루트 + 네비게이터(인증여부로 Auth/Main 전환)
  app.json            # Expo 설정 (scheme: naengkeum, 브랜드 컬러 #00D992)
  src/
    config.js         # API_BASE
    theme.js          # 디자인 토큰(colors/radius/shadow) — style.css :root 포팅
    api/              # axios 클라이언트 12종 (auth·member·fridge·seasoning·recipe·
                      #   review·wishlist·follow·challenge·report·admin·ai)
    screens/          # 화면 13종 (frontend/src/views 대응)
    components/        # RecipeCard·MyRecipeList·ReviewSection·ReportButton·
                      #   FridgeItemModal·SeasoningModal·AiRecipeModal
    stores/           # auth (zustand) — 로그인/소셜로그인/토큰 복원
    lib/              # token(SecureStore)·pickImage·socialAuth
```

## 소셜 로그인 흐름 (모바일)
Google/Kakao 콘솔에는 **백엔드 콜백만** 등록돼 있으면 되고, 앱 스킴 등록은 불필요합니다.
```
앱  ──▶ ${API}/oauth2/authorization/{google|kakao}?app_redirect=naengkeum://oauth   (인앱 브라우저)
백엔드 ──▶ provider 로그인 → 성공 핸들러가 app_redirect가 허용 스킴이면
          naengkeum://oauth#token=<JWT> 로 리다이렉트   (없으면 기존 웹 콜백)
앱  ──▶ openAuthSessionAsync가 복귀 URL의 #token= 파싱 → SecureStore 저장 → 자동 로그인
```
- 백엔드: `HttpCookieOAuth2AuthorizationRequestRepository`(app_redirect 쿠키 보관) + `OAuth2SuccessHandler`(허용 스킴 검증). 웹 흐름은 하위호환.
- 허용 스킴: `naengkeum://`(dev-build/독립실행), `exp://`(Expo Go 개발). `app.mobile.allowed-redirect-prefixes`로 설정.

## 포팅 매핑 (Vue → RN)
| Vue (web) | React Native |
|---|---|
| vue-router | @react-navigation (native-stack + bottom-tabs) |
| Pinia store | zustand |
| axios (`api/http.js`) | axios (동일, 토큰 인터셉터 유지) |
| localStorage(JWT) | expo-secure-store |
| `<router-link>` / 웹 CSS | `Pressable` + `StyleSheet` |
| InlineIcon(SVG) | @expo/vector-icons (Ionicons) |
| OAuth `#token` 콜백 페이지 | expo-auth-session + expo-web-browser (앱 스킴 복귀) |
