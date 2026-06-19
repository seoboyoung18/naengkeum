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

## 10. 핸드오프 (보영)

도메인(`https://${DOMAIN}`) 공유 → 보영이:
- 구글/카카오 콘솔에 운영 Redirect URI 추가: `https://${DOMAIN}/login/oauth2/code/{google|kakao}`
- 서버 `.env`에 `GOOGLE_CLIENT_ID/SECRET`, `KAKAO_CLIENT_ID/SECRET` 입력 → `docker compose up -d`

---

## 운영 메모

- **재배포**: `git pull && docker compose up -d --build` (Flyway가 새 마이그레이션 자동 적용)
- **업로드 영속화**: `./naengkeum/uploads` 볼륨 — 컨테이너 재생성돼도 유지. 시드 이미지(seed-*)는 repo에 포함돼 clone 시 따라옴.
- **DB 영속화**: `mysql-data` 볼륨. `docker compose down` 해도 유지(`-v` 붙이면 삭제되니 주의).
- **비용 절감**: 데모 안 할 땐 `인스턴스 stop` (EBS만 과금).
- **로그**: `docker compose logs -f [app|nginx|mysql]`
- **DuckDNS IP 갱신**: EIP는 고정이라 한 번이면 됨. (동적 IP면 cron으로 주기 갱신)
