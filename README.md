# Livingbrush Backend API

**Livingbrush XR 플랫폼을 위한 백엔드 API 서버**

Spring Boot 기반의 REST API 서버로, 어떤 개발 환경에서도 Docker를 통해 손쉽게 실행할 수 있도록 설계되었습니다.  
도메인 중심 설계를 적용하여 유지보수성과 확장성을 높였습니다.

## 📋 목차

- [기술 스택](#-기술-스택)
- [빠른 시작 (로컬 개발)](#-빠른-시작-로컬-개발)
- [프로젝트 구조](#-프로젝트-구조)
- [API 문서](#-api-문서)
- [데이터베이스](#-데이터베이스)
- [Docker 명령어](#-docker-명령어)
- [배포 (AWS)](#-배포-aws)

## 🛠 기술 스택

### Core Framework

- Java 17 - 최신 LTS 버전
- Spring Boot 3.5.3 - 현대적인 웹 애플리케이션 프레임워크
- Spring Data JPA - 데이터베이스 액세스 레이어
- Spring Security - 인증 및 인가
- Spring Validation - 입력 데이터 검증

### Authentication

- JWT (JSON Web Token) - Stateless 인증 방식
- OAuth2 - Google, Meta(Oculus) 소셜 로그인

### Database

- PostgreSQL 16 - 메인 데이터베이스
- PostGIS 3.4 - 위치 기반 서비스 지원 (필요시)
- Flyway - 데이터베이스 스키마 버전 관리

### Build & DevOps

- Gradle 8.5 - 빌드 자동화 도구
- Docker & Docker Compose - 컨테이너화 및 환경 구성
- Lombok - 보일러플레이트 코드 감소

### Documentation & Monitoring

- Springdoc OpenAPI 3 - API 문서 자동 생성 (Swagger UI)
- Spring Boot Actuator - 애플리케이션 상태 모니터링

## 🚀 빠른 시작 (로컬 개발)

### 전제 조건

- Docker Desktop 설치 필요
- 로컬에 Java나 PostgreSQL을 별도로 설치할 필요 없음

### 1. 프로젝트 클론

```bash
git clone https://github.com/your-organization/livingbrush-backend-api.git
cd livingbrush-backend-api
```

### 2. 환경 변수 설정

```bash
cp .env.example .env
```

- `.env` 파일은 로컬 개발에 필요한 모든 환경 변수를 포함하며, `JWT_SECRET`은 반드시 자신만의 비밀 값으로 변경해야 합니다.

```env
# PostgreSQL Database Credentials (for local docker-compose)
POSTGRES_DB=bauhaus_db
POSTGRES_USER=bauhaus_user
POSTGRES_PASSWORD=your_local_db_password

# JWT Secret Key (MUST BE CHANGED)
JWT_SECRET=your-super-secret-key-for-jwt-that-is-long-and-secure

# Meta (Oculus) OAuth2 Credentials
META_APP_ID=your_meta_app_id
META_APP_SECRET=your_meta_app_secret

# AI Server URL
AI_SERVER_URL=http://your_ai_server_url
```

> ⚠️ `.env` 파일은 `.gitignore`에 포함되어 Git에 커밋되지 않습니다.

### 3. 애플리케이션 실행

```bash
docker-compose up --build -d
```

### 4. 실행 확인

```bash
# 실시간 로그 확인
docker-compose logs -f app

# 브라우저에서 API 문서 확인
open http://localhost:8888/swagger-ui/index.html
```

> `Started LivingbrushBackendApiApplication` 메시지가 보이면 성공적으로 실행된 것입니다.

## 📁 프로젝트 구조

```
livingbrush-backend-api/
├── src/main/java/com/bauhaus/livingbrushbackendapi/
│   ├── ai/           # AI 관련 기능
│   ├── artwork/      # 작품 관련 기능
│   ├── auth/         # 인증 및 OAuth2 관련
│   ├── common/       # 공통 엔티티 및 DTO
│   ├── exception/    # 전역 예외 처리
│   ├── media/        # 미디어 파일 처리
│   ├── qrcode/       # QR 코드 기능
│   ├── security/     # Spring Security, JWT 설정
│   └── user/         # 사용자 기능
├── src/main/resources/
│   ├── db/migration/
│   │   └── V1__Initial_schema_setup.sql
│   ├── application.yml
│   └── application-prod.yml
├── docker-compose.yml
├── docker-compose.prod.yml
├── Dockerfile
├── build.gradle
├── .env.example
└── README.md
```

## 📖 API 문서

- **Swagger UI:** [http://localhost:8888/swagger-ui/index.html](http://localhost:8888/swagger-ui/index.html)
- **OpenAPI JSON:** [http://localhost:8888/v3/api-docs](http://localhost:8888/v3/api-docs)

### 주요 API 엔드포인트

| Method | Endpoint                     | 설명                      | 인증 |
|--------|------------------------------|---------------------------|------|
| GET    | /api/auth/health             | 서버 상태 확인            | ❌   |
| POST   | /api/auth/login/google       | 구글 로그인               | ❌   |
| POST   | /api/auth/login/meta         | 메타(오큘러스) 로그인     | ❌   |
| POST   | /api/auth/refresh            | 액세스 토큰 재발급        | ✅   |
| GET    | /api/auth/linking-status     | 소셜 계정 연동 상태 확인  | ✅   |
| POST   | /api/ai/brush/generate       | AI 브러시 생성            | ✅   |

## 🗄 데이터베이스

### 로컬 환경 (Docker)

- PostgreSQL 16 with PostGIS 3.4
- 호스트 포트: 5433
- 컨테이너 포트: 5432
- 데이터 영속성: `postgres_data` 볼륨 사용
- 설정 위치: `.env`, `docker-compose.yml`

### 스키마 관리

- Flyway 사용
- 마이그레이션 파일 위치: `src/main/resources/db/migration/`
- 애플리케이션 시작 시 자동 실행

## 🐳 Docker 명령어

```bash
# 컨테이너 빌드 및 실행
docker-compose up --build -d

# 컨테이너 중지 및 제거
docker-compose down

# 볼륨까지 제거 (DB 초기화)
docker-compose down -v

# 실시간 로그 확인
docker-compose logs -f app

# 컨테이너 내부 접속
docker-compose exec app bash
```

## 🚀 배포 (AWS)

본 프로젝트는 AWS EC2 + RDS 기반 배포를 위한 설정이 포함되어 있습니다.

### 아키텍처

- 애플리케이션: EC2에서 Docker 컨테이너 실행
- 데이터베이스: Amazon RDS (PostgreSQL)

### 배포 절차

1. **Docker 이미지 빌드 및 푸시**

    ```bash
    # ECR에 빌드 후 푸시
    docker build -t your-ecr-repo .
    docker push your-ecr-repo
    ```

2. **인프라 준비**
   - Amazon RDS 인스턴스 생성 (PostgreSQL)
   - EC2 인스턴스 생성 (Docker, Compose 설치)
   - EC2에서 RDS 5432 포트 접근 가능하도록 보안 그룹 설정

3. **EC2 환경 설정**
   - `docker-compose.prod.yml` 업로드
   - `.env.prod` 작성 (운영 환경 변수 포함)

4. **애플리케이션 실행**

    ```bash
    docker-compose -f docker-compose.prod.yml up -d
    ```

Made with ❤️ by Bauhaus Team