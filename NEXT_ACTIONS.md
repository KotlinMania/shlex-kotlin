# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 2/2 (100.0%)
- **Function parity:** 37/40 matched (target 57) — 92.5%
- **Class/type parity:** 6/9 matched (target 10) — 66.7%
- **Combined symbol parity:** 43/49 matched (target 67) — 87.8%
- **Average inline-code cosine:** 0.60 (function body across 2 matched files)
- **Average documentation cosine:** 0.64 (doc text across 2 matched files)
- **Cheat-zeroed Files:** 0
- **Critical Issues:** 1 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. lib

- **Target:** `shlex.Quote`
- **Similarity:** 0.60
- **Dependents:** 0
- **Priority Score:** 52204.0
- **Functions:** 14/17 matched (target 26)
- **Missing functions:** `deref`, `deref_mut`, `fmt`
- **Types:** 3/5 matched (target 6)
- **Missing types:** `Item`, `Target`
- **Tests:** 5/5 matched

### 2. bytes

- **Target:** `bytes.Shlex`
- **Similarity:** 0.60
- **Dependents:** 0
- **Priority Score:** 12704.0
- **Functions:** 23/23 matched (target 31)
- **Missing functions:** _none_
- **Types:** 3/4 matched
- **Missing types:** `Item`
- **Tests:** 5/5 matched

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

## Next Commands

```bash
# Initialize task queue for systematic porting
cd tools/ast_distance
./ast_distance --init-tasks ../../tmp/shlex/src rust ../../src/commonMain/kotlin/io/github/kotlinmania/shlex kotlin tasks.json ../../AGENTS.md

# Get next high-priority task
./ast_distance --assign tasks.json <agent-id>
```
