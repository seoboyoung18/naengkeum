# ============================================================================
#  냉큼 프론트 — 멀티스테이지 (Vite 빌드 → nginx 정적 서빙 + 백엔드 프록시)
#  이 이미지가 compose의 "nginx" 서비스. dist를 서빙하고 /api 등은 app으로 프록시.
# ============================================================================

# ---- 1) 빌드 스테이지 ----
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
# npm ci는 lock 완전 일치를 요구 → 플랫폼별 optional dep 누락 시 실패.
# 배포 빌드는 npm install로 유연하게 해결.
RUN npm install --no-audit --no-fund
COPY . .
# vite build(PROD)면 API_BASE가 자동으로 상대경로('') → nginx 같은 도메인 프록시.
RUN npm run build

# ---- 2) nginx 런타임 ----
FROM nginx:alpine
RUN rm -f /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
