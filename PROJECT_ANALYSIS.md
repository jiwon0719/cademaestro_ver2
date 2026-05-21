# CodeMaestro - 프로젝트 분석 및 포트폴리오 고도화 가이드

> SSAFY 프로젝트 기술 분석 · 구조 정리 · 개선 방향 정리 문서

---

## 목차

1. [서비스 개요](#1-서비스-개요)
2. [아키텍처 구조](#2-아키텍처-구조)
3. [기술 스택](#3-기술-스택)
4. [핵심 기능 상세](#4-핵심-기능-상세)
5. [데이터 모델 (ERD 요약)](#5-데이터-모델-erd-요약)
6. [API 구조](#6-api-구조)
7. [보안 설계](#7-보안-설계)
8. [실시간 통신 흐름](#8-실시간-통신-흐름)
9. [현재 코드 품질 이슈](#9-현재-코드-품질-이슈)
10. [포트폴리오 고도화 로드맵](#10-포트폴리오-고도화-로드맵)

---

## 1. 서비스 개요

**CodeMaestro**는 온라인 알고리즘 스터디를 위한 올인원 협업 플랫폼이다.  
Zoom·Discord 같은 범용 화상회의 툴의 한계를 극복하고, 개발자 학습에 특화된 기능(공동 코드 편집, AI 리뷰, 웹 컴파일러 등)을 하나의 서비스에 통합한 것이 핵심 차별점이다.

| 항목 | 내용 |
|------|------|
| 개발 기간 | 2025.01 ~ 2025.02 (약 6주) |
| 팀 구성 | 6인 (BE 2, FE 3, INFRA 1) |
| 배포 URL | https://www.codemaestro.site |
| 총 소스 파일 | 247개 |
| API 엔드포인트 | 50+ |
| DB 테이블 | 20+ |

---

## 2. 아키텍처 구조

### 전체 시스템 구성도

```
[클라이언트]
    │
    ├── React (my-code) ─── Vite 번들링 · 메인 SPA
    └── React (my-ide) ─── 회의실 전용 IDE

         │
    [Nginx Proxy Manager]  ← 도메인 라우팅 · TLS 종료
         │
    ┌────┴──────────────────────────────────┐
    │              Docker Compose           │
    │                                       │
    │  Spring Boot (8080)                   │
    │  Node.js WS Server (3001)             │
    │  MySQL 8.0                            │
    └───────────────────────────────────────┘
         │
    ┌────┴──────────────────────────────────┐
    │         Self-hosted 서비스             │
    │                                       │
    │  OpenVidu (4443) ─── WebRTC 미디어    │
    │  Judge0 ─────────── 코드 실행 엔진    │
    └───────────────────────────────────────┘
         │
    [AWS]
    ├── EC2 (컴퓨팅)
    └── S3 (파일 스토리지 · 이미지)
```

### 프론트엔드 모노레포 구조

```
frontend/codemaestro/
├── packages/
│   ├── my-code/      ← 메인 앱 (Vite + React 19)
│   │   └── 로그인, 그룹, 커뮤니티, 마이페이지 등
│   ├── my-ide/       ← 회의실 IDE (React Scripts + TypeScript)
│   │   └── 코드 에디터, 캔버스, AI 챗봇, 영상통화
│   └── server/       ← Yjs WS 릴레이 서버 (Express)
└── package.json      ← Yarn Workspaces 루트
```

### 백엔드 패키지 구조 (DDD 기반)

```
com.ssafy.codemaestro/
├── domain/
│   ├── auth/          ← 인증·인가
│   ├── board/         ← 게시판
│   ├── boj/           ← 백준 연동
│   ├── chat/          ← 실시간 채팅
│   ├── conference/    ← 회의실 (OpenVidu)
│   ├── friend/        ← 친구 관계
│   ├── group/         ← 스터디 그룹
│   ├── notification/  ← 알림
│   ├── studyRecord/   ← 학습 기록
│   └── user/          ← 사용자
└── global/
    ├── config/        ← Security, WebSocket, AWS, Swagger 설정
    └── util/          ← JWT, S3, OpenVidu 유틸
```

---

## 3. 기술 스택

### Backend

| 분류 | 기술 | 버전 |
|------|------|------|
| 언어 | Java | 17 |
| 프레임워크 | Spring Boot | 3.4.2 |
| 빌드 | Gradle | - |
| 보안 | Spring Security + JWT (jjwt) | 3.4.2 / 0.12.3 |
| 인증 | OAuth2 Client (Naver, Kakao) | 3.4.2 |
| ORM | Spring Data JPA (Hibernate) | 3.4.2 |
| 실시간 | Spring WebSocket (STOMP) | 3.4.2 |
| 반응형 | Spring WebFlux | 3.4.2 |
| 영상회의 | OpenVidu Java Client | 2.31.0 |
| 파일 스토리지 | Spring Cloud AWS (S3) | 2.2.6 |
| 이메일 | Spring Mail (Naver SMTP) | 3.4.2 |
| API 문서 | SpringDoc OpenAPI (Swagger UI) | 2.0.2 |
| DB 드라이버 | MySQL Connector/J | 8.2.0 |
| 유틸 | Lombok | - |

### Frontend

| 분류 | 기술 | 버전 |
|------|------|------|
| 프레임워크 | React | 19.0.0 |
| 빌드 (메인) | Vite | 6.0.5 |
| 빌드 (IDE) | React Scripts | 5.0.1 |
| 언어 (IDE) | TypeScript | 4.4.2 |
| 상태관리 | Redux Toolkit + Redux Persist | 2.5.0 / 6.0.0 |
| 라우팅 | React Router DOM | 7.1.1 |
| HTTP | Axios | 1.7.9 |
| UI 프레임워크 | TailwindCSS + DaisyUI | 3.4.17 / 4.12.23 |
| 차트 | Chart.js + react-chartjs-2 | 4.4.7 / 5.3.0 |
| 코드 에디터 | CodeMirror 6 | 6.x |
| CRDT 협업 | Yjs (y-webrtc, y-websocket) | 13.6.23 |
| 캔버스 | Konva + React Konva | 9.3.18 / 19.0.2 |
| 영상회의 | OpenVidu Browser | 2.31.0 |
| AI | OpenAI SDK | 4.78.1 |
| 마크다운 | React Markdown | 9.0.3 |
| 알림 | React-Toastify | 11.0.3 |

### 인프라

| 분류 | 기술 |
|------|------|
| 클라우드 | AWS EC2, S3 |
| CI/CD | Jenkins |
| 컨테이너 | Docker + Docker Compose |
| 리버스 프록시 | Nginx Proxy Manager |
| DB | MySQL 8.0 |
| 코드 실행 | Judge0 v1.13.0 (Self-hosted) |
| 영상 서버 | OpenVidu v2.31.0 (Self-hosted) |

---

## 4. 핵심 기능 상세

### 4.1 실시간 화상 협업 (OpenVidu)

- **기술**: OpenVidu v2.31.0 (WebRTC 기반)
- **흐름**: 프론트 → Spring Boot(토큰 발급) → OpenVidu 서버 → WebRTC P2P
- **기능**: 화상·음성·화면 공유, 참가자 강퇴, 음소거 강제 적용
- **이벤트 처리**: OpenVidu Webhook으로 입퇴장 이벤트 기록

### 4.2 실시간 코드 공동 편집 (CRDT)

- **기술**: Yjs CRDT + y-websocket + y-webrtc
- **에디터**: CodeMirror 6 (cpp, java, python, javascript 지원)
- **동작**: 충돌 없는 동시 편집 보장 (Conflict-free Replicated Data Type)
- **AI 연동**: 자동완성, 문법 검사, 복잡도 분석을 에디터에서 즉시 호출

### 4.3 캔버스 동시 편집

- **기술**: Konva.js + React Konva + Yjs
- **기능**: 드로잉, 텍스트, 도형 삽입 — 참가자 전원 동기화

### 4.4 웹 기반 컴파일러 (Judge0)

- **지원 언어**: C, C++, Java, Python
- **지표**: 실행 시간, 메모리 사용량 측정
- **운영**: Self-hosted Judge0 서버로 외부 의존 최소화

### 4.5 AI 기능 (OpenAI)

| 기능 | 설명 |
|------|------|
| AI 코드 리뷰 | 선택 코드 품질 분석 및 개선 제안 |
| AI 챗봇 | 코드 관련 질의응답 (스트리밍 응답) |
| 복잡도 분석 | 시간/공간 복잡도 자동 측정 |
| 자동완성 | 코드 패턴 기반 제안 |
| 문법 검사 | 실시간 lint·오탈자 감지 |

### 4.6 실시간 채팅 (WebSocket STOMP)

- **프로토콜**: STOMP over WebSocket
- **Pub/Sub 구조**: `/pub/chat/{chatRoomId}` → `/sub/chat/{chatRoomId}`
- **메시지 브로커**: Spring In-Memory Broker

### 4.7 스터디 그룹 관리

- 그룹 생성·가입 신청·승인
- 권한: OWNER / MEMBER 역할 분리
- 회의 이력, 출석 기록, 활동 시각화 (Chart.js)
- 도전과제·뱃지 시스템

### 4.8 커뮤니티 & 소셜

- 게시글·댓글 CRUD
- 친구 요청·수락·거절
- 백준(BOJ) 티어 연동 표시
- 알림 시스템 (읽음 처리 포함)

### 4.9 인증

- 로컬 이메일+비밀번호 로그인
- Kakao / Naver OAuth2 소셜 로그인
- 이메일 인증 (PIN 코드 발송)
- JWT 액세스(10분) + 리프레시(24시간) 토큰 전략

---

## 5. 데이터 모델 (ERD 요약)

```
User ──┬── FriendRequest (sender / receiver)
       ├── GroupMember ──── Group ─── Conference ─── ConferenceTag
       ├── UserConference               │
       ├── Board ── Comment             └── GroupJoinRequest
       ├── Chat ── ChatRoom
       ├── StudyRecord
       ├── Notification
       └── RefreshEntity

Conference: id, title, description, accessCode, thumbnailUrl, moderator(User), group(Group)
Group: id, name, owner(User), currentMembers
GroupMember: group + user + role(OWNER|MEMBER) + joinDate
UserConference: user + conference + joinedAt + leftAt
FriendRequest: sender + receiver + status(PENDING|ACCEPTED|REJECTED)
```

---

## 6. API 구조

| 도메인 | 주요 엔드포인트 |
|--------|----------------|
| **인증** | POST /auth/signup, /auth/signin, DELETE /auth/logout, POST /auth/reissue |
| **OAuth2** | GET /auth/oauth2/code/{provider} (naver, kakao) |
| **회의실** | POST /conference/create, POST /conference/{id}/issue-token, GET /conference |
| **그룹** | GET /groups, POST /groups, POST /groups/requests, PUT /groups/transfer-owner |
| **유저** | GET /users/{id}, PUT /users/{id}, GET /users/search |
| **게시판** | GET /boards, POST /boards, PUT /boards/{id}, DELETE /boards/{id} |
| **채팅** | WebSocket /ws-stomp, @MessageMapping /chat/{chatRoomId} |
| **친구** | GET /friends, POST /friends/requests, PUT /friends/requests/{id} |
| **BOJ** | GET /boj/tier?bojId={bojId} |
| **알림** | GET /notifications, POST /notifications/{id}/read |
| **학습기록** | GET /study-records/{groupId}, POST /study-records |
| **검증** | GET /api/validate/email/{email}, /api/validate/nickname/{nickname} |

---

## 7. 보안 설계

### JWT 전략

```
로그인 성공
  └─ Access Token (10분)  → Response Header "access"
  └─ Refresh Token (24시간) → HTTP-Only Cookie

API 요청
  └─ Axios Interceptor: Authorization: Bearer {accessToken}

401 응답 시
  └─ Refresh Token으로 /auth/reissue 자동 호출
  └─ 새 Access Token 발급 → 재시도
```

### Spring Security 필터 체인

```
JwtFilter → LoginFilter → CustomLogoutFilter → SecurityFilterChain
```

### CORS 설정

- 허용 Origin: 프론트 URL + localhost:3000
- 노출 헤더: `access`, `Set-Cookie`
- 자격증명(credentials): 허용

---

## 8. 실시간 통신 흐름

### 화상회의 연결 흐름

```
1. POST /conference/create → 회의실 생성
2. POST /conference/{id}/issue-token → OpenVidu 연결 토큰 발급
3. 프론트: OpenVidu Browser SDK로 세션 참가
4. WebRTC P2P 미디어 스트림 교환
5. OpenVidu Webhook → POST /conference/{id}/webhook → 입퇴장 이벤트 처리
```

### 코드 공동 편집 흐름

```
1. 회의실 입장 → Yjs Doc 초기화
2. y-websocket Provider → WS Server (packages/server) 연결
3. y-webrtc Provider → 참가자 간 P2P 동기화 (fallback)
4. CodeMirror 6 + y-codemirror.next → 커서·편집 내용 실시간 반영
5. CRDT 병합 알고리즘으로 충돌 없이 상태 수렴
```

### 채팅 흐름

```
1. SockJS + StompClient로 /ws-stomp 연결
2. /pub/chat/{chatRoomId} 구독
3. 메시지 전송 → /sub/chat/{chatRoomId} 브로드캐스트
```

---

## 9. 현재 코드 품질 이슈

> 포트폴리오 개선 시 우선 해결해야 할 항목들

### 🔴 Critical (보안)

| 이슈 | 위치 | 조치 |
|------|------|------|
| OpenVidu 시크릿 하드코딩 | `application-openvidu.properties` | 환경변수로 이전 |
| OAuth2 클라이언트 시크릿 노출 | `application-oauth2-client.properties` | `.gitignore` + 환경변수 |
| 프론트 `.env`에 빈 OpenAI Key | `.env` | 환경변수 관리 체계 정비 |

### 🟠 High (코드 품질)

| 이슈 | 위치 | 조치 |
|------|------|------|
| `System.out.println()` 15+ 곳 | BojService, AuthService, GroupService 등 | `@Slf4j` + `log.debug()` 교체 |
| 테스트 코드 전무 | 전체 백엔드 | 서비스 단위 테스트 추가 |
| `ValidateController` HTTP 302 반환 | ValidateController.java | 409 CONFLICT로 수정 |
| `CustomSuccessHandler` TODO 미완성 | CustomSuccessHandler.java | OAuth2 리다이렉트 로직 완성 |

### 🟡 Medium (품질·성능)

| 이슈 | 위치 | 조치 |
|------|------|------|
| BACKEND_URL 오타 ("codemaesro") | `.env` | `codemaestro`로 수정 |
| 입력값 @Valid 어노테이션 미사용 | 전체 Controller DTO | `@Valid`, `@NotBlank`, `@Email` 추가 |
| N+1 쿼리 가능성 | LAZY 관계 로딩 | `@EntityGraph` 또는 DTO Projection |
| 레이트 리밋 없는 검증 엔드포인트 | /api/validate/* | Spring Rate Limiter 또는 Bucket4j |

---

## 10. 포트폴리오 고도화 로드맵

### Phase 1 — 버그·보안 수정 (1~2일)

- [ ] `System.out.println()` → `log.debug()`/`log.info()` 전환
- [ ] 하드코딩 시크릿 환경변수화 + `.gitignore` 점검
- [ ] `ValidateController` HTTP 상태코드 수정 (302 → 409)
- [ ] `.env` 오타 수정 (`codemaesro` → `codemaestro`)
- [ ] `CustomSuccessHandler` OAuth2 리다이렉트 완성

### Phase 2 — 코드 품질 향상 (3~5일)

- [ ] Controller DTO에 `@Valid` + `@NotBlank`, `@Email` 추가
- [ ] Global `@ControllerAdvice` 예외 핸들러 통일
- [ ] 응답 형식 표준화 (`ApiResponse<T>` 래퍼 클래스 도입)
- [ ] Swagger 문서 어노테이션 (`@Operation`, `@Tag`) 추가
- [ ] N+1 쿼리 개선 (주요 목록 조회 쿼리 최적화)

### Phase 3 — 테스트 코드 (5~7일)

- [ ] 서비스 레이어 단위 테스트 (Mockito)
  - UserService, GroupService, ConferenceService 우선
- [ ] 컨트롤러 통합 테스트 (`@WebMvcTest`)
- [ ] JWT 필터 테스트
- [ ] Testcontainers로 MySQL 통합 테스트 환경 구성
- [ ] 목표 커버리지: 60% 이상

### Phase 4 — 기능 고도화 (선택)

- [ ] **WebSocket 채팅 메시지 영속화**: 현재 인메모리 → DB 저장 + 이전 메시지 로드
- [ ] **알림 WebSocket 실시간화**: 현재 폴링 방식이면 SSE 또는 WebSocket으로 개선
- [ ] **그룹 통계 대시보드**: 주간/월간 학습 시간 시각화 고도화
- [ ] **BOJ 문제 검색·추천**: 그룹 목표 난이도에 맞는 문제 추천
- [ ] **코드 스니펫 저장**: 회의 중 인상적인 풀이를 저장하는 기능
- [ ] **녹화 기능**: OpenVidu Recording API 활용

### Phase 5 — 인프라·운영 (선택)

- [ ] **모니터링**: Spring Actuator + Prometheus + Grafana 연동
- [ ] **로그 수집**: Logback 구조화 로깅 → ELK Stack 또는 CloudWatch
- [ ] **성능 테스트**: JMeter/k6로 WebSocket 동시 접속 부하 테스트
- [ ] **CI 파이프라인 강화**: Jenkins에 테스트 + 커버리지 리포트 단계 추가

---

## 기술적 어필 포인트 (면접·포트폴리오용)

### 이 프로젝트에서 설명하기 좋은 기술 포인트

1. **CRDT 기반 동시 편집**: Yjs의 CRDT 알고리즘이 어떻게 충돌을 해결하는지 설명 가능
2. **WebRTC + WebSocket 혼용 전략**: 미디어(OpenVidu/WebRTC) vs 데이터(STOMP/WS) 분리 이유
3. **JWT Stateless 인증**: Access/Refresh 토큰 전략, Axios Interceptor로 투명한 갱신 처리
4. **모노레포 구성**: Yarn Workspaces로 my-code / my-ide / server 패키지 분리 관리
5. **Self-hosted 인프라**: Judge0·OpenVidu를 직접 운영한 경험과 트레이드오프
6. **DDD 패키지 구조**: 도메인 중심 설계로 확장성·응집도 확보

### 개선 후 추가로 설명할 수 있는 포인트

1. **테스트 전략**: 단위·통합 테스트 레이어 분리와 Testcontainers 활용
2. **쿼리 최적화**: N+1 문제 발견 → @EntityGraph 적용 과정
3. **보안 강화**: 환경변수 관리, Rate Limiting 도입 경험
4. **표준화된 API 응답**: ApiResponse 래퍼로 클라이언트 파싱 일관성 확보
