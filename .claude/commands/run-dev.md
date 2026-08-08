---
description: 백엔드(Spring Boot, 8081)와 프론트엔드(Vite, 8082)를 동시에 로컬 실행합니다
---

## 목적

이 프로젝트는 백엔드(`backend/`, Spring Boot, gradle)와 프론트엔드(`frontend/`, React + Vite)가 분리되어 있어, 로컬 개발 시 두 서버를 각각 띄워야 한다. 이 커맨드는 두 서버를 한 번에 백그라운드로 실행하고 정상 기동 여부를 확인한다.

- 백엔드: `backend/gradlew bootRun` → 기본 포트 `8081` (`backend/src/main/resources/application.yml` 기준)
- 프론트엔드: `frontend`에서 `npm run dev` (Vite) → 기본 포트 `8082` (`frontend/vite.config.ts` 기준)

## 실행 절차

1. **이미 떠 있는지 확인**한다: `lsof -i :8081 -sTCP:LISTEN`, `lsof -i :8082 -sTCP:LISTEN`을 각각 실행한다.
   - 이미 LISTEN 중이면 해당 서버는 재시작하지 않고 "이미 실행 중"으로 안내한다.
   - 둘 다 이미 떠 있으면 URL만 안내하고 종료한다.
2. **백엔드가 안 떠 있으면** 프로젝트 루트에서 `cd backend && ./gradlew bootRun`을 `run_in_background: true`로 실행한다.
3. **프론트엔드가 안 떠 있으면**:
   - `frontend/node_modules`가 없으면 먼저 `cd frontend && npm install`을 실행한다 (포그라운드로, 완료까지 대기).
   - 이후 `cd frontend && npm run dev`를 `run_in_background: true`로 실행한다.
4. 새로 띄운 프로세스는 기동 로그를 확인해 정상 시작을 검증한다 (Monitor 도구 또는 `sleep` 없이 짧게 폴링):
   - 백엔드: 로그에 `Started ... Application` 같은 Spring Boot 기동 완료 메시지가 뜨는지 확인.
   - 프론트엔드: 로그에 Vite의 `Local:   http://localhost:8082/` 안내가 뜨는지 확인.
   - 실패(에러 로그, 포트 충돌 등)가 보이면 그대로 사용자에게 보여주고 원인을 설명한다.
5. 완료되면 다음을 사용자에게 안내한다:
   - 백엔드: http://localhost:8081
   - 프론트엔드: http://localhost:8082
   - 두 프로세스가 백그라운드에서 계속 실행 중이라는 점과, 로그를 보고 싶으면 다시 요청하면 된다는 점

## 주의사항

- 이미 같은 포트로 다른 프로세스가 떠 있다면(용도를 알 수 없는 경우) 강제 종료(kill)하지 말고 사용자에게 먼저 확인한다.
- `backend/src/main/resources/application.yml`은 git에 커밋되지 않는 로컬 설정 파일이다(업비트 API 키 등 포함). 파일이 없으면 `application-sample.yml`을 참고해 만들어야 한다고 안내하고, 대신 만들어주지 않는다(민감정보 포함 가능).