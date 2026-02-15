You are running inside GitHub Actions to review a pull request with Cursor CLI.

Task:

1. Review this PR diff and identify line-level issues of High/Medium severity only.
2. Use Korean for all comment text.
3. Keep workflow non-blocking. Do not fail intentionally.
4. Do not edit files, commit, or push.

Execution context:

- PR metadata is provided above this prompt (PR Number, URL, Head SHA, refs, repository).
- Related issue context (derived from branch name, commit messages, PR text) is also provided above this prompt.
- Environment variables available: `PR_NUMBER`, `PR_URL`, `PR_HEAD_SHA`, `GITHUB_REPOSITORY`.
- `gh` CLI is available and authenticated via `GH_TOKEN`.

Review scope rules:

- Include only code/config file extensions:
  - `.ts`, `.tsx`, `.js`, `.jsx`, `.java`, `.gradle`, `.sql`, `.yml`, `.yaml`
- Exclude:
    - `package-lock.json`
    - `*.md`
    - binary/media files

Commenting policy:

- Severity: only `High` and `Medium`.
- Every comment body must include this marker on a separate line:
    - `<!-- cursor-pr-review:v1 -->`
- Target only changed lines in the PR.
- Line comments are optional. If no High/Medium issues exist, skip line comments entirely.
- Do NOT post comments via GitHub API. Output them as structured JSON to stdout instead.

Suggested command flow:

1. Read files changed:
    - `gh pr diff "$PR_NUMBER" --name-only`
2. Filter by allowed extensions and exclusions.
3. Analyze diff and identify High/Medium issues only.
4. For each issue, confirm target line exists in changed lines of the current diff.
5. Output line comments as JSON to stdout (see output format below). Do NOT call any GitHub API to post comments.

Required output format to stdout (final lines):

- `PR_OVERALL_COMMENT_BEGIN`
- (한국어 마크다운으로 PR 전반 코멘트)
  - 기본 포함:
    - `### 요약`
    - `### 주요 변경점`
  - 선택 포함(개선 사항이 있을 때만):
    - `### 개선 사항`
  - High/Medium 이슈가 없으면 `LGTM` 문구를 반드시 포함
- `PR_OVERALL_COMMENT_END`
- `LINE_COMMENTS_BEGIN`
- JSON array of line comment objects, one per line. Each object:
    - `path`: relative file path
    - `line`: line number in the diff
    - `side`: `"RIGHT"`
    - `body`: Korean comment text including `<!-- cursor-pr-review:v1 -->` marker
- If no issues found, output an empty array `[]`.
- `LINE_COMMENTS_END`
- `요약: High=<number>, Medium=<number>, Comments=<number>`
- `완료: cursor-pr-review:v1`

Formatting requirements (strict):

- Emit marker lines exactly as plain text (no indentation, no extra characters):
  - `PR_OVERALL_COMMENT_BEGIN`, `PR_OVERALL_COMMENT_END`
  - `LINE_COMMENTS_BEGIN`, `LINE_COMMENTS_END`
- Do not wrap the final output section in markdown code fences.
- For `LINE_COMMENTS` section, output a valid JSON array only (for no issues, exactly `[]`).

Quality bar:

- Focus on concrete defects and meaningful risks:
    - correctness bugs
    - security risks
    - performance risks
    - regression risk
    - maintainability issues likely to cause production problems
- Avoid style nitpicks and low-value comments.
- Use related issues context to prioritize domain-specific regression checks when relevant.
