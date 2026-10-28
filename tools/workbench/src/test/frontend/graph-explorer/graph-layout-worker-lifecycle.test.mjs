/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
import assert from "node:assert/strict";
import test from "node:test";
import {
  releaseGraphLayoutWorker,
  retainGraphLayoutWorker,
  startGraphLayout,
  warmGraphLayoutWorker,
} from "../../../main/frontend/graph-explorer/graph-layout-client.ts";

const graph = { nodes: [], edges: [], revision: 1 };
const result = () => ({ positions: new Map(), bends: new Map() });
const turn = () => new Promise(resolve => setTimeout(resolve, 0));

async function withWorkers(run) {
  releaseGraphLayoutWorker();
  const previous = globalThis.Worker;
  const created = [];
  const owners = [];
  class FakeWorker {
    onmessage = null;
    onerror = null;
    onmessageerror = null;
    posted = [];
    terminated = 0;
    constructor() { created.push(this); }
    postMessage(message) { this.posted.push(message); }
    terminate() { this.terminated++; }
    finish(value = result(), requestId = this.posted.at(-1).requestId) {
      this.onmessage?.({ data: { requestId, result: value } });
    }
  }
  globalThis.Worker = FakeWorker;
  try {
    await run({ created, FakeWorker, retain() {
      const release = retainGraphLayoutWorker();
      owners.push(release);
      return release;
    } });
  } finally {
    for (const release of owners) release();
    releaseGraphLayoutWorker();
    globalThis.Worker = previous;
  }
}

test("success reuses one worker; stale replies and cancelling its previous job cannot affect the next lease", async () => {
  await withWorkers(async ({ created, retain }) => {
    retain();
    const first = startGraphLayout(graph);
    const worker = created[0];
    const oldHandler = worker.onmessage;
    const firstId = worker.posted[0].requestId;
    worker.finish();
    await first.promise;
    const second = startGraphLayout(graph);
    const secondId = worker.posted[1].requestId;
    assert.notEqual(firstId, secondId);
    let settled = false;
    second.promise.then(() => { settled = true; });
    oldHandler({ data: { requestId: firstId, error: "late first error" } });
    worker.finish(result(), firstId);
    first.cancel();
    await Promise.resolve();
    assert.equal(settled, false);
    assert.equal(worker.terminated, 0);
    const value = result();
    worker.finish(value, secondId);
    assert.equal(await second.promise, value);
    assert.equal(created.length, 1);
    assert.equal(worker.terminated, 0);
  });
});

test("concurrent jobs receive exclusive workers and only one successful idle worker is retained", async () => {
  await withWorkers(async ({ created, retain }) => {
    retain();
    const first = startGraphLayout(graph);
    const second = startGraphLayout(graph);
    assert.equal(created.length, 2);
    created[1].finish();
    await second.promise;
    created[0].finish();
    await first.promise;
    assert.equal(created[0].terminated, 1);
    assert.equal(created[1].terminated, 0);
    const third = startGraphLayout(graph);
    assert.equal(created[1].posted.length, 2);
    const rejection = assert.rejects(third.promise, { name: "AbortError" });
    third.cancel();
    await rejection;
  });
});

test("last-owner teardown cancels delayed replacement warmup and owner releases are idempotent", async () => {
  await withWorkers(async ({ created, retain }) => {
    const releaseFirst = retain();
    const releaseSecond = retain();
    const job = startGraphLayout(graph);
    const rejection = assert.rejects(job.promise, { name: "AbortError" });
    job.cancel();
    await rejection;
    releaseFirst();
    releaseFirst();
    releaseSecond();
    await turn();
    assert.equal(created.length, 1, "the queued prewarm must not revive an unmounted explorer");
    assert.equal(created[0].terminated, 1);
  });
});

test("one explorer leaving does not destroy another explorer's spare", async () => {
  await withWorkers(async ({ created, retain }) => {
    const releaseFirst = retain();
    const releaseSecond = retain();
    releaseFirst();
    releaseFirst();
    releaseGraphLayoutWorker();
    assert.equal(created[0].terminated, 0);
    releaseSecond();
    assert.equal(created[0].terminated, 1);
  });
});

test("an old active job finishing after teardown cannot repopulate a newly mounted explorer's pool", async () => {
  await withWorkers(async ({ created, retain }) => {
    const releaseOld = retain();
    const old = startGraphLayout(graph);
    releaseOld();
    retain();
    const current = startGraphLayout(graph);
    created[0].finish();
    await old.promise;
    assert.equal(created[0].terminated, 1);
    assert.equal(created.length, 2);
    created[1].finish();
    await current.promise;
    assert.equal(created[1].terminated, 0);
  });
});

