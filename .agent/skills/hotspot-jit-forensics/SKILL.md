---
name: hotspot-jit-forensics
description: Diagnose Java performance issues by inspecting HotSpot bytecode, tiered compilation state, inlining decisions, C2 assembly, compilation limits, and object layout. Produces reproducible artifacts (jit.xml, directives, JFR, disassembly) and links them to actionable code changes.
---

# HotSpot C2 / JIT Forensics & Optimization

A practical playbook for diagnosing Java performance issues by looking at what HotSpot *actually* does.

Use this skill when you suspect:
- Lost inlining, megamorphic dispatch, or deoptimization.
- A hot method is slow because it never reaches C2.
- High CPU in tiny methods (often a no-inline or missed intrinsic).
- High allocation rate or cache-unfriendly object layout.
- Compilation failures/bailouts (node limits, method too big, deep inlining).
- Code cache pressure, safepoint stalls, or lock contention.

---

## Quick start (repeatable artifacts)

1) **Collect JVM facts**
```bash
.codex/skills/hotspot-jit-forensics/scripts/jit-facts.sh --out jit-facts.txt
```
For a running process:
```bash
.codex/skills/hotspot-jit-forensics/scripts/jit-facts.sh --pid <pid> --out jit-facts.txt
```

2) **Generate a C2 directives file**
```bash
.codex/skills/hotspot-jit-forensics/scripts/jit-directives.sh \
  --method "com/foo/MyClass.myMethod()" \
  --out c2-directives.json5
```

3) **Run the target command with compiler logging**
```bash
.codex/skills/hotspot-jit-forensics/scripts/jit-run-log.sh \
  --directives c2-directives.json5 \
  --logfile jit.xml \
  -- java -XX:+UnlockDiagnosticVMOptions -jar app.jar
```

Artifacts produced:
- `jit-facts.txt` (version, flags, OS/arch, optional jcmd output)
- `c2-directives.json5` (method-scoped compiler diagnostics)
- `jit.xml` (HotSpot compilation log)
- Console output with inlining and assembly (if `hsdis` is available)

---

## Core workflow

### 1) Profile first (do not guess)
Use **JFR** or **async-profiler** to identify the actual hot method(s).

### 2) Confirm compilation tier
Determine if the method is interpreted, C1, or C2.

Runtime:
```bash
jcmd <pid> Compiler.codelist | head
jcmd <pid> Compiler.queue
jcmd <pid> Compiler.codecache
```
Start-up (noisy):
```bash
java -XX:+PrintCompilation -jar app.jar
```

### 3) Capture inlining + compilation decisions for ONE method
Prefer **Compiler Directives** with a focused match.

### 4) If assembly doesn’t print, fix hsdis
HotSpot only prints assembly with the **hsdis** plugin installed.

### 5) Read “why not inlined?” and “why not compiled?”
Check `jit.xml` (or JITWatch) for inline failures and compilation bailouts.

### 6) Inspect object layout
Use **JOL** (CLI or code) and `jcmd` class histograms.

### 7) Cross-check system effects
GC, safepoints, locks, and code cache can dominate CPU.

---

## Key flags (diagnosis only)

- `-XX:+UnlockDiagnosticVMOptions`
- `-XX:+LogCompilation -XX:LogFile=jit.xml`
- `-XX:+CompilerDirectivesPrint -XX:CompilerDirectivesFile=c2-directives.json5`
- `-XX:+PrintCompilation`
- `-Xlog:safepoint=info` (JDK 9+)
- `-Xlog:gc*` (JDK 9+)

---

## Script reference

### `jit-facts.sh`
Collect JVM + OS facts and optional `jcmd` diagnostics into one file.

```
Usage: jit-facts.sh [--pid <pid>] [--out <file>]
```

### `jit-directives.sh`
Generate a compiler directives file for a target method.

```
Usage: jit-directives.sh --method <pattern> [--out <file>]
```

### `jit-run-log.sh`
Run a `java` command with directive-based C2 logging.

```
Usage: jit-run-log.sh --directives <file> --logfile <file> -- java <args...>
```

---

## Deliverables checklist

- Repro command line (exact).
- JVM version + flags (`jit-facts.txt`).
- Profile (`recording.jfr` or `cpu.svg`/`alloc.svg`).
- `jit.xml` (LogCompilation).
- directives file (`c2-directives.json5`).
- Assembly snippet for target method(s).
- Object layout output (JOL internals + footprint).
- Summary: “hypothesis → evidence → change → result”.

---

## Reference URLs

- https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html
- https://docs.oracle.com/en/java/javase/12/vm/compiler-control1.html
- https://docs.oracle.com/en/java/javase/12/vm/writing-directives.html
- https://docs.oracle.com/en/java/javase/11/troubleshoot/diagnostic-tools.html
- https://openjdk.org/projects/code-tools/jol/
- https://openjdk.org/jeps/520
