# 250종목 저장 명세 (03_stock.md 기반)

KOSPI 200 + KOSDAQ 50 = **250종목**을 **어떻게 저장할지**, **저장할 때 어떤 값이 필요한지**를 03_stock.md API 명세에 맞춰 테이블·컬럼·값 출처까지 나열한 문서입니다.

---

## 1. 저장 대상 요약

| 순서 | 테이블 | 저장 대상 | 행 수 감각 |
|------|--------|-----------|------------|
| 1 | **stocks** | 250개 종목 마스터 | 250행 |
| 2 | **stock_prices** | 종목별·주기별 OHLCV (일/주/월/년) | 종목×주기×봉 수 (예: 125만 행) |
| 3 | **events** | 구간 내 급등/급락 이벤트 (차트 이벤트 핀) | 이벤트 발생일별 |
| 4 | **interest_stocks** | 사용자별 관심종목 | 사용자×관심 종목 수 |

응답 시 **currentPrice, changeRate, tradingAmount** 등은 저장값 + **실시간/캐시**로 보강합니다. (저장 명세에서는 “어떤 값을 DB/시드에 둘지”만 다룹니다.)

---

## 2. stocks (종목 마스터) — 250종목 저장

### 2.1 역할

- 03_stock **§1 종목 리스트, §2 검색, §3 인기 검색어, §4 종목 기본 정보, §5 차트, §6 기업 정보**에서 공통으로 쓰는 **종목 식별·기본 정보**.
- 250종목 = **코스피 200 + 코스닥 50**을 이 테이블에 한 번만 등록해 두고, 이후 API는 전부 `stockId`(PK) 또는 `ticker`로 이 테이블을 참조합니다.

### 2.2 저장 방식

- **초기 1회**: 시드 데이터(CSV/JSON) 또는 관리자 API로 **250행 일괄 INSERT**.
- **이후**: 종목 추가/수정이 있을 때만 UPDATE 또는 INSERT.  
- LS Open API 호출 시에는 이 테이블의 `ticker`, `market`(→ LS exchgubun)를 사용합니다.

### 2.3 컬럼 정의 — 저장 시 필요한 값

| 컬럼명 | 타입 | 필수 | 03_stock 응답 필드 | 값 출처 | 비고 |
|--------|------|------|--------------------|---------|------|
| **id** | BIGINT (PK, AUTO) | ✓ | 모든 응답의 `stockId` | 우리가 부여 (시드 시 1~250 또는 별도 ID) | 03_stock 응답의 `stockId`와 동일 |
| **ticker** | VARCHAR(6) | ✓ | §1,2,3,4,5,6 `ticker` | 시드: 종목코드 6자 (예: 005930) | LS API `shcode`와 동일 |
| **name** | VARCHAR(100) | ✓ | §1,2,3,4,6 `name` | 시드: 종목명 (예: 삼성전자) |  |
| **market** | VARCHAR(10) | ✓ | §1,2,3,4,6 `market` | 시드: `KOSPI` \| `KOSDAQ` \| `KONEX` | LS `exchgubun` 매핑 (0→KOSPI, 1→KOSDAQ 등) |
| **logo_url** | VARCHAR(500) |  | §1,2,3,4,6 `logoUrl` | 시드 또는 별도 수집; null 가능 |  |
| **sector** | VARCHAR(50) |  | §4,6 `sector` | 시드 또는 외부 API; null 가능 | 종목 상세·기업정보용 |
| **display_order** | INT |  | (리스트 정렬용) | 시드: 코스피200/코스닥50 순위 등; null 가능 | ORDER BY에 사용, 없으면 ticker 순 |
| **created_at** | TIMESTAMP |  | - | INSERT 시 CURRENT_TIMESTAMP |  |
| **updated_at** | TIMESTAMP |  | - | UPDATE 시 갱신 (선택) |  |

### 2.4 03_stock.md에서 쓰이는 위치

