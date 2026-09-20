# 로컬 관측 스택

Prometheus + Loki + Grafana + Alloy. **Tempo 와 Mimir 는 넣지 않는다**
— 단일 서비스라 분산 추적의 값어치가 없고, MDC `traceId` 로 충분하다
([07 ADR](../docs/plans/07-architecture-decision.md), [10 §7](../docs/plans/10-logging-and-observability.md)).

## 띄우기

```bash
# 1. 앱. Loki 로 로그를 보내려면 컨테이너여야 한다 (Alloy 가 도커 로그를 읽는다)
docker compose --profile app up -d --build

# 2. 관측 스택
docker compose -f observability/docker-compose.observability.yml up -d
```

| | 주소 |
| --- | --- |
| Grafana | http://localhost:3000 (익명 로그인) |
| Prometheus | http://localhost:9090 |
| Alloy | http://localhost:12345 |

앱을 IDE 로 띄워도 **메트릭은 수집된다**(호스트 8080 을 스크레이프).
다만 **로그는 도커 컨테이너에서만** 수집된다.

## 확인

```bash
# 스크레이프 대상이 up 인지
curl -s 'http://localhost:9090/api/v1/targets?state=active' | jq '.data.activeTargets[].health'

# 게임 메트릭
curl -s 'http://localhost:9090/api/v1/query?query=morris_rooms_active'
```

Grafana 에서 LogQL:

```logql
{container="morris-server"} | json | line_format "{{.traceId}} | {{.userId}} | {{.message}}"
{container="morris-server"} | json | traceId="a4bbe912"
```

에러 응답의 `traceId` 를 그대로 넣으면 그 요청의 로그가 나온다.

## 라벨을 적게 두는 이유

`traceId` / `userId` 는 라벨로 만들지 않는다. 값마다 스트림이 생겨 Loki 가 망가진다.
라벨은 `container` 와 `level` 만 두고, 나머지는 `| json` 으로 조회한다.
