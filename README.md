<img src="https://github.com/Nine-Men-s-Morris/.github/blob/main/images/logo.png?raw=true" width="128px" />

## Nine Men's Morris | 나인멘스모리스

> 온라인 1:1 대전 나인멘스모리스. 서버와 웹을 한 리포지터리에 둔 모노레포다.
>
> 2024년에 팀으로 만든 프로젝트를 2026년에 혼자 다시 정비하고 있다.

<br />

## 구성

| 디렉터리 | 스택 | 배포 |
| --- | --- | --- |
| [server/](server) | Java 21 · Spring Boot 3.5 · STOMP · JPA · MySQL 8 | AWS (예정) |
| [web/](web) | React 18 · Vite · TypeScript · TanStack Query · Tailwind | GitHub Pages (예정) |

규칙 엔진은 Spring 을 모르는 `morris-core` 모듈에 따로 두었다.
판정은 전부 서버가 하고 클라이언트는 그리기만 한다.

<br />

## 실행

**서버** — MySQL 이 먼저 떠 있어야 한다

```bash
cd server
docker compose up -d          # MySQL 만 (앱까지 띄우려면 --profile app)
./gradlew bootRun
```

**웹** — Node 22 를 쓴다 (24 에서는 Yarn PnP 가 깨진다)

```bash
cd web
yarn install
yarn dev
```

<br />

## Playing Images

![](https://github.com/Nine-Men-s-Morris/.github/blob/main/images/0.png?raw=true)

![](https://github.com/Nine-Men-s-Morris/.github/blob/main/images/1.png?raw=true)

![](https://github.com/Nine-Men-s-Morris/.github/blob/main/images/2.png?raw=true)

![](https://github.com/Nine-Men-s-Morris/.github/blob/main/images/3.png?raw=true)

<br />

## 기록

전수조사 결과와 설계 결정을 [server/docs/plans](server/docs/plans) 에 남기고 있다.

| 문서 | 내용 |
| --- | --- |
| [07-architecture-decision.md](server/docs/plans/07-architecture-decision.md) | ADR — 모듈 구조, 플랫폼, DB, HTTPS, 토큰, Redis, 메시징 |
| [14-audit-2026-09.md](server/docs/plans/14-audit-2026-09.md) | 2차 전수조사. 서버를 띄워 놓고 실제로 찔러 본 결과 |
| [15-audit-fixes-2026-09.md](server/docs/plans/15-audit-fixes-2026-09.md) | 무엇이 문제였고 어떻게 고쳤는지, 검증까지 |

<br />

## Developer

2024년 초판은 아래 두 명이 만들었다. 2026년 재정비는 [@ldhbenecia](https://github.com/ldhbenecia) 가 혼자 하고 있다.

| 임동혁 | 맹지승 |
| :---: | :---: |
|  <img width="160px" src="https://avatars.githubusercontent.com/u/77393976?v=4">   |  <img width="160px" src="https://avatars.githubusercontent.com/u/50646827?v=4">   |
| [@ldhbenecia](https://github.com/ldhbenecia)  |  [@js43o](https://github.com/js43o) |
| BE |  FE |
