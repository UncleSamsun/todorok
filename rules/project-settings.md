# TODOROK 프로젝트 설정

이 폴더는 [SyncDoc](https://github.com/UncleSamsun/syncdoc)의 공통 문서 규칙을 이 프로젝트에 고정 버전으로 적용한 것이다. 이 파일만 이 프로젝트의 값이고, 나머지 파일은 원본을 그대로 복사했다. 규칙 자체를 고치려면 원본 저장소에서 고치고 이 폴더를 새 버전으로 다시 가져온다. 여기서 복사본을 직접 고치지 않는다.

복사한 파일 안의 `../docs/...`, `../tools/...` 상대 링크는 원본 저장소 기준이며 이 저장소에서는 열리지 않는다. 검사기는 링크를 검사하지 않으므로 판정에는 영향이 없다.

## 저장소

- GitHub: https://github.com/UncleSamsun/todorok
- 릴리스 기준 브랜치: `main`
- 개발 통합 브랜치: `develop`
- 작업 브랜치·이슈·PR·커밋·기록 정책은 이 저장소의 [CONTRIBUTING.md](../CONTRIBUTING.md)를 따른다. 복사한 [GitHub 협업 규칙](github-collaboration.md)과 어긋나는 항목은 CONTRIBUTING.md가 우선한다. 두 문서의 차이를 이 프로젝트에서 합치는 일은 아직 하지 않았다.

## 적용 규칙 버전

원본은 `UncleSamsun/syncdoc` 저장소의 `dev` 브랜치 커밋 `db1a2e4`(2026-09-10)의 `rules/` 폴더다. 아래 파일을 그 revision에서 그대로 복사했다. 규칙을 고치면 이 표의 버전과 원본 revision을 함께 올린다.

| 규칙 | 버전 | 상태 |
|---|---|---|
| [GitHub 협업](github-collaboration.md) | 2026-09-09 | 활성 (CONTRIBUTING.md와 충돌 시 CONTRIBUTING.md 우선) |
| [ID와 문서 참조](identity-and-references.md) | 2026-09-09 | 활성 |
| [SDD 역할과 승인 범위](sdd-workflow.md) | 2026-09-09 | 활성 |
| [문서 작성 규칙](spec-writing.md) | 2026-09-10 | 활성 |
| [포맷 정의](spec-format.json) | 2026-09-10 | 활성 (검사기와 SyncDoc이 읽는 기계 정본) |
| [검증 규칙](validation.md) | 2026-09-10 | 활성 (검사기는 원본 저장소에서 실행) |

검사기는 이 저장소에 복사하지 않는다. 원본 저장소를 옆에 받아 두고 이 저장소 루트를 인자로 준다.

```bash
python ../syncdoc/tools/spec-validator/validate.py .
```

## 적용 Spec

이 프로젝트가 적용하는 문서 종류다. 종류와 필수 내용의 정의는 [문서 작성 규칙](spec-writing.md) §3과 [포맷 정의](spec-format.json)에 있다. [검증 규칙](validation.md) C1이 이 표를 읽어 필수 문서 존재를 판정하므로 표의 형식을 바꾸지 않는다.

2026-09-10 기준으로 모든 종류를 보류한다. 기존 `docs/PRD.md`·`docs/plans`·`docs/superpowers`의 문서는 이 규칙 이전에 작성되어 frontmatter와 문서 ID가 없다. 어느 문서를 어느 종류로 이관할지 정한 뒤 종류별로 `적용`으로 바꾼다. 보류인 동안에도 검사기는 기존 문서의 frontmatter 부재를 오류로 보고한다. 이 오류 목록이 이관 작업의 시작점이다.

| type | 적용 | 사유 |
|---|---|---|
| prd-overview | 보류 | 기존 docs/PRD.md에서 개요를 분리하는 이관 계획 확정 전 |
| prd-requirements | 보류 | 기존 docs/PRD.md의 요구를 REQ 단위로 옮기는 이관 계획 확정 전 |
| ui-conventions | 보류 | 루트 DESIGN.md를 docs/로 옮길지 확정 전 |
| ui-screens | 보류 | 화면 명세를 PRD에서 분리할지 확정 전 |
| tech-overview | 보류 | 기존 plans의 기술 결정을 개요로 묶는 이관 계획 확정 전 |
| tech-interface | 보류 | contracts/ OpenAPI와의 역할 분담 확정 전 |
| tech-data | 보류 | 서비스별 스키마 문서화 범위 확정 전 |
| tech-ops | 보류 | infra/ Compose 구성의 문서화 범위 확정 전 |
| tasks | 보류 | docs/plans의 계획을 TASK 단위로 옮기는 이관 계획 확정 전 |
| proposal | 보류 | 기존 superpowers/specs를 proposal로 분류할지 확정 전 |
| record | 보류 | 기존 문서 이관 후 결정 기록부터 적용 |
| guide | 보류 | CONTRIBUTING.md와 README.md는 검사 대상 밖이며 docs/ 안내 문서는 아직 없음 |

## 추가로 정할 설정

- 기존 문서의 종류 분류와 frontmatter 부여 순서
- 이 저장소의 CONTRIBUTING.md와 복사한 GitHub 협업 규칙의 차이 정리
- 검사기 실행을 CI에 연결할지 여부