| 엔드포인트 | stocks에서 사용하는 컬럼 |
|------------|--------------------------|
| GET /api/stocks | id→stockId, ticker, name, market, logo_url→logoUrl (+ 시세/캐시로 currentPrice 등) |
| GET /api/stocks/search | id→stockId, ticker, name, market, logo_url (+ 시세/캐시) |
| GET /api/stocks/search/popular | id→stockId, ticker, name, market, logo_url (+ 시세/캐시, 순위는 검색 로그 등) |
| GET /api/stocks/{stockId} | id, ticker, name, market, sector, logo_url (+ 시세/캐시, interest_stocks→isInterested) |
| GET /api/stocks/{stockId}/prices | id→stockId (candles는 stock_prices, eventPins는 events) |
| GET /api/stocks/{stockId}/company | id, ticker, name, market, logo_url, sector (+ 명세 주석대로 외부 API로 시가총액 등) |

### 2.5 시드 데이터 예시 (250종목)

저장 시 최소한 아래 값은 필요합니다. (나머지는 null 허용 후 추후 보강 가능.)

| id | ticker | name | market | logo_url | sector | display_order |
|----|--------|------|--------|----------|--------|---------------|
| 1 | 005930 | 삼성전자 | KOSPI | (URL 또는 null) | 반도체 | 1 |
| 2 | 000660 | SK하이닉스 | KOSPI | ... | 반도체 | 2 |
| ... | ... | ... | ... | ... | ... | ... |
| 250 | (코스닥 50번째) | ... | KOSDAQ | ... | ... | 250 |

**값 출처**: 코스피200/코스닥50 종목 목록은 거래소 공개 목록, LS 마스터 API(주식마스터조회API용) 또는 사내 시드 파일에서 취득 후 위 형식으로 저장.

### 2.6 batch 모듈 — t9945로 `stocks` 적재

- **실행**: `:batch` 스프링 부트 앱. 기본은 부팅 시 적재하지 않음.
  - 적재 실행: `stock.master.load-at-startup=true` (예: `java -jar batch.jar --stock.master.load-at-startup=true` 또는 `SPRING_APPLICATION_JSON` / 프로필 yml).
- **LS API**: `POST {baseUrl}/stock/market-data`, 헤더 `tr_cd=t9945`, `authorization: Bearer {LS_ACCESS_TOKEN}`, 연속조회는 `tr_cont` / `tr_cont_key` (응답 헤더 또는 JSON 본문).
- **요청 본문**: `t9945InBlock.gubun` — `"1"` KOSPI 전 종목, `"2"` KOSDAQ 전 종목. 초당 2건 제한을 위해 호출 간 최소 500ms 간격(`ls.openapi.min-interval-ms`).

- **환경변수**: `LS_ACCESS_TOKEN`, `LS_OPENAPI_BASE_URL`, DB는 기존 `DB_*`와 동일 (`.env.example` 참고).

---

## 3. stock_prices (시세 이력) — 저장할 값

### 3.1 역할

- 03_stock **§5 가격 차트**의 `candles` (일별 OHLCV).
- §1 리스트의 `tradingAmount`, `tradingVolume`, §4의 `todayOhlcv`, 현재가/등락률 계산의 기반이 되는 **과거·당일 봉** 데이터.

### 3.2 저장 방식

- **일봉**: 사용자가 해당 종목·기간 차트를 최초 요청할 때 **LS t8451** 연속조회 → 조회 결과를 **일괄 INSERT**.  
  또는 **배치**: 250종목 일봉을 장 마감 후 주기적으로 t8451 호출해 INSERT/UPDATE.
- **주봉/월봉/년봉**: t8451으로 주기별(gubun 3/4/5) 조회 후 동일 스키마로 저장합니다. (일봉만 저장하고 재집계하는 방식은 선택사항)
- **유일 제약**: (stock_id, trade_date, period) 또는 (ticker, trade_date, period) 당 1행.

### 3.3 컬럼 정의 — 저장 시 필요한 값

