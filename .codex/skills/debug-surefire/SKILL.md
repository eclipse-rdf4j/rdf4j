---
name: debug-surefire
description: Debug Maven Surefire unit tests by running them in JDWP "wait for debugger" mode (`-Dmaven.surefire.debug`) and attaching to the forked test JVM using **jdb** (preferred for CLI/agent debugging), IntelliJ, or VS Code. Use when asked to debug/step through a failing JUnit test, attach a debugger to a Maven test run, or run `mvn test -Dtest=Class[#method]` suspended on a port (including multi-module `-pl` runs). The JVM will block at startup until a debugger attaches; the agent should attach with `jdb -attach <host>:<port>` and drive the session from the terminal.
---

# debug-surefire

Run Maven Surefire tests **suspended in JDWP** so you can attach a debugger and step through the code.  
In headless/agent environments, **attach with `jdb`** (the JDK’s command-line debugger).

## What this does

- Runs `mvn test` with Surefire in debug mode via `-Dmaven.surefire.debug`.
- Surefire launches the **forked test JVM** with a JDWP socket and `suspend=y`, meaning:
  - the forked JVM starts,
  - prints “Listening for transport dt_socket at address: <port>”,
  - then **waits** until a debugger attaches,
  - only then does it begin executing tests.

This is important: you do **not** attach to the `mvn` process itself; you attach to the **forked JVM** running the tests.

## Quick start

1) Start a suspended test run

- Debug a test class:
  - `.codex/skills/debug-surefire/scripts/debug-surefire.sh --test-class MyTest`
- Debug a single test method (quote the `#`):
  - `.codex/skills/debug-surefire/scripts/debug-surefire.sh --test 'MyTest#shouldDoThing'`
- Debug a test in a specific module:
  - `.codex/skills/debug-surefire/scripts/debug-surefire.sh --module core/sail/shacl --test-class ShaclSailTest`
  - `.codex/skills/debug-surefire/scripts/debug-surefire.sh --module rdf4j-sail-shacl --test 'ShaclSailTest#testSomething'`

When the forked JVM is ready, you should see something like:

- `Listening for transport dt_socket at address: 55005`

2) Attach with `jdb` (preferred)

In a second terminal, attach to the printed port:

- Local attach (port only implies localhost):
  - `jdb -attach 55005`
- Explicit host+port:
  - `jdb -attach localhost:55005`

If you need source listing inside jdb, provide a sourcepath up front:

- `jdb -sourcepath module/src/main/java:module/src/test/java -attach 55005`

3) Set breakpoints / catches, then resume

Once you’re at the `jdb` prompt, set a breakpoint (or exception catch) and continue:

- `stop in com.example.MyTest.shouldDoThing`
- `catch uncaught java.lang.AssertionError`
- `cont`

Tip: right after attaching to a just-started suspended JVM, `jdb` may show:
- `No frames on the current call stack`

That’s normal. The JVM hasn’t executed into any Java frames yet. Set breakpoints first, then `cont`.

---

## Notes

- The script runs a fast pre-test install (`-Pquick` into the repo-local `.m2_repo`) and then runs `mvn test` with Surefire in debug mode.
- Use `SUREFIRE_DEBUG_PORT=8000` to change the port (default: `55005`).
- Use `--no-offline` / `--online` if offline (`-o`) resolution fails.
- Everything after `--` is passed to Maven, e.g.:
  - `.codex/skills/debug-surefire/scripts/debug-surefire.sh --test-class MyTest -- -DtrimStackTrace=false -DfailIfNoTests=false -DforkCount=1 -DreuseForks=false`

---

## jdb interaction guide

This section is optimized for quickly getting signal from a failing unit test without drowning in framework/JDK internals.

### Command cheat sheet (the ones you’ll actually use)

**Start / resume**
- `cont` — continue execution from current stop/breakpoint
- `run` — start execution *when jdb launched the VM* (less relevant when you used `-attach`)

**Breakpoints**
- `stop in <class>.<method>` — break on method entry
- `stop at <class>:<line>` — break at a specific line
- `stop` — list all breakpoints
- `clear <class>.<method>` / `clear <class>:<line>` — remove a breakpoint
- `clear` — list breakpoints (same idea as `stop` listing)

**Stepping**
- `next` — step over calls (line-level)
- `step` — step into calls (line-level)
- `step up` — run until current method returns
- `stepi` — step one bytecode instruction (rarely needed)

**Threads / stacks**
- `threads` — list threads
- `thread <id>` — select default thread
- `where` — stack trace for current thread
- `where all` — stack traces for all threads
- `up` / `down` — move the current frame up/down the stack

**Inspect state**
- `locals` — print locals in current frame
- `print <expr>` — evaluate/print an expression (same as `eval`)
- `dump <expr>` — more complete object dump
- `set <lvalue> = <expr>` — mutate a variable/field (use sparingly)

