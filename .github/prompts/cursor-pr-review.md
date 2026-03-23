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

CRITICAL: You MUST end your final output with EXACTLY the block below. This is machine-parsed by a shell script. Any
deviation — extra text, indentation, code fences, or natural-language summary instead of this block — will cause a parse
failure.

REQUIRED OUTPUT BLOCK (copy exactly, do not paraphrase or reformat):

PR_OVERALL_COMMENT_BEGIN

### 요약

(한 문단 요약)

### 주요 변경점

(변경점 bullet list)

### 개선 사항

(개선 사항이 없으면 이 섹션 전체를 생략)
PR_OVERALL_COMMENT_END
LINE_COMMENTS_BEGIN
[{"path":"<file>","line":<number>,"side":"RIGHT","body":"<Korean comment>\n<!-- cursor-pr-review:v1 -->"}]
LINE_COMMENTS_END
요약: High=<number>, Medium=<number>, Comments=<number>
완료: cursor-pr-review:v1

Rules for the output block:

- The marker lines (`PR_OVERALL_COMMENT_BEGIN`, `PR_OVERALL_COMMENT_END`, `LINE_COMMENTS_BEGIN`, `LINE_COMMENTS_END`)
  MUST appear as standalone lines with NO leading/trailing spaces or characters.
- Do NOT wrap this block in markdown code fences (no ` ``` `).
- Do NOT write a conversational summary before or after this block. The block IS your final output.
- All text inside `PR_OVERALL_COMMENT_BEGIN` ~ `PR_OVERALL_COMMENT_END` and all `body` fields in LINE_COMMENTS MUST be
  written in Korean.
- If no High/Medium issues found: include `LGTM` in the 요약 section and output `[]` for LINE_COMMENTS.
- LINE_COMMENTS must be a valid JSON array. For no issues, output exactly `[]` on a single line.

Quality bar:

- Focus on concrete defects and meaningful risks:
    - correctness bugs
    - security risks
    - performance risks
    - regression risk
    - maintainability issues likely to cause production problems
- Avoid style nitpicks and low-value comments.
- Use related issues context to prioritize domain-specific regression checks when relevant.

