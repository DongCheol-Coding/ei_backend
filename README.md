# <img src="https://avatars.githubusercontent.com/u/223587735?s=50&v=4"> 동철코딩 팀

현 교육과정을 제공하는 슈퍼코딩의 메인 홈페이지를 클론코딩 진행

<br>

## 1️⃣ 팀 구성원 (Team Members)

### 🔹 [Front-End](https://github.com/DongCheol-Coding/ei_frontend) : 이동현


### 🔹 [Back-End](https://github.com/DongCheol-Coding/ei_backend) : 하민철

<br>

## 2️⃣ 프로젝트 관련 링크

🔹 [프로덕트](https://dongcheolcoding.life/) : https://dongcheolcoding.life/

🔹 [노션](https://sassy-mustard-349.notion.site/240ac01973c4809689ebd424d5e6434a) : https://sassy-mustard-349.notion.site/240ac01973c4809689ebd424d5e6434a

🔹 [깃허브](https://github.com/DongCheol-Coding) : https://github.com/DongCheol-Coding

<br>

## 3️⃣ 프로젝트 기술스택

### 🛠️ 기술 스택 (Tech Stack)

#### 🔹 FrontEnd

<img src="https://img.shields.io/badge/JavaScript-F7DF1E?style=for-the-badge&logo=javascript&logoColor=white"/> <img src="https://img.shields.io/badge/React-61DAFB?style=for-the-badge&logo=react&logoColor=white"/> <img src="https://img.shields.io/badge/Redux-764ABC?style=for-the-badge&logo=redux&logoColor=white"/> <img src="https://img.shields.io/badge/TailwindCSS-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white"/> <img src="https://img.shields.io/badge/Vite-646CFF?style=for-the-badge&logo=vite&logoColor=white"/> <img src="https://img.shields.io/badge/ReactRouter-CA4245?style=for-the-badge&logo=reactrouter&logoColor=white"/>

#### 🔹 BackEnd

<img src="https://img.shields.io/badge/Java-007396?style=for-the-badge&logo=OpenJDK&logoColor=white"/> <img src="https://img.shields.io/badge/SpringBoot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"/> <img src="https://img.shields.io/badge/SpringSecurity-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white"/> <img src="https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white"/> <img src="https://img.shields.io/badge/OAuth-2F6B9A?style=for-the-badge&logo=oauth&logoColor=white"/> <img src="https://img.shields.io/badge/JPA-59666C?style=for-the-badge&logo=hibernate&logoColor=white"/> <img src="https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white"/> <img src="https://img.shields.io/badge/MapStruct-0EA5E9?style=for-the-badge"/> <img src="https://img.shields.io/badge/Lombok-F47C15?style=for-the-badge&logo=lombok&logoColor=white"/> <img src="https://img.shields.io/badge/EmailSender-EA4335?style=for-the-badge&logo=gmail&logoColor=white"/>

#### 🔹 Database

<img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white"/> <img src="https://img.shields.io/badge/AWS RDS-527FFF?style=for-the-badge&logo=amazonrds&logoColor=white"/>

#### 🔹 DevOps / 배포

<img src="https://img.shields.io/badge/AWS EC2-FF9900?style=for-the-badge&logo=amazonec2&logoColor=white"/> <img src="https://img.shields.io/badge/AWS S3-569A31?style=for-the-badge&logo=amazons3&logoColor=white"/>

#### 🔹 협업 툴

<img src="https://img.shields.io/badge/Git-F05032?style=for-the-badge&logo=git&logoColor=white"/> <img src="https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white"/> <img src="https://img.shields.io/badge/Notion-000000?style=for-the-badge&logo=notion&logoColor=white"/>
<br>

## 4️⃣ 이번 프로젝트 진행 사항(Project proceed)

| 이름                                         | 진행 사항 |
| -------------------------------------------- | --------- |
| [**이동현**](https://github.com/soohofather) | 프론트엔드 개발 및 UI 구현  |
| [**하민철**](https://github.com/teotaku)     | 백엔드 개발 및 배포 환경 구성   |

## 5️⃣ 프로젝트 소개 (Architecture & Design)

동철코딩은 Spring Boot 기반 교육 플랫폼으로,
계층형 아키텍처(Layered Architecture) 위에 **도메인 모델 패턴(Domain Model Pattern)**을 적용하여 개발된 모놀리식 애플리케이션입니다.

🔹 아키텍처

3계층 구조: Controller → Service → Repository

주요 도메인(회원, 강의, 결제, 마이페이지, 채팅)을 단일 애플리케이션 안에서 관리

🔹 도메인 모델 패턴

User 엔티티 내부에 validatePassword(), softDelete(), bumpTokenVersion() 같은 비즈니스 로직 포함

단순 데이터 보관이 아닌, 풍부한 도메인 모델로 비즈니스 규칙을 캡슐화

🔹 영속성 계층

Spring Data JPA Repository로 데이터 접근 추상화

Specification<User>와 @EntityGraph를 활용하여 동적 검색 + 성능 최적화 구현

🔹 표현 계층

UserDto.Request, UserDto.Response로 API 입출력 분리

MapStruct 기반의 DTO ↔ Entity 변환으로 반복 코드 제거

🔹 인증 및 보안

JWT 기반 Stateless 인증 (AccessToken + RefreshToken)

JwtAuthenticationFilter로 매 요청 인증/인가 처리

🔹 데이터 관리

isDeleted, deletedAt, deletedReason 필드로 Soft Delete 패턴 적용

회원 탈퇴 시 이메일을 deleted-{id}@user.invalid로 익명화하여 유니크 제약 충돌 방지 + 개인정보 보호 강화

6️⃣ API 설계 철학 (Design Philosophy)

동철코딩 백엔드의 API는 단순 CRUD를 넘어서, 보안·데이터 정합성·성능 최적화·개인정보 보호를 고려하여 설계되었습니다.
각 기능마다 문제 상황 → 고민 → 최종 의사결정 과정을 거쳐 실제 서비스 운영 환경에 적합한 구조를 선택했습니다.

🔹 회원가입 & 이메일 인증

문제: 잘못된 이메일·중복 가입 방지 필요

해결: EmailVerification 엔티티를 통해 인증 코드와 요청 JSON을 저장 → 만료 검증 후 최종 회원가입

🔹 로그인 & JWT 인증

문제: 세션 방식은 확장성에 한계

해결: AccessToken + RefreshToken 구조, tokenVersion 필드로 탈퇴/비밀번호 변경 시 토큰 무효화

🔹 회원 탈퇴

문제: Hard Delete 시 결제·수강 이력 정합성 깨짐

해결: Soft Delete(isDeleted) + 개인정보 익명화(deleted-{id}@user.invalid) → 유니크 충돌 방지 및 보안 강화

🔹 결제 API (KakaoPay 연동)

문제: Ready 성공 후 Approve 실패 시 데이터 불일치

해결: PendingPayment 테이블로 중간 상태 저장, Approve 성공 시에만 Payment / UserCourse 생성

🔹 코스 진행률

문제: 단순 시청 완료 여부만 저장하면 세부 진행률 불가

해결: LectureProgress 엔티티에 watchedSec, completed 저장 → 90% 이상 시 자동 완료 처리

🔹 관리자 회원 검색

문제: 전체 조회 후 필터링은 성능 저하 및 N+1 문제 발생

해결: Spring Data JPA Specification + @EntityGraph로 동적 검색 및 성능 최적화

🔹 실시간 채팅 (WebSocket + STOMP)

문제: REST 폴링 방식은 지연·트래픽 부담이 크고, 인증되지 않은 사용자의 접근 위험 존재

해결: WebSocket 기반 STOMP 프로토콜 도입 →

채팅방(ChatRoom) 생성 시 JWT 인증을 Handshake Interceptor에서 검증

메시지는 ChatMessage 엔티티에 저장하여 이력 유지

클라이언트 재접속 시 기존 대화 기록 불러오기 가능

DB에는 sentAt(KST 기준) 저장 → JSON 응답 시 타임존 명시
