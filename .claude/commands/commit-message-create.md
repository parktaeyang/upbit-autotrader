---
description: 프로젝트 커밋 규칙(YY.MM.DD | scope #n| type: message)에 맞춰 커밋 메시지를 만들고 커밋합니다
argument-hint: [type] <커밋 요약 내용>
---

## 목적

이 저장소의 커밋 메시지 규칙은 다음 형식을 따른다:

```
YY.MM.DD | {scope} #{n}| {type}: {message}
```

- `YY.MM.DD`: 오늘 날짜 (예: 26.06.14)
- `{scope}`: 변경된 파일 위치에 따라 자동 결정
  - `backend/` 이하만 변경 → `back`
  - `frontend/` 이하만 변경 → `front`
  - 둘 다 변경 → `back,front`
- `#{n}`: **오늘 날짜 + 해당 scope 기준**으로 이어지는 순번 (1부터 시작, scope별 독립 카운트)
- `{type}`: `feat` / `fix` / `refactor` / `chore` / `docs` / `test` / `style` / `perf` / `build` / `ci` 중 하나. 인자로 안 주면 `feat` 기본값
- `{message}`: 한글 요약 (사용자가 입력한 내용 그대로 사용, 과장하거나 재해석하지 말 것)

예시 (실제 로그):
```
26.06.14 | back #1| feat: 조회목록 불필요 코인 정리
26.01.04 | back #3| feat: RSI 조건변경 - 매수 30 -> 40 - 매도 70 -> 65
26.01.04 | front #1| feat: 자동매매 화면로그출력 추가
25.12.29 | back,front #1 | feat: 실행 키 확인
```

## 사용자 입력

`$ARGUMENTS`

- 첫 단어가 `feat/fix/refactor/chore/docs/test/style/perf/build/ci` 중 하나면 그것을 `{type}`으로, 나머지를 `{message}`로 사용한다.
- 아니면 전체를 `{message}`로 사용하고 `{type}`은 `feat`로 기본 설정한다.
- `$ARGUMENTS`가 비어 있으면, 커밋 요약 내용을 사용자에게 물어본다 (임의로 지어내지 말 것).

## 실행 절차

1. `git status`로 현재 staged/unstaged/untracked 변경사항을 확인한다.
   - 아무 변경사항도 없으면 커밋할 것이 없다고 안내하고 종료한다.
2. **scope 판단**: 변경된 파일(staged가 있으면 staged 기준, 없으면 전체 워킹트리 변경 기준) 경로를 확인한다.
   - `backend/` 하위 파일만 있으면 `back`
   - `frontend/` 하위 파일만 있으면 `front`
   - 둘 다 있으면 `back,front`
   - 그 외 경로(루트 설정파일 등)만 있으면 scope 없이 `#{n}` 형태 사용 (과거 로그의 `#1 | ...` 처럼)
3. **오늘 날짜**를 `date '+%y.%m.%d'`로 구한다.
4. **순번(n) 계산**: `git log --oneline` 에서 오늘 날짜 문자열로 시작하고 같은 scope 라벨을 포함하는 커밋 개수를 세어 +1 한다.
   - 예: scope가 `back`이면 `YY.MM.DD | back #` 패턴을 포함하는 오늘 커밋 개수를 센다.
   - scope가 없는 경우는 `YY.MM.DD | #` 패턴으로 센다.
5. 최종 메시지를 조립한다: `{date} | {scope} #{n}| {type}: {message}` (scope가 없으면 `{date} | #{n}| {type}: {message}`).
6. 조립한 메시지를 사용자에게 보여주고, staged된 변경사항이 없다면 관련 파일을 `git add`로 스테이징한다 (스테이징 대상 파일 목록을 명시적으로 보여줄 것).
7. `git commit -m "$(cat <<'EOF' ... EOF)"` 형태의 heredoc으로 커밋을 생성한다. 이 프로젝트의 기존 커밋 로그에는 Co-Authored-By 트레일러가 없으므로, 본문 규칙을 유지하기 위해 커밋 메시지에는 트레일러를 추가하지 않는다.
8. 커밋 후 `git log -1`로 결과를 확인해 사용자에게 보여준다.

## 주의사항

- 이미 다른 사람이 만든 미완성 작업(untracked 파일 등)이 섞여 있을 수 있으니, 스테이징 전에 어떤 파일이 포함되는지 반드시 사용자에게 보여줄 것.
- 변경 내용이 명백히 여러 주제로 나뉘어 보이면(예: 백엔드 기능 추가 + 프론트 무관한 수정), 하나의 커밋으로 합칠지 나눌지 사용자에게 확인한다.
- `{message}`는 사용자가 준 표현을 최대한 그대로 쓰고, 임의로 기술적 설명을 덧붙이지 않는다.