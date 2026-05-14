# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 2/2 (100.0%)
- **Function parity:** 34/40 matched (target 47) — 85.0%
- **Class/type parity:** 6/9 matched (target 9) — 66.7%
- **Combined symbol parity:** 40/49 matched (target 56) — 81.6%
- **Average inline-code cosine:** 0.52 (function body across 2 matched files)
- **Average documentation cosine:** 0.90 (doc text across 2 matched files)
- **Cheat-zeroed Files:** 0
- **Critical Issues:** 2 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. lib

- **Target:** `shlex.Lib`
- **Similarity:** 0.49
- **Dependents:** 0
- **Priority Score:** 72205.1
- **Functions:** 12/17 matched
- **Missing functions:** `new`, `deref`, `deref_mut`, `fmt`, `from`
- **Types:** 3/5 matched
- **Missing types:** `Item`, `Target`
- **Tests:** 5/5 matched

### 2. bytes

- **Target:** `bytes.Bytes`
- **Similarity:** 0.56
- **Dependents:** 0
- **Priority Score:** 22704.4
- **Functions:** 22/23 matched (target 30)
- **Missing functions:** `new`
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
