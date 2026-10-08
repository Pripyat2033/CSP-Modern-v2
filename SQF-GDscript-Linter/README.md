# 🔧 SQF & GDScript Linter - Compiler-like Tooling

## What This Is

A **"compiler-like" static analysis tool** for Arma 3 (SQF) and Godot (GDScript) languages.

Just like Java compiles your code before running, this linter will:

- ✅ Parse syntax errors (missing semicolons, invalid types)
- ✅ Check semantic errors (method signatures, parameter mismatches)  
- ✅ Report ALL issues at once (not just one by one)
- ✅ Give EXACT line numbers and error descriptions

## Why Build This?

### Current Problem

SQF Runtime Error: "Line 47: error in #1090"
    ↓ Could be ANYTHING!
    Guessing where the bug is...

### What We Want

Lint Output: "Error at Line 47, Column 5: Expected function '#call', found 'invalid_operator'"
    ↓ EXACT LOCATION + TYPE OF ERROR
    One fix solves it!

---

## Architecture (Like Java Compilation)

┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│  Source     │      │  Parser     │      │  Analyzer   │
│  Code       │ ───► │  Grammar    │ ───► │  Rules      │
└─────────────┘      └─────────────┘      └─────────────┘
                                              ↓
                                      ┌─────────────┐
                                      │  Report     │
                                      │  Generator  │
                                      └─────────────┘

---

## Features We'll Build

### Phase 1: Syntax Validation (Easy)

- [x] Parse file structure
- [ ] Check for missing semicolons (SQF)
- [ ] Check for invalid function calls
- [ ] Validate basic types

### Phase 2: Semantic Analysis (Medium)

- [ ] Method signature matching
- [ ] Parameter type checking
- [ ] Import/package validation
- [ ] Global variable scope checks

### Phase 3: Runtime Simulation (Hard)

- [ ] Simulate game state changes
- [ ] Check array bounds at runtime points
- [ ] Validate object references

---

## Technologies to Use

### For SQF Linter

- **Language:** Python (easiest for rapid development)
- **Parser:** `lark` or `ANTLR` (grammar parser generator)
- **VSCode Integration:** Language Server Protocol (LSP) extension

### For GDScript Linter

- Use Godot's built-in type hints + custom analyzer
- OR build standalone Python-based linter
- VSCode GDscript extension already has some features

---

## Example Lint Output

```bash
$ sqf-lint check mission.sqf

SQF Syntax Check Report
━━━━━━━━━━━━━━━━━━━━━━━━

Errors: 3, Warnings: 2, Info: 5
━━━━━━━━━━━━━━━━━━━━━━━━

[ERROR] Line 47, Column 5: Expected function call '#call', found 'invalid_operator'
        |
      46 |     _value = count units;
    47 |     [invalid_op, something];  ← Missing proper syntax
      48 |     

[ERROR] Line 123, Column 1: Function 'doesNotExist' not found
        |
    122 |     if (someCondition);
    123 |     doesNotExist();           ← Method signature mismatch
       ↓
    
[WARNING] Line 567, Column 3: Variable '_temp' declared but never used

Total scan time: 0.8s
```

---

## How to Start Building (Step-by-Step)

### Step 1: Grammar Definition

```python
# Define SQF grammar rules
grammar = """
program: statement*

statement: assignment | function_call | control_flow

assignment: IDENTIFIER "=" expression ";"

function_call: IDENTIFIER "(" arguments ")"

control_flow: "if" "(" condition ") {" program "}"
"""
```

### Step 2: Build Parser

```python
from lark import Lark
parser = Lark(grammar, parser='lalr')
```

### Step 3: Create Analyzer Rules

```python
def check_semantics(tree):
    errors = []
    
    # Check function exists
    if 'doesNotExist' in tree.children[0]:
        errors.append("Function not found")
    
    return errors
```

### Step 4: VSCode Integration

```json
// .vscode/settings.json
{
    "sqf-linter.enable": true,
    "sqf-linter.checkOnSave": true
}
```

---

## Getting Started

See `SQF-LINTER/` or `GDSCRIPT-LINTER/` subdirectories for actual implementation.

Each will include:

- Grammar parser files
- Analyzer rules
- VSCode extension manifest
- Example usage scripts

---

## Why This Will Work for Your Projects

| Aspect | Before | After Linter |
| -------- | -------- | -------------- |
| Error Discovery | Runtime crash | Compile-time warning |
| Error Location | "Somewhere in function" | Exact line + column |
| Fix Difficulty | Hours of guessing | One edit per error |
| Testing Needed | Every change breaks tests | All errors found upfront |

---

## Next Steps

1. **Pick one language first** (recommend GDScript - Godot provides base)
2. **Build syntax parser** using existing grammar definitions
3. **Create analyzer rules** based on common error patterns
4. **Integrate with VSCode** as a language server
5. **Test against your existing codebases**

---

## Contact

Questions? Check the implementation examples in subdirectories!
