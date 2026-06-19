# ============================================================================
#  냉큼 백엔드 — 멀티스테이지 빌드 (maven 빌드 → JRE17 런타임)
#  ⚠️ 메모리 작은 인스턴스(t3.micro 등)에선 빌드 단계가 OOM 날 수 있음 → t3.small + swap 권장.
# ============================================================================

# ---- 1) 빌드 스테이지 ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 의존성 레이어 캐싱: pom 먼저 받아 오프라인 캐시
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# 소스 복사 후 패키지 (테스트는 빌드 속도 위해 생략 — CI에서 별도 수행 권장)
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- 2) 런타임 스테이지 ----
FROM eclipse-temurin:17-jre
WORKDIR /app

# 빌드 산출물 jar 복사 (버전 무관하게 잡히도록 와일드카드)
COPY --from=build /build/target/*.jar app.jar

# 업로드 파일 디렉터리 (compose에서 호스트 볼륨 마운트)
ENV UPLOAD_DIR=/app/uploads
RUN mkdir -p /app/uploads

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