| 컬럼명 | 타입 | 필수 | 03_stock 응답 필드 | 값 출처 | 비고 |
|--------|------|------|--------------------|---------|------|
| **id** | BIGINT (PK, AUTO) | ✓ | - | DB 자동 생성 |  |
| **stock_id** | BIGINT (FK→stocks.id) | ✓ | §5 candles는 stock 단위 | stocks.id | ticker로 저장해도 됨(조회 시 stocks와 조인) |
| **trade_date** | DATE | ✓ | §5 `candles[].date` | LS t8451 응답 일자 (YYYY-MM-DD) |  |
| **period** | VARCHAR(10) 또는 ENUM | ✓ | §5 period는 “주기” | `DAILY` \| `WEEKLY` \| `MONTHLY` \| `YEARLY` | t8451 gubun 2/3/4/5에 대응 |
| **open** | BIGINT | ✓ | §5 `candles[].open` | t8451 시가 (원) |  |
| **high** | BIGINT | ✓ | §5 `candles[].high` | t8451 고가 (원) |  |
| **low** | BIGINT | ✓ | §5 `candles[].low` | t8451 저가 (원) |  |
| **close** | BIGINT | ✓ | §5 `candles[].close` | t8451 종가 (원) |  |
| **volume** | BIGINT | ✓ | §5 `candles[].volume` | t8451 거래량 (주), LS `jdiff_vol` 등 |  |

**인덱스**: (stock_id, period, trade_date) 또는 (ticker, period, trade_date) — 차트·기간 조회용.

### 3.4 03_stock.md와의 매핑

| 03_stock 필드 | stock_prices 컬럼 |
|---------------|-------------------|
| §5 candles[].date | trade_date |
| §5 candles[].open | open |
| §5 candles[].high | high |
| §5 candles[].low | low |
| §5 candles[].close | close |
| §5 candles[].volume | volume |

§1의 `tradingAmount`(거래대금), `tradingVolume`(거래량)은 **해당 기간(period)의 최신 봉 또는 당일**의 close×volume, volume로 계산하거나, 실시간 API(t8407/t1102) 값으로 보강.  
§4의 `todayOhlcv`(open, high, low, volume)는 **당일 일봉 1행**이 있으면 그대로, 없으면 t1102 등으로 채움.

### 3.5 값 출처 요약

| 값 | 출처 |
|----|------|
| stock_id / ticker | stocks 테이블 (종목 식별) |
| trade_date, open, high, low, close, volume | **LS t8451** (통합 주식챠트 일주월년 API용) 응답 매핑 |
| period | 우리가 고정 (DAILY 등), t8451 gubun과 1:1 매핑 |

---

## 4. events (이벤트 핀) — 저장할 값

### 4.1 역할

- 03_stock **§1** 리스트의 `hasEvent`, `eventType`.
- **§5** 가격 차트의 `eventPins` (eventId, date, eventType, changeRate) — 차트 위 “급등/급락” 핀.

### 4.2 저장 방식

- **배치 또는 도메인 로직**: `stock_prices`의 일봉 기간별 수익률(또는 N일 대비 등락률)을 계산해, **임계치(예: +10% 이상 → SURGE, -10% 이하 → DROP)** 를 넘는 날을 이벤트로 판단 후 **INSERT**.
- 실시간 재계산은 부담이 크므로, **DAILY/WEEKLY/MONTHLY/YEARLY 배치가 끝난 뒤** 해당 주기(period)에 대해 이벤트를 계산·저장하는 방식을 권장.

### 4.3 컬럼 정의 — 저장 시 필요한 값

| 컬럼명 | 타입 | 필수 | 03_stock 응답 필드 | 값 출처 | 비고 |
|--------|------|------|--------------------|---------|------|
| **id** | BIGINT (PK, AUTO) | ✓ | §5 `eventPins[].eventId` | DB 자동 생성 |  |
| **stock_id** | BIGINT (FK→stocks.id) | ✓ | (종목별 이벤트) | stocks.id |  |
| **event_date** | DATE | ✓ | §5 `eventPins[].date` | 이벤트 발생일 (급등/급락 기준일) |  |
| **event_type** | VARCHAR(10) | ✓ | §5 `eventPins[].eventType`, §1 `eventType` | `SURGE` \| `DROP` |  |
| **change_rate** | DOUBLE | ✓ | §5 `eventPins[].changeRate` | 해당 구간 누적 등락률 (%) | 예: 전일 대비, 또는 N일 대비 |
| **base_period** | VARCHAR(10) |  | (선택) | 이벤트 계산 기준 주기(예: DAILY 등) |  |

---

## 4.5 LS t8451 / t1102 필드 매핑 (저장값/응답값)