test("error, messageerror, error replies and malformed replies terminate the failed worker", async () => {
  for (const kind of ["error", "messageerror", "reply", "missing-id", "invalid-result"]) {
    await withWorkers(async ({ created, retain }) => {
      const release = retain();
      const job = startGraphLayout(graph);
      const worker = created[0];
      const requestId = worker.posted[0].requestId;
      const rejection = assert.rejects(job.promise);
      if (kind === "error") worker.onerror();
      else if (kind === "messageerror") worker.onmessageerror();
      else if (kind === "reply") worker.onmessage({ data: { requestId, error: "calculation failed" } });
      else if (kind === "missing-id") worker.onmessage({ data: { result: result() } });
      else worker.onmessage({ data: { requestId, result: null } });
      await rejection;
      job.cancel();
      assert.equal(worker.terminated, 1);
      assert.equal(worker.onmessage, null);
      release();
      await turn();
      assert.equal(created.length, 1);
    });
  }
});

test("an idle worker failure is retired until the next calculation, and stale idle errors cannot kill an active lease", async () => {
  await withWorkers(async ({ created, retain }) => {
    const release = retain();
    const failed = created[0];
    failed.onerror();
    await turn();
    assert.equal(failed.terminated, 1);
    assert.equal(created.length, 1, "an idle startup failure must not automatically retry");
    const job = startGraphLayout(graph);
    assert.equal(created.length, 2);
    created[1].finish();
    await job.promise;
    const oldIdleError = created[1].onerror;
    const nextJob = startGraphLayout(graph);
    oldIdleError();
    assert.equal(created[1].terminated, 0, "a stale idle handler cannot terminate an active lease");
    created[1].finish();
    await nextJob.promise;
    release();
    assert.equal(created[1].terminated, 1);
  });
});

test("persistent startup failures stop after one replacement and do not create a retry loop", async () => {
  await withWorkers(async ({ created, FakeWorker, retain }) => {
    globalThis.Worker = class extends FakeWorker {
      constructor() {
        super();
        queueMicrotask(() => this.onerror?.());
      }
    };
    retain();
    await turn();
    await turn();
    assert.equal(created.length, 1);
    assert.equal(created[0].terminated, 1);
    const job = startGraphLayout(graph);
    await assert.rejects(job.promise, /worker failed/);
    await turn();
    await turn();
    assert.equal(created.length, 3, "a failed explicit job may prewarm once; that idle failure must stop");
    assert.deepEqual(created.map(worker => worker.terminated), [1, 1, 1]);
  });
});

test("injected workers keep the legacy one-shot contract and cannot be leased concurrently", async () => {
  await withWorkers(async ({ created, FakeWorker, retain }) => {
    retain();
    const worker = new FakeWorker();
    const factory = () => worker;
    const first = startGraphLayout(graph, {}, factory);
    const handler = worker.onmessage;
    await assert.rejects(startGraphLayout(graph, {}, factory).promise, /already owned/);
    assert.equal(worker.terminated, 0);
    assert.equal(worker.onmessage, handler);
    worker.onmessage({ data: { result: result() } });
    await first.promise;
    first.cancel();
    assert.equal(worker.terminated, 1);
    assert.equal(created[0].posted.length, 0);
    assert.equal(created[0].terminated, 0);
    await assert.rejects(startGraphLayout(graph, {}, () => created[0]).promise, /already owned/);
    assert.equal(created[0].terminated, 0);
  });
});

test("constructor and postMessage failures reject without leaving an active lease", async () => {
  await withWorkers(async ({ FakeWorker }) => {
    await assert.rejects(startGraphLayout(graph, {}, () => { throw new Error("constructor failure"); }).promise, /constructor failure/);
    const worker = new FakeWorker();
    worker.postMessage = () => { throw new DOMException("cannot clone", "DataCloneError"); };
    await assert.rejects(startGraphLayout(graph, {}, () => worker).promise, { name: "DataCloneError" });
    assert.equal(worker.terminated, 1);
    assert.equal(worker.onmessage, null);
  });
});

test("explicit prewarm remains idempotent and release prevents a replacement after cancellation", async () => {
  await withWorkers(async ({ created }) => {
    warmGraphLayoutWorker();
    warmGraphLayoutWorker();
    const job = startGraphLayout(graph);
    const rejection = assert.rejects(job.promise, { name: "AbortError" });
    job.cancel();
    releaseGraphLayoutWorker();
    await rejection;
    await turn();
    assert.equal(created.length, 1);
    assert.equal(created[0].terminated, 1);
  });
});
