# 냉큼 배포 런북 — EC2 단일 + Docker Compose

> 대상: 배포 담당(페어). HTTP 기동까지가 이 문서 범위, 이후 HTTPS·소셜 운영설정은 아래 단계 참고.
> 사전: t3.small / EBS 30GB / 서울 리전, 보안그룹 인바운드 **22·80·443** 오픈, Elastic IP 할당.
> DuckDNS 서브도메인 + 토큰 확보됨.

아래 명령은 **Amazon Linux 2023** 기준. (Ubuntu면 `dnf`→`apt`, 패키지명 일부 다름 — 각 단계에 메모)

---

## 0. 변수 메모 (이 값들 채워서 진행)

```
EIP            = <Elastic IP>
DUCK_SUB       = <duckdns 서브도메인>        # 예: naengkeum  (→ naengkeum.duckdns.org)
DUCK_TOKEN     = <duckdns 토큰>
DOMAIN         = ${DUCK_SUB}.duckdns.org
```

## 1. SSH 접속

```bash
chmod 400 <키페어>.pem
ssh -i <키페어>.pem ec2-user@<EIP>        # Ubuntu면 ubuntu@<EIP>
```

## 2. swap 2GB (빌드 OOM 방지) + 시스템 업데이트

```bash
sudo dnf update -y                         # Ubuntu: sudo apt update && sudo apt upgrade -y
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
free -h        # Swap 2.0Gi 확인
```

## 3. Docker + Compose + git 설치

```bash
sudo dnf install -y docker git              # Ubuntu: sudo apt install -y docker.io git
sudo systemctl enable --now docker
sudo usermod -aG docker $USER
# compose 플러그인
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -SL "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64" \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
# 그룹 적용 위해 재접속(또는 newgrp docker)
exit
```
재접속 후:
```bash
docker --version && docker compose version
```

## 4. DuckDNS → Elastic IP 연결

```bash
curl "https://www.duckdns.org/update?domains=${DUCK_SUB}&token=${DUCK_TOKEN}&ip=${EIP}"
# 결과 "OK" 떠야 함. 확인:
dig +short ${DUCK_SUB}.duckdns.org         # EIP 나오면 성공
```

## 5. 소스 clone

```bash
git clone https://lab.ssafy.com/s15/a16/pjt_springbatch/springbatch_seoul_16_jaeyoung_boyoung.git app
cd app/java_seoul_16_jaeyoung_boyoung       # docker-compose.yml 있는 폴더
```

## 6. .env 작성

```bash
cp .env.example .env
nano .env
```
채울 값:
```
DB_NAME=naengkeum
DB_USERNAME=naengkeum
DB_PASSWORD=<강한 비번>
DB_ROOT_PASSWORD=<강한 비번>
JWT_SECRET=<openssl rand -base64 48 결과>
OPENAI_API_KEY=<GMS 키>
FRONTEND_BASE_URL=https://<DOMAIN>          # HTTPS 적용 예정이므로 https로 미리
# 소셜 키는 보영 단계에서 (지금은 changeme 그대로 둬도 일반 로그인 동작)
```

## 7. 기동

```bash
docker compose up -d --build               # 첫 빌드 5~10분 (maven). swap 덕에 OOM 회피
docker compose ps                          # mysql(healthy)·app·nginx 떠야 함
docker compose logs -f app                 # "Started NaengkeumApplication" + Flyway V1~V13 확인
```

## 8. HTTP 동작 확인

```bash
curl http://localhost/actuator/health      # {"status":"UP"}
```
브라우저: `http://<EIP>` 또는 `http://<DOMAIN>` → 로그인 화면 + 레시피 이미지 확인.
이 시점에서 **일반 로그인·레시피·AI추천·관리자**까지 다 동작 (소셜 로그인만 HTTPS 후).

---

## 9. HTTPS (Let's Encrypt) — 도메인 연결 후

