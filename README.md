# Livingbrush Backend API

**Livingbrush XR 플랫폼을 위한 백엔드 API 서버**

Docker를 통해 어떤 개발 환경에서든 일관된 방식으로 손쉽게 실행할 수 있도록 설계된 Spring Boot 기반의 REST API 서버입니다.

## 📋 목차

- [기술 스택](#-기술-스택)
- [빠른 시작](#-빠른-시작)
- [프로젝트 구조](#-프로젝트-구조)
- [API 문서](#-api-문서)
- [데이터베이스](#-데이터베이스)
- [Docker 명령어](#-docker-명령어)
- [개발 환경 설정](#-개발-환경-설정)
- [배포](#-배포)

## 🛠 기술 스택

### Core Framework
- **Java 17** - 최신 LTS 버전
- **Spring Boot 3.5.3** - 현대적인 웹 애플리케이션 프레임워크
- **Spring Data JPA** - 데이터베이스 액세스 레이어
- **Spring Validation** - 입력 데이터 검증

### Database
- **PostgreSQL 16** - 메인 데이터베이스
- **PostGIS 3.4** - 위치 기반 서비스 지원
- **Flyway** - 데이터베이스 마이그레이션 관리

### Build & DevOps
- **Gradle 8.14.2** - 빌드 자동화 도구
- **Docker & Docker Compose** - 컨테이너화 및 오케스트레이션
- **Lombok** - 보일러플레이트 코드 감소

### Documentation & Monitoring
- **Springdoc OpenAPI 3** - API 문서 자동 생성 (Swagger UI)
- **Spring Boot Actuator** - 애플리케이션 모니터링

## 🚀 빠른 시작

### 전제 조건
- **Docker Desktop** 설치 필요
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

생성된 `.env` 파일은 기본값으로 설정되어 있어 즉시 실행 가능합니다. 필요시 아래 값들을 수정하세요:

```env
# PostgreSQL Database Credentials
POSTGRES_DB=bauhaus_db
POSTGRES_USER=bauhaus_user
POSTGRES_PASSWORD=your_secret_password_here

# AI Server URL
AI_SERVER_URL=http://your_ai_server_url_here
```

> ⚠️ **중요**: `.env` 파일은 민감한 정보를 포함하므로 Git에 커밋하지 마세요. (`.gitignore`에 등록됨)

### 3. 애플리케이션 실행
```bash
docker-compose up --build -d
```

### 4. 실행 확인
```bash
# 로그 확인
docker-compose logs -f app

# 브라우저에서 API 문서 확인
open http://localhost:8888/swagger-ui/index.html
```

`Started LivingbrushBackendApiApplication` 메시지가 보이면 성공적으로 실행된 것입니다.

## 📁 프로젝트 구조

```
livingbrush-backend-api/
├── 📁 src/main/java/com/bauhaus/livingbrushbackendapi/
│   ├── 📁 config/              # 설정 클래스
│   │   ├── AppConfig.java      # 애플리케이션 설정
│   │   └── WebConfig.java      # CORS 등 웹 설정
│   ├── 📁 controller/          # REST 컨트롤러
│   │   └── AiProxyController.java
│   ├── 📁 dto/                 # 데이터 전송 객체
│   │   ├── 📁 request/         # 요청 DTO
│   │   ├── 📁 response/        # 응답 DTO
│   │   └── 📁 aiproxy/         # AI 서버 연동 DTO
│   ├── 📁 entity/              # JPA 엔티티
│   │   ├── User.java
│   │   ├── UserSettings.java
│   │   └── AiRequestLog.java
│   ├── 📁 exception/           # 예외 처리
│   │   └── GlobalExceptionHandler.java
│   ├── 📁 repository/          # 데이터 액세스 레이어
│   │   ├── UserRepository.java
│   │   ├── UserSettingsRepository.java
│   │   └── AiRequestLogRepository.java
│   └── 📁 service/             # 비즈니스 로직
│       ├── AiProxyService.java
│       └── LogService.java
├── 📁 src/main/resources/
│   ├── 📁 db/migration/        # Flyway 마이그레이션 파일
│   │   └── V1__Initial_schema_setup.sql
│   ├── application.yml         # 기본 설정
│   ├── application-local.yml   # 로컬 개발 설정
│   └── application-docker.yml  # Docker 환경 설정
├── 📁 docker/
│   ├── docker-compose.yml      # 컨테이너 오케스트레이션
│   └── Dockerfile             # 애플리케이션 컨테이너
├── build.gradle              # Gradle 빌드 스크립트
├── .env.example              # 환경 변수 템플릿
└── README.md                 # 프로젝트 문서
```

## 📖 API 문서

### Swagger UI
실행 후 다음 URL에서 대화형 API 문서를 확인할 수 있습니다:
- **Swagger UI**: http://localhost:8888/swagger-ui/index.html
- **OpenAPI JSON**: http://localhost:8888/v3/api-docs

### 주요 API 엔드포인트

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/api/ai/health` | 헬스체크 |
| `POST` | `/api/ai/brush/generate` | AI 브러시 생성 |
| `POST` | `/api/ai/color/generate` | AI 컬러 팔레트 생성 |
| `POST` | `/api/ai/chatbot` | AI 챗봇 대화 |

## 🗄 데이터베이스

### 구성
- **PostgreSQL 16** with **PostGIS 3.4** 확장
- **포트**: 5432 (호스트에서 접근 가능)
- **볼륨**: `postgres_data` (데이터 영속성 보장)

### 연결 정보
```yaml
Host: localhost
Port: 5432
Database: bauhaus_db (기본값)
Username: bauhaus_user (기본값)
Password: .env 파일에서 설정
```

### 스키마 관리
- **Flyway**를 통한 버전 관리형 마이그레이션
- 마이그레이션 파일: `src/main/resources/db/migration/`
- 애플리케이션 시작 시 자동 실행

## 🐳 Docker 명령어

### 기본 명령어
```bash
# 컨테이너 빌드 및 백그라운드 실행
docker-compose up --build -d

# 실행 중인 컨테이너 중지 및 제거
docker-compose down

# 컨테이너 및 볼륨까지 완전 제거 (데이터 초기화)
docker-compose down -v

# 실시간 로그 확인
docker-compose logs -f

# 특정 서비스 로그만 확인
docker-compose logs -f app
docker-compose logs -f postgres
```

### 개발 중 유용한 명령어
```bash
# 애플리케이션만 재빌드 (DB는 유지)
docker-compose up --build app

# 컨테이너 상태 확인
docker-compose ps

# 컨테이너 내부 접속
docker-compose exec app sh
docker-compose exec postgres psql -U bauhaus_user -d bauhaus_db
```

## 💻 개발 환경 설정

### IntelliJ IDEA 설정
1. **프로젝트 가져오기**: `File > Open > build.gradle` 선택
2. **Java SDK**: Project Structure에서 Java 17 설정
3. **Lombok 플러그인**: Settings > Plugins에서 Lombok 설치 및 활성화
4. **Annotation Processing**: Settings > Build > Compiler > Annotation Processors 활성화

### 로컬 개발 실행
Docker 없이 로컬에서 실행하려면:

1. PostgreSQL 16 설치 및 실행
2. `application-local.yml`에 로컬 DB 정보 설정
3. 프로필 지정하여 실행:
   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=local'
   ```

## 🚀 배포

### 프로덕션 배포 준비사항
1. `.env` 파일에 프로덕션 환경 변수 설정
2. `application-prod.yml` 프로필 생성 (선택사항)
3. SSL/TLS 인증서 설정
4. 리버스 프록시 (Nginx 등) 설정

### Docker 이미지 빌드
```bash
# 프로덕션용 이미지 빌드
docker build -t livingbrush-backend-api:latest .

# 특정 태그로 빌드
docker build -t livingbrush-backend-api:v1.0.0 .
```

### 헬스체크
애플리케이션 상태 확인:
```bash
curl http://localhost:8888/api/ai/health
```

---

*Made with ❤️ by Bauhaus Team*