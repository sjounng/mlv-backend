# Maribel Backend

마리벨 공식 웹사이트 요구사항 v1.0 기반 Spring Boot 백엔드입니다. 현재 구현은 프론트엔드 개발과 운영 플로우 검증을 바로 시작할 수 있는 1차 API 서버입니다.

## Stack

- Java 21
- Spring Boot 4.0.6
- Spring MVC, Spring Security, Spring Data JPA
- Flyway database migrations (`src/main/resources/db/migration`)
- Redis-backed refresh token sessions
- PostgreSQL local/production DB
- H2 test DB
- JWT access token + HttpOnly refresh token cookie

## Run

PostgreSQL과 Redis가 필요합니다. 로컬에서는 먼저 DB와 Redis를 준비한 뒤 애플리케이션을 실행하세요.

```bash
psql -d postgres -c "CREATE ROLE maribel WITH LOGIN PASSWORD 'maribel_dev_password';"
createdb -O maribel maribel
```

`.env.example`을 `.env`로 복사하고 로컬 DB 비밀번호를 채우세요.

```bash
redis-server
```

```bash
./gradlew bootRun
```

기본 서버는 `http://localhost:8080`에서 실행됩니다.

로컬 기본 관리자:

- username: `admin`
- password: `change-me-now`

운영 전에는 반드시 `MARIBEL_BOOTSTRAP_ADMIN_PASSWORD`, `MARIBEL_JWT_SECRET`, `MARIBEL_WEBPANEL_API_KEY`, `STELLA_WEBHOOK_SECRET`, `STELLA_ALLOW_UNSIGNED_WEBHOOK=false`, `REDIS_HOST`, `REDIS_PASSWORD`를 환경변수로 바꿔주세요. HTTPS 운영 환경에서는 `MARIBEL_REFRESH_COOKIE_SECURE=true`로 설정하세요.

## Database Migrations

- 스키마는 Flyway 로 관리합니다. 변경 시 `src/main/resources/db/migration/V{n}__{설명}.sql` 을 추가하세요.
- `ddl-auto` 기본값은 `validate` 입니다. 엔티티와 마이그레이션이 어긋나면 부팅이 실패합니다.
- 기존 로컬 DB(과거 `ddl-auto=update` 로 생성)는 첫 부팅 때 `baseline-on-migrate` 로 V1 을 건너뛰고 자동 합류합니다.

## Token Policy

- Access token: JWT, 15분 만료, `Authorization: Bearer <token>`로 사용
- Member refresh token: 14일 만료, Redis에 SHA-256 hash만 저장
- Admin refresh token: 12시간 만료, Redis에 SHA-256 hash만 저장
- Refresh token 전달: `maribel_refresh` HttpOnly cookie, 개발 호환을 위해 response body의 `refreshToken`도 유지
- `/api/auth/refresh`: refresh token rotation 적용. 기존 refresh token은 즉시 폐기되고 새 refresh token이 발급됩니다.
- 이미 rotate된 refresh token이 다시 들어오면 재사용 공격으로 보고 해당 세션을 종료합니다.
- `/api/auth/logout`: 현재 refresh session 폐기
- `/api/auth/logout-all`: 현재 계정의 모든 refresh session 폐기

## Main API

- `POST /api/auth/dev-login`: 프론트/로컬 개발용 Microsoft OAuth 대체 로그인
- `GET /api/auth/microsoft/authorize-url`: Microsoft OAuth authorize URL 생성
- `POST /api/auth/refresh`, `POST /api/auth/logout`, `POST /api/auth/logout-all`
- `GET /api/public/server-status`
- `GET /api/legal/terms/latest?type=TERMS|PRIVACY|REFUND`
- `GET /api/shop/categories`, `GET /api/shop/products`
- `POST /api/shop/cash/charges`
- `POST /api/payments/stella/webhook`
- `POST /api/shop/purchases`
- `GET /api/me/profile`, `/api/me/cash`, `/api/me/charges`, `/api/me/purchases`, `/api/me/mails`
- `GET /api/events`, `POST /api/events/{id}/claim`
- `POST /api/redeem-codes/use`
- `POST /api/contact/inquiries`, `GET /api/contact/inquiries/my`
- `GET /api/webpanel/mails/pending`, `POST /api/webpanel/mails/{id}/ack`
- `GET /api/admin/dashboard`
- 관리자 상품/카테고리/우편템플릿/이벤트/리딤코드/환불/문의/감사로그 API: `/api/admin/**`

## API Docs

- Scalar UI: `http://localhost:8080/scalar`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

문서에는 JWT bearer 토큰, refresh cookie(`maribel_refresh`), 웹패널 API 키(`X-Maribel-Webpanel-Key`), Stella 웹훅 서명(`X-Stella-Signature`) 보안 스키마가 포함됩니다.

## Integration Boundaries

- Microsoft OAuth 실제 콜백은 Azure 앱 등록, client secret, Xbox Live/XSTS/Minecraft Profile API 체인 확정 후 구현해야 합니다.
- Stella IT는 공식 API 명세가 아직 없으므로 현재는 주문 생성과 웹훅 멱등 처리 경계를 먼저 구현했습니다. 서명은 `merchantOrderId:stellaPaymentId:status:paidAmountKrw` HMAC-SHA256 Base64 형태의 임시 검증입니다.
- 마크 서버 연동은 문서상 DB 폴링/gRPC 미확정입니다. 현재는 DB 폴링형 웹패널 API(`/api/webpanel/mails/pending`, `/ack`)로 우편 큐를 제공합니다.

## Verify

```bash
./gradlew test
```

통합 테스트는 Scalar/OpenAPI 문서 공개, refresh token cookie/rotation/logout, 로그인, Stella 웹훅 캐시 충전, 상품 구매, 웹패널 우편 ACK 흐름을 검증합니다.
