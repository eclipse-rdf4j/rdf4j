/*
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials are made available under
 * the terms of the Eclipse Distribution License v1.0, available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 * SPDX-License-Identifier: BSD-3-Clause
 */
const fs = require("node:fs");
const path = require("node:path");
const { spawnSync } = require("node:child_process");

function restoreRunnerOwnership(file) {
  if (process.platform !== "linux") {
    throw new Error(`Diagnostic file is not readable by the runner: ${file}`);
  }
  // HotSpot creates mode-0600 heaps. Containers may use a different UID from the runner.
  // Change ownership only inside this job's dedicated scratch directory, never a symlink target.
  const result = spawnSync("sudo", ["-n", "chown", "--no-dereference",
    `${process.getuid()}:${process.getgid()}`, "--", file], { encoding: "utf8", timeout: 10000 });
  if (result.status !== 0) {
    throw new Error(`Cannot restore runner ownership of diagnostic output ${file}: ${result.stderr || result.error || result.status}`);
  }
}

function prepare(directory, repair = restoreRunnerOwnership) {
  if (!directory || !fs.existsSync(directory)) return;
  function readable(file, directoryEntry) {
    const access = fs.constants.R_OK | (directoryEntry ? fs.constants.X_OK : 0);
    try {
      fs.accessSync(file, access);
    } catch (error) {
      if (error.code !== "EACCES") throw error;
      repair(file, directoryEntry ? "a+rx" : "a+r");
      fs.accessSync(file, access);
    }
  }
  function visit(file) {
    const stat = fs.lstatSync(file);
    if (stat.isSymbolicLink()) return;
    if (stat.isDirectory()) {
      readable(file, true);
      for (const child of fs.readdirSync(file)) visit(path.join(file, child));
    } else if (stat.isFile() && /(?:\.hprof(?:\.gz)?|hs_err_pid[^/]*\.log)$/u.test(file)) {
      readable(file, false);
    }
  }
  visit(directory);
}

if (require.main === module) prepare(process.env.RDF4J_JVM_DIAGNOSTICS_DIR);
module.exports = { prepare };
