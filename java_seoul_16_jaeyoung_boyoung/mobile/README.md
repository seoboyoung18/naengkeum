# 냉큼 — React Native (Expo) 버전 · WIP

이 브랜치(`react-native`)는 냉큼의 **React Native(Expo) 모바일 포팅** 버전입니다.
백엔드(Spring Boot, `../naengkeum`)는 **Vue 버전과 동일한 REST API를 그대로 재사용**합니다.

> Vue 웹 버전은 `vue` 브랜치를 참고하세요. (`java_seoul_16_jaeyoung_boyoung/frontend`)

## 진행 상태
- [x] 브랜치 / Expo 스캐폴드 구조
- [ ] 네비게이션 (React Navigation)
- [ ] API 클라이언트 · 인증(JWT) 스토어
- [ ] 화면 포팅: 로그인 → 회원가입 → 냉장고 → AI 추천 → 레시피 목록/상세 → 마이 → 챌린지 → 관리자

## 실행
```bash
cd java_seoul_16_jaeyoung_boyoung/mobile
npm install        # 또는: npx create-expo-app 로 최신 템플릿 재생성 후 src/ 이식
npx expo start
```
- API base URL은 `src/config.js`에서 백엔드 주소로 설정 (기본: 배포 도메인).
- 에뮬레이터/실기기에서는 `localhost` 대신 PC IP 또는 배포 주소를 사용하세요.

## 구조(예정)
```
mobile/
  App.js              # 루트 + 네비게이터
  app.json            # Expo 설정 (브랜드 컬러 #00D992)
  src/
    config.js         # API_BASE
    api/              # axios 클라이언트 (frontend/src/api 포팅)
    screens/          # 화면 (frontend/src/views 대응)
    components/
    stores/           # 상태 (Pinia → zustand/Context)
```

## 포팅 매핑 (Vue → RN)
| Vue (web) | React Native |
|---|---|
| vue-router | @react-navigation |
| Pinia store | zustand / Context |
| axios (`api/http.js`) | axios (동일, 토큰 인터셉터 유지) |
| `<router-link>` / 웹 CSS | `Pressable` + `StyleSheet` |
| InlineIcon(SVG) | react-native-svg |
