# API 명세서 — Stock 도메인

> **Base Path** `/api/stocks`
> **인증 방식** JWT Bearer Token (전 엔드포인트 인증 불필요, 일부 선택)
> **최종 수정** 2026.03.18

---

## 에러 코드 (Stock 도메인)

| 코드 | 설명 | HTTP Status |
|---|---|---|
| `STOCK_001` | 존재하지 않는 종목 | 404 |
| `STOCK_002` | 차트 데이터 없음 (해당 기간) | 404 |
| `STOCK_003` | 외부 시세 API 오류 | 502 |
| `STOCK_004` | 검색어 길이 오류 (1자 미만) | 400 |
| `COMMON_001` | 서버 내부 오류 | 500 |
| `COMMON_002` | 네트워크 오류 / 요청 타임아웃 | 503 |

---

## 엔드포인트 목록

| # | Method | Path | 설명 | 인증 |
|---|---|---|---|---|
| 1 | GET | `/api/stocks` | 종목 리스트 조회 | ❌ |
| 2 | GET | `/api/stocks/search` | 종목 검색 | ❌ |
| 3 | GET | `/api/stocks/search/popular` | 인기 검색어 조회 | ❌ |
| 4 | GET | `/api/stocks/{stockId}` | 종목 기본 정보 조회 | ❌ (로그인 시 `isInterested` 반영) |
| 5 | GET | `/api/stocks/{stockId}/prices` | 가격 차트 + 이벤트 핀 조회 | ❌ |
| 6 | GET | `/api/stocks/{stockId}/company` | 기업 정보 조회 | ❌ |

---

## 1. 종목 리스트 조회

### 기본 정보

| 항목 | 내용 |
|---|---|
| **Method** | `GET` |
| **Path** | `/api/stocks` |
| **인증** | 불필요 (로그인 시 `isInterested` 반영) |
| **관련 요구사항** | #71 (시장 탭), #72 (정렬·필터), #73 (종목 정보 표기), #74 (상세 이동) |
| **관련 DB** | `stocks`, `stock_prices`, `events` |

### Query Parameters

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `market` | String | ❌ | `ALL` | `ALL` \| `KOSPI` \| `KOSDAQ` |
| `sort` | String | ❌ | `TRADING_AMOUNT` | `TRADING_AMOUNT` \| `TRADING_VOLUME` \| `SURGE` \| `DROP` |
| `period` | String | ❌ | `1D` | `REALTIME` \| `1D` \| `1W` \| `1M` \| `3M` \| `6M` |
| `cursor` | String | ❌ | `null` | 이전 응답의 `nextCursor` 값 (최초 요청 시 생략) |
| `size` | Integer | ❌ | `20` | 페이지당 항목 수 (최대 100) |

> `period`는 등락률 기준 기간입니다. `REALTIME`은 당일 실시간 등락률을 반환합니다.
> `cursor`는 `sort`, `period`, `market` 파라미터와 묶인 상태값입니다.
> 세 값 중 하나라도 변경되면 `cursor`를 생략하고 처음부터 요청해야 합니다.
> 유효하지 않은 `cursor` 값이 전달될 경우 `COMMON_003` (400)으로 응답합니다.

### Response

**성공 `200 OK`**
```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "nextCursor": "eyJyYW5rIjoyMSwic3RvY2tJZCI6MzM1fQ==",
    "hasNext": true,
    "size": 20,
    "items": [
      {
        "rank": 1,
        "stockId": 101,
        "ticker": "AMX026",
        "name": "SNXX",
        "market": "KOSPI",
        "logoUrl": "https://...",
        "currentPrice": 62151,
        "priceChange": 7651,
        "changeRate": 14.06,
        "changeDirection": "UP",
        "tradingAmount": 120000000,
        "tradingVolume": 3889,
        "hasEvent": true,
        "eventType": "SURGE",
        "isInterested": false,
      }
    ]
  }
}
```
>`hasNext`가 `false`이면 프론트에서 추가 요청을 멈추면 됩니다.

| 필드 | 타입 | 설명 |
|---|---|---|
| `rank` | Integer | 정렬 기준 순위 |
| `stockId` | Long | 종목 ID |
| `ticker` | String | 종목코드 |
| `name` | String | 종목명 |
| `market` | String | `KOSPI` \| `KOSDAQ` \| `KONEX` |
| `logoUrl` | String | 종목 로고 이미지 URL (null 가능) |
| `currentPrice` | Long | 현재가 (원) |
| `priceChange` | Long | 전일 대비 변동 금액 |
| `changeRate` | Double | 등락률 (%, period 기준) |
| `changeDirection` | String | `UP` \| `DOWN` \| `FLAT` |
| `tradingAmount` | Long | 거래대금 (원) |
| `tradingVolume` | Long | 거래량 (주) |
| `hasEvent` | Boolean | 해당 기간 내 급등락 이벤트 존재 여부 |
| `eventType` | String | `SURGE` \| `DROP` \| null |
| `isInterested` | Boolean | 관심종목 등록 여부 (비로그인 시 `false`) |