### 4.5.1 t8451 (통합 주식챠트: 일/주/월/년) → `stock_prices`

#### 요청 (헤더/바디)에서 필요한 값
- `authorization` (또는 `LS_ACCESS_TOKEN`)에 Bearer 토큰
- `tr_cd`: `t8451`
- `tr_cont`: 연속조회 여부 (`Y`이면 `tr_cont_key` 필요)
- `tr_cont_key`: 연속조회 키 (연속조회일 때만)
- `t8451InBlock.shcode`: 종목코드 (예: `005930`)
- `t8451InBlock.gubun`: 주기구분 `2=일`, `3=주`, `4=월`, `5=년` (03_stock §5 period와 1:1 매핑)
- `t8451InBlock.qrycnt`: 최대 500 (우리는 500 사용)
- `t8451InBlock.edate`: `99999999` 로 “가능한 과거~최근” 연속조회
- `t8451InBlock.cts_date`: 연속조회용 (응답의 `t8451OutBlock.cts_date`를 다음 요청으로 반영)
- `t8451InBlock.comp_yn`: `N` (비압축)
- `t8451InBlock.sujung`: `Y` (수정주가 적용: 액면분할 등 보정)
- `t8451InBlock.exchgubun`: 거래소구분코드 (`K`, `N`, `U` 중 값에 따라 KRX/NXT/통합 취급)

#### 응답(t8451OutBlock1)에서 저장하는 값
- `date` (YYYYMMDD 문자열) → `trade_date` (YYYY-MM-DD로 변환)
- `open`, `high`, `low`, `close` → `stock_prices.open/high/low/close`
- `jdiff_vol` → `stock_prices.volume` (거래량)
- `value` → (선택) t8451 기준 거래대금(현재 DB에 컬럼이 없으면 저장하지 않고, 필요 시 추가 가능)
- `sign` → (선택) 상승/하락 판단 또는 보조지표로 활용 가능

> 추가로, 현재 구현은 `up = (close >= open)`으로 거래량 히스토그램 색을 정합니다. (저장에는 `up` 필드를 별도로 두지 않고 응답 시 계산)

### 4.5.2 t1102 (주식현재가 시세) → `todayOhlcv`, `currentPrice/priceChange/changeRate`

#### 요청 (헤더/바디)에서 필요한 값
- `authorization` (또는 `LS_ACCESS_TOKEN`)에 Bearer 토큰(필요 시)
- `tr_cd`: `t1102`
- `t1102InBlock.shcode`: 종목코드
- `t1102InBlock.exchgubun`: 거래소구분코드 (`K`/`N`/`U` 중 값에 따라 KRX/NXT/통합 취급)

#### 응답(t1102OutBlock)에서 사용하는 값
- `price` → `currentPrice`
- `change` → `priceChange` (전일 대비 변동 금액)
- `diff` → `changeRate` (전일 대비 등락률)
- `sign` → `changeDirection` (`UP`/`DOWN`/`FLAT` 매핑에 사용)
- `open`, `high`, `low` → `todayOhlcv.open/high/low`
- `volume` → `todayOhlcv.volume`

### 4.6 03_stock.md와의 매핑

| 03_stock 필드 | events 컬럼 |
|---------------|--------------|
| eventPins[].eventId | id |
| eventPins[].date | event_date |
| eventPins[].eventType | event_type |
| eventPins[].changeRate | change_rate |

§1 리스트의 `hasEvent`, `eventType`은 해당 종목·기간(period)에 맞는 **events**가 있는지 조회해 Boolean/문자열로 반환.

### 4.7 값 출처 요약

| 값 | 출처 |
|----|------|
| stock_id | stocks.id |
| event_date, event_type, change_rate | **stock_prices 기반 계산** (기간별 수익률 + 임계치) 또는 별도 규칙 |

---

## 5. interest_stocks (관심종목) — 저장할 값

### 5.1 역할

- 03_stock **§1** 리스트의 `isInterested`.
- **§4** 종목 기본 정보의 `isInterested`.
- 로그인 사용자별 “관심 종목” 목록.

### 5.2 저장 방식