> ⚠️ 컨테이너 nginx에 TLS를 붙이려면 **nginx.conf에 443 블록 + 인증서 볼륨**이 필요합니다.
> 이 부분은 도메인이 실제로 붙은 뒤 재영이 nginx.conf·compose를 보강해서 함께 진행 (certbot webroot 방식).
> 임시로는 HTTP로 운영 가능하나, **소셜 로그인 운영은 HTTPS 필수**(구글/카카오 정책).

대략 흐름(예정):
1. nginx에 `/.well-known/acme-challenge` 서빙 + 443 server 블록 추가
2. `certbot` 컨테이너로 `${DOMAIN}` 인증서 발급(webroot)
3. 인증서 볼륨 마운트 → nginx reload → https 동작
4. 80→443 리다이렉트

## 10. 소셜 로그인 운영설정 (보영) — 도메인·HTTPS 확정 후

운영 도메인 `https://naengkeum.duckdns.org` 기준. 코드는 이미 완성 — **콘솔 + 서버 `.env`** 설정만 하면 동작.

### 10-1. 구글 콘솔 (console.cloud.google.com → API 및 서비스 → 사용자 인증 정보)
- OAuth 2.0 클라이언트 ID → **승인된 리디렉션 URI**에 추가(로컬 URI는 지우지 말고 유지):
  - `https://naengkeum.duckdns.org/login/oauth2/code/google`
- OAuth 동의 화면이 "테스트" 상태면 테스트 사용자에 시연 계정 추가(또는 게시).

### 10-2. 카카오 콘솔 (developers.kakao.com → 내 애플리케이션)
- **카카오 로그인 > Redirect URI**: `https://naengkeum.duckdns.org/login/oauth2/code/kakao`
- **앱 설정 > 플랫폼 > Web > 사이트 도메인**: `https://naengkeum.duckdns.org` 추가
- **카카오 로그인 활성화** ON, **동의항목**: 카카오계정(이메일)·닉네임 = `account_email`, `profile_nickname`
- **보안 > Client Secret**: 발급 + **활성화 ON** (코드가 `client_secret_post` 사용)
- `KAKAO_CLIENT_ID` = **REST API 키** (네이티브/JS 키 아님)

### 10-3. 서버 `.env` 입력 후 재기동
```bash
ssh -i <키페어>.pem ec2-user@<EIP>
cd app/java_seoul_16_jaeyoung_boyoung
nano .env        # 아래 5개 값 확정
#   FRONTEND_BASE_URL=https://naengkeum.duckdns.org
#   GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET
#   KAKAO_CLIENT_ID  / KAKAO_CLIENT_SECRET
docker compose up -d            # app 컨테이너만 새 env로 재생성(--build 불필요)
docker compose ps               # app 다시 Up 확인
```

### 10-4. 검증
- `https://naengkeum.duckdns.org` → 로그인 화면 → **구글/카카오 버튼** 클릭 → 제공자 동의 → `/oauth/callback` 복귀 → 로그인 완료.
- 실패 시: `docker compose logs -f app` 에서 `invalid_client`(키 불일치) / `redirect_uri_mismatch`(콘솔 URI 누락) 메시지 확인.

---

## 운영 메모

- **재배포**: `git pull && docker compose up -d --build` (Flyway가 새 마이그레이션 자동 적용)
- **업로드 영속화**: `./naengkeum/uploads` 볼륨 — 컨테이너 재생성돼도 유지. 시드 이미지(seed-*)는 repo에 포함돼 clone 시 따라옴.
- **DB 영속화**: `mysql-data` 볼륨. `docker compose down` 해도 유지(`-v` 붙이면 삭제되니 주의).
- **비용 절감**: 데모 안 할 땐 `인스턴스 stop` (EBS만 과금).
- **로그**: `docker compose logs -f [app|nginx|mysql]`
- **DuckDNS IP 갱신**: EIP는 고정이라 한 번이면 됨. (동적 IP면 cron으로 주기 갱신)