**실패**

| 상황 | code | HTTP |
|---|---|---|
| 잘못된 파라미터 값 | `COMMON_003` | 400 |
| 서버 오류 | `COMMON_001` | 500 |

---

## 2. 종목 검색

### 기본 정보

| 항목 | 내용 |
|---|---|
| **Method** | `GET` |
| **Path** | `/api/stocks/search` |
| **인증** | 불필요 |
| **관련 요구사항** | #4 (종목 검색), #5 (검색 결과 정보), #61 (실시간 검색) |
| **관련 DB** | `stocks` |

### Query Parameters

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `q` | String | ✅ | - | 검색어 (종목명 또는 종목코드), 최소 1자 |
| `limit` | Integer | ❌ | `10` | 반환 결과 수 (최대 20) |

### Response

**성공 `200 OK`**

```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "query": "삼성",
    "totalCount": 3,
    "items": [
      {
        "stockId": 11,
        "ticker": "005930",
        "name": "삼성전자",
        "market": "KOSPI",
        "logoUrl": "https://...",
        "currentPrice": 184000,
        "changeRate": -2.07,
        "changeDirection": "DOWN"
      }
    ]
  }
}
```

> 검색 결과가 없을 경우 `items: []`, `totalCount: 0`으로 응답합니다. (에러 아님 — 빈 상태 안내는 프론트에서 처리)

**실패**

| 상황 | code | HTTP |
|---|---|---|
| 검색어 없음 (`q` 미입력) | `STOCK_004` | 400 |
| 서버 오류 | `COMMON_001` | 500 |

---

## 3. 인기 검색어 조회

### 기본 정보

| 항목 | 내용 |
|---|---|
| **Method** | `GET` |
| **Path** | `/api/stocks/search/popular` |
| **인증** | 불필요 |
| **관련 요구사항** | #68 (인기 검색어 상위 5개) |
| **관련 DB** | `stocks`, `stock_prices` (검색 로그는 별도 운영) |

### Response

**성공 `200 OK`**

```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "baseTime": "2026-03-18T13:37:00",
    "items": [
      {
        "rank": 1,
        "stockId": 2,
        "ticker": "000660",
        "name": "SK하이닉스",
        "market": "KOSPI",
        "logoUrl": "https://...",
        "changeRate": 5.05,
        "changeDirection": "UP"
      }
    ]
  }
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `baseTime` | DateTime | 인기 검색어 집계 기준 시각 |
| `items[].rank` | Integer | 인기 순위 (1~5) |
| `changeRate` | Double | 당일 등락률 (%) |

**실패**

| 상황 | code | HTTP |
|---|---|---|
| 서버 오류 | `COMMON_001` | 500 |

---

## 4. 종목 기본 정보 조회

### 기본 정보

| 항목 | 내용 |
|---|---|
| **Method** | `GET` |
| **Path** | `/api/stocks/{stockId}` |
| **인증** | 불필요 (로그인 시 `isInterested` 반영) |
| **관련 요구사항** | #6 (종목 상세 진입) |
| **관련 DB** | `stocks`, `stock_prices`, `interest_stocks` |

### Path Parameter

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `stockId` | Long | 종목 ID |

### Response

**성공 `200 OK`**

```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "stockId": 11,
    "ticker": "005930",
    "name": "삼성전자",
    "market": "KOSPI",
    "sector": "반도체",
    "logoUrl": "https://...",
    "currentPrice": 184000,
    "priceChange": -4000,
    "changeRate": -2.08,
    "changeDirection": "DOWN",
    "todayOhlcv": {
      "open": 183000,
      "high": 186200,
      "low": 178300,
      "volume": 34980000
    },
    "isInterested": true
  }
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `priceChange` | Long | 전일 종가 대비 변동 금액 (원) |
| `changeRate` | Double | 전일 대비 등락률 (%) |
| `changeDirection` | String | `UP` \| `DOWN` \| `FLAT` |
| `todayOhlcv` | Object | 당일 시가·고가·저가·거래량 |
| `isInterested` | Boolean | 관심종목 등록 여부 (비로그인 시 `false`) |

**실패**

| 상황 | code | HTTP |
|---|---|---|
| 존재하지 않는 종목 | `STOCK_001` | 404 |
| 서버 오류 | `COMMON_001` | 500 |

---

## 5. 가격 차트 + 이벤트 핀 조회

### 기본 정보

| 항목 | 내용 |
|---|---|
| **Method** | `GET` |
| **Path** | `/api/stocks/{stockId}/prices` |
| **인증** | 불필요 |
| **관련 요구사항** | #7 (가격 차트), #8 (기간 선택), #9~10 (이벤트 핀), #62 (오늘의 뉴스 핀) |
| **관련 DB** | `stock_prices`, `events`, `news` |

> 차트 렌더링에 필요한 OHLCV 데이터와 이벤트 핀, 오늘의 뉴스 핀을 한 번에 반환합니다.

### Path Parameter

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `stockId` | Long | 종목 ID |

