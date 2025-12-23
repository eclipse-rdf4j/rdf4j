const { execSync } = require("child_process");

function run(command) {
  execSync(command, { stdio: "inherit", shell: "/bin/bash" });
}

function dumpThreads() {
  run('echo "== Cancellation detected: capturing JVM thread dumps =="');
  run(
    [
      "set -euo pipefail",
      "pids=$(pgrep -f '[j]ava' || true)",
      'if [[ -z "${pids}" ]]; then echo "No Java processes found."; exit 0; fi',
      "if command -v jcmd >/dev/null 2>&1; then",
      "  for pid in ${pids}; do",
      '    echo "-- jcmd Thread.print for PID ${pid} --"',
      "    jcmd \"${pid}\" Thread.print || true",
      "  done",
      "  exit 0",
      "fi",
      "if command -v jstack >/dev/null 2>&1; then",
      "  for pid in ${pids}; do",
      '    echo "-- jstack for PID ${pid} --"',
      "    jstack \"${pid}\" || true",
      "  done",
      "  exit 0",
      "fi",
      "for pid in ${pids}; do",
      '  echo "-- kill -QUIT ${pid} (no jcmd/jstack available) --"',
      "  kill -QUIT \"${pid}\" || true",
      "done",
    ].join("\n")
  );
}

try {
  dumpThreads();
} catch (error) {
  console.error("Thread dump post-step failed:", error);
}
