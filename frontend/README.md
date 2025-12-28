# Upbit AutoTrader Frontend

업비트 API를 이용한 자동매수매도 프로그램의 프론트엔드 애플리케이션입니다.

## 📋 프로젝트 개요

이 프로젝트는 업비트 거래소의 자동매매 기능을 제어하고 모니터링하기 위한 웹 인터페이스를 제공합니다. React와 TypeScript를 기반으로 구축되었으며, 백엔드 API와 통신하여 자동매매 시스템을 제어합니다.

## 🛠 기술 스택

- **React** 19.1.1
- **TypeScript** 5.8.3
- **Vite** 7.1.6
- **ESLint** 9.35.0

## ✨ 주요 기능

### 1. 자동매매 제어
- **자동매매 시작/중지**: 원클릭으로 자동매매 시스템을 제어할 수 있습니다
- **실시간 상태 모니터링**: 5초마다 자동으로 상태를 폴링하여 최신 상태를 표시합니다
- **시각적 상태 표시**: RUNNING/IDLE 배지를 통해 현재 상태를 한눈에 확인할 수 있습니다

### 2. 계정 관리
- **계정 정보 조회**: 업비트 계정의 잔액 및 보유 자산 정보를 조회할 수 있습니다
- **JSON 형식 표시**: 조회된 계정 정보를 구조화된 JSON 형식으로 표시합니다

### 3. 매수 기능
- **균등 분배 매수**: 설정된 자산에 대해 균등 분배 매수를 1회 실행할 수 있습니다

### 4. 활동 로깅
- **실시간 로그**: 모든 주요 액션에 대한 로그를 실시간으로 기록합니다
- **타임스탬프**: 각 로그 항목에 타임스탬프가 포함되어 있습니다
- **로그 제한**: 최대 200개의 로그를 유지하여 성능을 최적화합니다

## 🔌 API 엔드포인트

프론트엔드는 다음 백엔드 API 엔드포인트와 통신합니다:

| 엔드포인트 | 메서드 | 설명 |
|----------|--------|------|
| `/api/upbit/auto/status` | GET | 자동매매 상태 조회 |
| `/api/upbit/auto/start` | POST | 자동매매 시작 |
| `/api/upbit/auto/stop` | POST | 자동매매 중지 |
| `/api/upbit/orders` | POST | 균등 분배 매수 실행 |
| `/api/upbit/accounts` | GET | 계정 정보 조회 |

### API Base URL 설정

기본적으로 `http://localhost:8081`을 사용하며, 환경 변수를 통해 변경할 수 있습니다:

```bash
VITE_API_BASE_URL=http://your-backend-url:port
```

## 🚀 실행 방법

### 1. 의존성 설치

프로젝트 루트 디렉토리에서 다음 명령어를 실행합니다:

```bash
npm install
```

### 2. 환경 변수 설정 (선택사항)

백엔드 API URL이 기본값(`http://localhost:8081`)과 다른 경우, 프로젝트 루트에 `.env` 파일을 생성합니다:

```bash
VITE_API_BASE_URL=http://your-backend-url:port
```

### 3. 개발 서버 실행

```bash
npm run dev
```

개발 서버가 실행되면 브라우저에서 `http://localhost:8082`로 접속할 수 있습니다.

### 4. 프로덕션 빌드

```bash
npm run build
```

빌드된 파일은 `dist/` 폴더에 생성됩니다.

### 5. 빌드 미리보기

```bash
npm run preview
```

프로덕션 빌드를 로컬에서 미리 확인할 수 있습니다.

### 6. 코드 린트

```bash
npm run lint
```

ESLint를 사용하여 코드 스타일을 검사합니다.

## 📦 실행 파일로 패키징하기

프론트엔드를 jar 파일처럼 단일 실행 파일로 패키징할 수 있습니다. 자세한 내용은 [PACKAGING.md](./PACKAGING.md) 파일을 참고하세요.

### 주요 방법

1. **Electron** (권장): 데스크톱 앱으로 패키징
   ```bash
   npm install --save-dev electron electron-builder
   npm run electron:build
   ```

2. **pkg**: Node.js 실행 파일로 패키징
   ```bash
   npm install --save-dev pkg
   npm run package
   ```

3. **Docker**: 컨테이너 이미지로 패키징
   ```bash
   docker build -t upbit-autotrader-frontend .
   ```

자세한 설정 및 사용법은 [PACKAGING.md](./PACKAGING.md)를 참고하세요.

## 📁 프로젝트 구조

```
frontend/
├── src/
│   ├── App.tsx          # 메인 컴포넌트 (모든 기능 포함)
│   ├── main.tsx         # React 앱 진입점
│   ├── index.css        # 전역 스타일
│   └── App.css          # App 컴포넌트 스타일
├── public/              # 정적 파일
│   └── vite.svg
├── package.json         # 의존성 및 스크립트
├── vite.config.ts       # Vite 설정 (포트 8082)
├── tsconfig.json        # TypeScript 설정
├── tsconfig.app.json    # 앱용 TypeScript 설정
├── tsconfig.node.json   # Node용 TypeScript 설정
└── eslint.config.js     # ESLint 설정
```

## 🎨 UI 구조

애플리케이션은 다음과 같은 섹션으로 구성되어 있습니다:

1. **헤더**: 프로젝트 제목과 현재 상태 배지 (RUNNING/IDLE)
2. **상태 섹션**: 현재 자동매매 시스템의 상태를 텍스트로 표시
3. **컨트롤 섹션**: 
   - 균등 분배 매수 버튼
   - 자동매매 시작 버튼
   - 자동매매 중지 버튼
   - 계정 조회 버튼
4. **계정 정보 섹션**: 조회된 계정 정보를 JSON 형식으로 표시
5. **활동 로그 섹션**: 최근 활동 기록을 시간순으로 표시

## ⚠️ 주의사항

1. **백엔드 서버 필요**: 프론트엔드는 백엔드 API 서버가 실행 중이어야 정상적으로 동작합니다. 기본적으로 `http://localhost:8081`에서 실행되는 백엔드를 기대합니다.

2. **CORS 설정**: 백엔드 서버에서 프론트엔드 도메인(`http://localhost:8082`)에 대한 CORS를 허용해야 합니다.

3. **환경 변수**: `.env` 파일을 사용하여 백엔드 API URL을 변경할 수 있습니다. Vite는 `VITE_` 접두사가 붙은 환경 변수만 클라이언트에서 접근 가능합니다.

4. **상태 폴링**: 자동매매 상태는 5초마다 자동으로 폴링됩니다. 컴포넌트가 언마운트될 때 폴링이 자동으로 정리됩니다.

## 📝 개발 스크립트

| 명령어 | 설명 |
|--------|------|
| `npm run dev` | 개발 서버 실행 (포트 8082) |
| `npm run build` | 프로덕션 빌드 생성 |
| `npm run preview` | 빌드된 파일 미리보기 |
| `npm run lint` | ESLint로 코드 검사 |

## 🔧 설정 파일

- **vite.config.ts**: Vite 빌드 도구 설정 (포트 8082로 설정됨)
- **tsconfig.json**: TypeScript 컴파일러 설정
- **eslint.config.js**: ESLint 린터 설정

## 📄 라이선스

이 프로젝트는 개인 사용 목적으로 개발되었습니다.
