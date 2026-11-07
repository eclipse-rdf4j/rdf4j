/*
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials are made available under
 * the terms of the Eclipse Distribution License v1.0, available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 * SPDX-License-Identifier: BSD-3-Clause
 */
const fs = require("node:fs");
const path = require("node:path");
const crypto = require("node:crypto");

function configure(environment = process.env) {
  if (!environment.RUNNER_TEMP || !environment.GITHUB_ENV) {
    throw new Error("GitHub runner temporary directory and environment file are required");
  }
  const directory = fs.mkdtempSync(path.join(environment.RUNNER_TEMP, "rdf4j-jvm-diagnostics-"));
  const jvmDirectory = directory.replace(/\\/g, "/");
  if (/[\r\n"]/u.test(jvmDirectory)) {
    throw new Error("Unsupported character in JVM diagnostic directory");
  }
  const diagnosticOptions = [
    "-XX:+HeapDumpOnOutOfMemoryError",
    "-XX:HeapDumpGzipLevel=1",
    `"-XX:HeapDumpPath=${jvmDirectory}/java_pid%p.hprof.gz"`,
    `"-XX:ErrorFile=${jvmDirectory}/hs_err_pid%p.log"`,
  ].join(" ");
  const javaToolOptions = [environment.JAVA_TOOL_OPTIONS, diagnosticOptions].filter(Boolean).join(" ");
  const exports = {
    RDF4J_JVM_DIAGNOSTICS_DIR: directory,
    RDF4J_CI_JVM_DIAGNOSTIC_OPTIONS: diagnosticOptions,
    JAVA_TOOL_OPTIONS: javaToolOptions,
  };
  for (const [name, value] of Object.entries(exports)) {
    const delimiter = `rdf4j_${crypto.randomUUID()}`;
    fs.appendFileSync(environment.GITHUB_ENV, `${name}<<${delimiter}\n${value}\n${delimiter}\n`);
  }
  return { directory, diagnosticOptions, javaToolOptions };
}

if (require.main === module) {
  const result = configure();
  console.log(`JVM OOM heaps and fatal-error logs will be retained in ${result.directory}`);
}
module.exports = { configure };
