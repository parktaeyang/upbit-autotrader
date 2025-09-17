#!/usr/bin/env bash

set -euo pipefail

# 기본값 설정 (필요 시 환경변수로 덮어쓰기)
HOST="${HOST:-localhost}"
PORT="${PORT:-8081}"
BASE_URL="http://${HOST}:${PORT}"

echo "Using API base: ${BASE_URL}" 

function get_accounts() {
  echo "\n==> GET /api/upbit/accounts"
  curl -sS -X GET "${BASE_URL}/api/upbit/accounts" \
    -H 'Accept: application/json' | jq .
}

# 실제 주문은 비용/위험이 있으니 주석 해제 전 반드시 값 확인하세요.
function post_order_limit_bid() {
  local market="${1:-KRW-BTC}"
  local price="${2:-50000000}"   # 지정가 가격 (KRW)
  local volume="${3:-0.0001}"     # 수량
  echo "\n==> POST /api/upbit/orders (limit bid) ${market} price=${price} volume=${volume}"
  curl -sS -X POST "${BASE_URL}/api/upbit/orders" \
    -H 'Accept: application/json' \
    -d "market=${market}" \
    -d "side=bid" \
    -d "price=${price}" \
    -d "volume=${volume}" \
    -d "ord_type=limit" | jq .
}

function post_order_market_bid_by_krw() {
  local market="${1:-KRW-BTC}"
  local krw="${2:-10000}"        # 사용 금액(KRW)
  echo "\n==> POST /api/upbit/orders (market bid by KRW) ${market} krw=${krw}"
  curl -sS -X POST "${BASE_URL}/api/upbit/orders" \
    -H 'Accept: application/json' \
    -d "market=${market}" \
    -d "side=bid" \
    -d "price=${krw}" \
    -d "ord_type=price" | jq .
}

function post_order_market_ask_by_volume() {
  local market="${1:-KRW-BTC}"
  local volume="${2:-0.0001}"     # 매도 수량
  echo "\n==> POST /api/upbit/orders (market ask by volume) ${market} volume=${volume}"
  curl -sS -X POST "${BASE_URL}/api/upbit/orders" \
    -H 'Accept: application/json' \
    -d "market=${market}" \
    -d "side=ask" \
    -d "volume=${volume}" \
    -d "ord_type=market" | jq .
}

function usage() {
  cat <<EOF
Usage: $(basename "$0") <command> [args]

Commands:
  accounts                                 계정 정보 조회
  bid-limit [market] [price] [volume]      지정가 매수 주문
  bid-market [market] [krw]                시장가 매수(원화금액 지정)
  ask-market [market] [volume]             시장가 매도(수량 지정)

Env:
  HOST, PORT  (default: localhost, 8081)

Examples:
  $(basename "$0") accounts
  $(basename "$0") bid-limit KRW-BTC 50000000 0.0001
  $(basename "$0") bid-market KRW-BTC 10000
  $(basename "$0") ask-market KRW-BTC 0.0001
EOF
}

cmd="${1:-}" || true
case "$cmd" in
  accounts)
    get_accounts ;;
  bid-limit)
    shift || true
    post_order_limit_bid "${1:-KRW-BTC}" "${2:-50000000}" "${3:-0.0001}" ;;
  bid-market)
    shift || true
    post_order_market_bid_by_krw "${1:-KRW-BTC}" "${2:-10000}" ;;
  ask-market)
    shift || true
    post_order_market_ask_by_volume "${1:-KRW-BTC}" "${2:-0.0001}" ;;
  *)
    usage ;;
esac