- 사용자가 “관심종목 추가/해제” 시 **INSERT 또는 DELETE**.
- 조회: 해당 사용자(또는 세션)가 해당 stock_id를 가지고 있는지 여부로 `isInterested` 반환.

### 5.3 컬럼 정의 — 저장 시 필요한 값

| 컬럼명 | 타입 | 필수 | 03_stock 응답 필드 | 값 출처 | 비고 |
|--------|------|------|--------------------|---------|------|
| **user_id** | BIGINT (FK) 또는 VARCHAR | ✓ | (로그인 사용자) | 인증 후 사용자 식별자 | 비로그인 시 테이블 조회 안 함 → isInterested=false |
| **stock_id** | BIGINT (FK→stocks.id) | ✓ | - | stocks.id |  |
| **created_at** | TIMESTAMP |  | - | INSERT 시 |  |

**유일 제약**: (user_id, stock_id) 당 1행.

### 5.4 값 출처 요약

| 값 | 출처 |
|----|------|
| user_id | 로그인 세션/토큰에서 추출 |
| stock_id | 프론트에서 선택한 종목의 stockId (stocks.id) |

---

## 6. news (선택) — §5 newsPins용

### 6.1 역할

- 03_stock **§5** `newsPins` (date, newsCount).

### 6.2 저장 방식

- 뉴스 수집 배치 또는 외부 API로 **날짜·종목별 수집 건수**를 저장.
- §5 응답 시 해당 기간 내 데이터가 있는 날짜만 newsPins로 반환.

### 6.3 컬럼 정의 (선택)

| 컬럼명 | 타입 | 필수 | 03_stock 응답 필드 | 값 출처 |
|--------|------|------|--------------------|---------|
| **id** | BIGINT (PK) | ✓ | - | DB 자동 |
| **stock_id** | BIGINT (FK) | ✓ | - | stocks.id |
| **news_date** | DATE | ✓ | newsPins[].date | 수집일 |
| **news_count** | INT | ✓ | newsPins[].newsCount | 해당 날짜 수집 뉴스 건수 |

---

## 7. 저장 순서 및 의존 관계

1. **stocks** 먼저 저장 (250행). — 다른 테이블이 모두 stock_id(FK)로 참조.
2. **stock_prices**: stocks 존재한 뒤, t8451로 종목·주기별 조회 후 INSERT.
3. **events**: stock_prices(일봉)가 쌓인 뒤, 기간별 수익률 계산·임계치 적용 후 INSERT.
4. **interest_stocks**: 사용자 행동 시점에 INSERT/DELETE (stocks만 있으면 가능).
5. **news**: 별도 수집 파이프라인으로 INSERT (선택).

---

## 8. 03_stock 응답 필드 ↔ 저장소 매핑 요약

| 03_stock 응답 필드 | 저장 테이블·컬럼 | 비고 |
|--------------------|-------------------|------|
| stockId | stocks.id |  |
| ticker | stocks.ticker |  |
| name | stocks.name |  |
| market | stocks.market |  |
| logoUrl | stocks.logo_url |  |
| sector | stocks.sector |  |
| currentPrice, priceChange, changeRate, changeDirection | 캐시/실시간(t1102·t8407) 또는 stock_prices 최신 봉 기반 계산 | DB에 “현재가 전용” 컬럼 두지 않아도 됨(선택 시 스냅샷 테이블 가능) |
| tradingAmount, tradingVolume | stock_prices(해당 기간 봉) 또는 캐시/실시간 |  |
| hasEvent, eventType | events 조회 |  |
| isInterested | interest_stocks 조회 |  |
| todayOhlcv | stock_prices(당일 일봉) 또는 t1102 |  |
| candles | stock_prices (period=DAILY 등, 기간 필터) |  |
| eventPins | events (해당 종목·기간) |  |
| newsPins | news (해당 종목·기간) |  |
| §6 기업정보 대부분 | 명세 주석대로 외부 API 또는 별도 테이블/배치 | stocks에는 sector 등 기본만 |

이 명세대로 저장하면 03_stock.md의 모든 엔드포인트가 요구하는 데이터를 내부 DB에서 제공할 수 있고, **저장 시 필요한 값**은 위 컬럼 정의와 값 출처에 정리된 것만 있으면 됩니다.