**Source**
- `list` — show source around current line
- `list <line>` or `list <method>` — show a specific region
- `use <path1>:<path2>` (aka `sourcepath`) — set where jdb looks for sources

**Exceptions**
- `catch uncaught <exception>` — break when an uncaught exception occurs
- `catch caught <exception>` — break when a caught exception occurs
- `catch all <exception>` — break for both caught+uncaught
- `ignore ...` — undo a catch

**Reduce noise**
- `exclude <pattern>,<pattern>,...` — don’t report step/method events for matching classes
- `exclude none` — clear exclusions

**Automation**
- `monitor <command>` — run a command every time the program stops (e.g., `monitor where`)
- `read <file>` — execute commands from a file
- `!!` — repeat last command
- `<n> <command>` — repeat command n times (e.g., `10 next`)

### Efficient workflow for failing JUnit tests

#### 1) Add exclusions immediately (so stepping doesn’t become a swamp)

Right after connecting, set exclusions to avoid stepping into JDK/framework code:

- `exclude java.*,javax.*,jdk.*,sun.*,com.sun.*,org.junit.*,org.junit.jupiter.*,org.assertj.*,org.hamcrest.*,org.mockito.*,org.apache.maven.*`

You can always clear this later:

- `exclude none`

#### 2) Break where it matters

Common breakpoint patterns:

- Break at the failing test method:
  - `stop in com.example.MyTest.shouldDoThing`

- Break inside the code under test:
  - `stop in com.example.service.FooService.doWork`

- Break at a specific suspicious line:
  - `stop at com.example.service.FooService:123`

If the method is overloaded, specify argument types:

- `stop in com.example.FooService.doWork(int,java.lang.String)`

Note: `jdb` supports **deferred breakpoints**. If the class isn’t loaded yet, it will still accept the breakpoint and activate it when the class loads.

#### 3) Break on the *failure*, not just your guess

For unit tests, breaking on the thrown assertion/error is often faster than guessing a line.

Useful catches:

- Classic Java assertions / many test failures:
  - `catch uncaught java.lang.AssertionError`

- Common in JUnit 5 assertions:
  - `catch uncaught org.opentest4j.AssertionFailedError`

- NPE hunting:
  - `catch uncaught java.lang.NullPointerException`

You can remove a catch with `ignore`:

- `ignore uncaught java.lang.AssertionError`

Tip: `catch all java.lang.Throwable` is the nuclear option. It works, but it can get loud.

#### 4) Resume and drive

Once breakpoints/catches are set:

- `cont`

When you hit a breakpoint:

- `where` to see the call stack
- `list` to see nearby source
- `locals` to see local variables
- `print someVar` or `print this.someField` to inspect
- `next` to step over, `step` to step into

#### 5) Find the “real” test thread quickly

Surefire + JUnit can spin up multiple threads (and Maven itself has its own). When in doubt:

- `threads`
- `where all`

Then pick the thread that’s in your test/code-under-test:

- `thread <id>`
- `where`

#### 6) Keep the JVM count predictable

Surefire forking and parallelism can make debugging confusing. If you see multiple JVMs / inconsistent behavior, pass Maven flags to keep it single and fresh:

- `-DforkCount=1 -DreuseForks=false`

Example:

- `.codex/skills/debug-surefire/scripts/debug-surefire.sh --test 'MyTest#shouldDoThing' -- -DforkCount=1 -DreuseForks=false`

#### 7) Use “thread-only” breakpoints when concurrency matters

By default, `jdb` breakpoints suspend all threads, which can create deadlocks in concurrent tests. You can tell jdb to suspend only the thread that hits the breakpoint:

- `stop thread in com.example.concurrent.Worker.run`

(You can also target a specific thread id with `stop thread <thread_id> in ...` if needed.)

---

## jdb startup customization (optional but powerful)

jdb will execute startup commands from `jdb.ini` or `.jdbrc` in either `user.home` or `user.dir`.

This is handy for keeping your default exclusions/catches consistent, e.g.:

- `exclude java.*,javax.*,jdk.*,sun.*,com.sun.*,org.junit.*,org.assertj.*`
- `catch uncaught java.lang.AssertionError`

You can also keep a project-local command file and load it on demand:

- `read .codex/skills/debug-surefire/jdb.cmds`

---

## Troubleshooting

- **jdb can’t connect**
  - Confirm the port printed by the suspended JVM matches what you used in `jdb -attach`.
  - Confirm you’re attaching to the forked JVM port (the one that prints “Listening for transport…”), not Maven’s PID.
  - If running remotely (CI box / container / VM), ensure the port is reachable or forwarded.

- **Breakpoints don’t hit**
  - Use fully qualified class names.
  - If overloaded, include argument types.
  - Use `classes` to see what’s loaded.
  - Try `stop at <class>:<line>` to avoid signature mismatch.

- **`list` can’t find source**
  - Set sourcepath:
    - `use module/src/main/java:module/src/test/java`
  - Or launch jdb with `-sourcepath ...` from the start.