### Query Parameters

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `period` | String | ❌ | `6M` | `1M` \| `3M` \| `6M` \| `1Y` |

### Response

**성공 `200 OK`**

```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "stockId": 11,
    "period": "6M",
    "candles": [
      {
        "date": "2025-10-01",
        "open": 142000,
        "close": 144000,
        "high": 145500,
        "low": 141000,
        "volume": 15200000
      }
    ],
    "eventPins": [
      {
        "eventId": 42,
        "date": "2025-11-26",
        "eventType": "SURGE",
        "changeRate": 17.2
      },
      {
        "eventId": 38,
        "date": "2025-10-14",
        "eventType": "DROP",
        "changeRate": -12.4
      }
    ],
    "newsPins": [
      {
        "date": "2026-03-17",
        "newsCount": 5
      }
    ]
  }
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `candles` | Array | 기간 내 일별 OHLCV 캔들 데이터 |
| `eventPins` | Array | 기간 내 급등락 이벤트 핀 목록 |
| `eventPins[].eventId` | Long | 이벤트 상세 조회에 사용할 ID |
| `eventPins[].eventType` | String | `SURGE` \| `DROP` |
| `eventPins[].changeRate` | Double | 누적 등락률 (%) |
| `newsPins` | Array | 오늘의 뉴스 핀 (뉴스가 수집된 가장 최신 날짜) |
| `newsPins[].newsCount` | Integer | 해당 날짜 수집 뉴스 수 |

**실패**

| 상황 | code | HTTP |
|---|---|---|
| 존재하지 않는 종목 | `STOCK_001` | 404 |
| 해당 기간 데이터 없음 (→ A4 기본 기간 자동 전환) | `STOCK_002` | 404 |
| 서버 오류 | `COMMON_001` | 500 |

---

## 6. 기업 정보 조회

### 기본 정보

| 항목 | 내용 |
|---|---|
| **Method** | `GET` |
| **Path** | `/api/stocks/{stockId}/company` |
| **인증** | 불필요 |
| **관련 요구사항** | 종목 상세 기업정보 탭 |
| **관련 DB** | `stocks` (기본 정보) + **외부 API** (시가총액 등 실시간 데이터) |

> ⚠️ **설계 주의**: 시가총액, 외국인비중, 52주 고저, 실적, 투자자별 매매동향은 `stocks` 테이블에 없습니다.
> 외부 증권 API(KIS API 등)에서 실시간 조회하거나, 별도 수집 배치로 DB에 적재하는 방식이 필요합니다.
> 현재 명세는 외부 API 실시간 조회 기준으로 작성합니다.

### Path Parameter

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `stockId` | Long | 종목 ID |

### Response

**성공 `200 OK`**

```json
{
  "code": "SUCCESS",
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": {
    "stockId": 11,
    "ticker": "005930",
    "name": "삼성전자",
    "market": "KOSPI",
    "logoUrl": "https://...",
    "sectorTags": ["스마트폰", "차세대이동통신", "자율주행", "반도체", "OLED"],
    "marketCap": 1086000000000000,
    "marketRank": 1,
    "totalShares": 5920000000,
    "foreignRatio": 49.66,
    "industryGroup": "하드웨어/IT장비",
    "subIndustry": "반도체/반도체장비",
    "week52Low": 52900,
    "week52High": 228500,
    "overview": "삼성전자는 1969년 설립된 기업으로 반도체, 전자 제품 제조·판매업을 영위하고 있다...",
    "financials": {
      "baseDate": "2025-12",
      "revenue": 3330000,
      "revenueGrowthRate": 10.88,
      "operatingProfit": 430000,
      "operatingProfitGrowthRate": 33.23,
      "netProfit": 450000,
      "netProfitGrowthRate": 31.22
    },
    "investorTrading": {
      "baseDate": "2026-03-18",
      "foreign": -7278,
      "institution": 24834,
      "individual": -16339
    }
  }
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `sectorTags` | Array | 종목 관련 섹터 태그 목록 |
| `marketCap` | Long | 시가총액 (원) |
| `marketRank` | Integer | 시장 내 시가총액 순위 |
| `totalShares` | Long | 총 주식 수 (주) |
| `foreignRatio` | Double | 외국인 보유 비중 (%) |
| `week52Low` | Long | 52주 최저가 (원) |
| `week52High` | Long | 52주 최고가 (원) |
| `overview` | String | 기업 개요 텍스트 |
| `financials.baseDate` | String | 실적 기준 연월 (YYYY-MM) |
| `financials.revenue` | Long | 매출액 (억) |
| `financials.operatingProfit` | Long | 영업이익 (억) |
| `financials.netProfit` | Long | 당기순이익 (억) |
| `investorTrading` | Object | 투자자별 순매수 수량 (주, 당일 기준) |

**실패**

| 상황 | code | HTTP |
|---|---|---|
| 존재하지 않는 종목 | `STOCK_001` | 404 |
| 외부 시세 API 오류 | `STOCK_003` | 502 |
| 서버 오류 | `COMMON_001` | 500 |
