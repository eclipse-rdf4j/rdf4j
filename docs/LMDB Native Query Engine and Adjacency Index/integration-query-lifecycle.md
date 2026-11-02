# Shared iteration, timeout, and operation lifecycle

This guide separates the branch's shared iteration changes from existing
request-cancellation machinery that provides context for them. The historical
comparison is merge base `4aec7e9a2223d873b1c1a7703aad4c87bf8354df` through
inventory snapshot `a869fe298dc4700ce956bcaf9fece3745c57fe05`. Branch
additions are the
experimental `CloseableIteration.seek` hint and the internal
`CooperativeCancellation` capability, including timeout integration and
wrapper forwarding. The HTTP request-ID protocol and Workbench/server
operation coordinators described later are present by this inventory snapshot
but are not new branch features. `AbstractHTTPQuery` changes only to transport a forced
LMDB execution strategy; it does not add cancellation IDs. The repository
connection change adds a batch-ingestion extension, described in
[bulk loading](integration-bulk-loading.md).

## Optional ordered seek

`CloseableIteration` gains `seek(min, minInclusive, max, maxInclusive)` and
`supportsSeek()`. Both are experimental. Existing implementations stay
compatible because the defaults ignore the hint and return `false`.

The contract is deliberately weaker than a filter: it applies only when the
producer and consumer already agree on an element order. A reader may skip any
subset of values below the lower bound or above the upper bound, or may ignore
the hint and return them. A null bound is unbounded. Successive lower bounds
must not decrease. Callers therefore need to keep their ordinary comparison
checks and cannot depend on seek for correctness. An iteration can implement
the hint only if it can compare the requested values in the producer's order;
when it cannot, it does nothing. Wrappers forward `supportsSeek` and `seek`
only while preserving that same order. A wrapper that changes order cannot
advertise its delegate's seek capability unchanged.

This is a way for an ordered merge consumer to communicate a proven future
lower bound to a capable store reader without making an optimization part of
the public result semantics. It can avoid traversing a prefix on suitable
readers; the default implementation has no additional retained state. The
benefit depends on the concrete producer and ordering, and no timing claim is
made here. See the [native physical-access guide](query-physical-access.md)
for LMDB-side implementations and consumers.

## Cooperative timeout path

`CooperativeCancellation` is an internal capability with one method,
`requestCancellation()`. It asks an iteration or downstream iteration to stop
at an implementation-defined poll point and reports whether the request was
accepted. It does not close a resource itself and does not promise an
immediate interrupt. Pass-through iterations forward the request to the
iterations they read from: `IterationWrapper`, `FilterIteration`,
`ConvertingIteration`, the set and union iterations, the Sail and repository
wrappers, and the query-algebra join, path and asynchronous iterators.
`LookAheadIteration` records the request so that long-running subclasses can
poll it. Operators that loop over their input in memory, such as
`GroupIterator`, `OrderIterator`, the hash and nested-loop joins and the
asynchronous read-ahead iterators, poll the request and throw
`QueryInterruptedException`, so a cancelled operator never reads as a complete
result. A closed wrapper declines the request.

A query's maximum execution time is enforced in two places. While
`SailConnection.evaluate()` runs and no result exists yet, the Sail query
guards the evaluating thread with `InterruptingTimeLimit`, which interrupts
that thread if the limit elapses; the query then reports
`QueryInterruptedException("Query evaluation took too long")` and clears the
interrupt it delivered. The result is wrapped in a `TimeLimitIteration` that
receives the remaining budget; an already elapsed budget makes the first call
report the time limit.

When the time limit of a `TimeLimitIteration` elapses it escalates in three
steps. It first requests cooperative cancellation and reports the time limit
to every later call. If the wrapped iteration has not been closed after the
interrupt grace (two seconds by default), it interrupts the threads still
inside one of its methods. If it has still not been closed after the close
grace (two more seconds), it closes the wrapped iteration on a separate
thread, also beneath an active call; sources that own native resources defer
releasing them until that call exits. A wrapped iteration is otherwise closed
only when no call is active: a caller that observes the time limit closes it,
and an explicit `close()` from another thread during an active call requests
cancellation, leaves the close to the active caller and follows the same
escalation. Interrupts delivered by the wrapper are cleared before the
interrupted call returns. The grace periods can be changed with the
`org.eclipse.rdf4j.common.iteration.TimeLimitIteration.interruptGraceMillis`
and `org.eclipse.rdf4j.common.iteration.TimeLimitIteration.closeGraceMillis`
system properties.

The timer is a shared daemon `Timer`; its tasks only signal, interrupt or hand
the close to a virtual thread, so one blocked iteration cannot delay another
iteration's time limit. This is cooperative cancellation backed by interrupts,
not a promise that native or remote I/O stops at the instant the timer fires.

Source: [`CloseableIteration`](../../../core/common/iterator/src/main/java/org/eclipse/rdf4j/common/iteration/CloseableIteration.java),
[`CooperativeCancellation`](../../../core/common/iterator/src/main/java/org/eclipse/rdf4j/common/iteration/CooperativeCancellation.java),
[`TimeLimitIteration`](../../../core/common/iterator/src/main/java/org/eclipse/rdf4j/common/iteration/TimeLimitIteration.java),
[`IterationWrapper`](../../../core/common/iterator/src/main/java/org/eclipse/rdf4j/common/iteration/IterationWrapper.java),
[`InterruptingTimeLimit`](../../../core/common/iterator/src/main/java/org/eclipse/rdf4j/common/iteration/InterruptingTimeLimit.java)
and [`FilterIteration`](../../../core/common/iterator/src/main/java/org/eclipse/rdf4j/common/iteration/FilterIteration.java).
Relevant source tests are
[`CloseableIterationSeekDefaultTest`](../../../core/common/iterator/src/test/java/org/eclipse/rdf4j/common/iteration/CloseableIterationSeekDefaultTest.java),
[`TimeLimitIterationTest`](../../../core/common/iterator/src/test/java/org/eclipse/rdf4j/common/iteration/TimeLimitIterationTest.java),
[`TimeLimitIterationLadderTest`](../../../core/common/iterator/src/test/java/org/eclipse/rdf4j/common/iteration/TimeLimitIterationLadderTest.java),
[`CooperativeCancellationForwardingTest`](../../../core/common/iterator/src/test/java/org/eclipse/rdf4j/common/iteration/CooperativeCancellationForwardingTest.java),
[`CooperativeCancellationWrapperTest`](../../../core/sail/api/src/test/java/org/eclipse/rdf4j/sail/helpers/CooperativeCancellationWrapperTest.java),
and the materialized replay tests listed in the [query memory guide](query-memory-and-result-lifecycle.md).
These links identify source coverage and do not imply passing results.

## Existing RDF4J Server HTTP cancellation protocol

The current server has separate identifiers for regular queries and query
explanations. These protocol parameters are context for the lifecycle but
were not introduced by this branch. A client that needs an externally
addressable operation supplies a nonblank ID on its non-transactional query
request. The request context is a scoped `ThreadLocal`: the server activates
it around evaluation, and `RDF4JProtocolSession` carries it onto the outbound
HTTP query or explanation request. The activation restores any enclosing
context on close, which matters when one server query calls another server.
Transactions reject `query-request-id`, `cancel-query`, and `cancel-explain`
with HTTP 400; tracked query cancellation is not supported on that route.

The regular query cancel operation is:

```text
POST /repositories/{repository-id}/query?cancel-query=true&query-request-id={id}
```

For an explanation, use the same query URL and `POST` with
`cancel-explain=true&explain-request-id={id}`. The protocol client sends those
parameters on the query endpoint and requires a non-null, nonblank ID; it
trims surrounding whitespace. The server's query request handler recognizes
the operation when the method is `POST` and the matching cancel parameter is
present, requires a nonblank ID (400 otherwise), and scopes lookup by
repository ID. It returns 204 when an active handle accepted cancellation,
404 when no matching active operation exists (including one that has already
completed), and 502 when remote cancellation forwarding fails. The client
surfaces non-success responses, including 404, as a repository exception.
The handler checks parameter presence; the canonical client spelling is the
literal value `true` shown above.

An ordinary non-transactional server query with no supplied query ID receives
an internal UUID so the server can track its own execution and propagate the
context to a downstream HTTP repository. That generated ID is not a substitute
for a caller-supplied ID when an external caller needs to issue a matching
cancel request. Explanations are tracked by the async-explain handler only
when an `explain-request-id` is supplied. Duplicate active `(repository,
request-id)` registrations return 400. IDs are trimmed and must not be blank.

The existing coordinator has states `ACTIVE`, `CANCELLING`, `CANCEL_FAILED`,
`CANCELLED`, `COMPLETED`, and `TERMINAL`. Cancellation reserves the handle,
marks it inactive, interrupts its attached worker thread, then runs any remote
cancel callback. It dispatches owned connection close asynchronously after
the remote attempt, because that close can wait for work released by remote
cancellation. Success removes the handle. A failed remote attempt leaves it
registered and starts delayed retries: one initial attempt plus at most 20
retries, spaced 250 ms apart. Exhaustion makes the handle terminal. A
completed operation returns false to a later cancellation request. These are
coordinator lifecycle mechanics, not an API-level promise that every iterator
responds immediately to `Thread.interrupt()`.

The identifiers do not create a public `Query.cancel()` or
`RepositoryConnection.cancel()` method. Cancellation is an HTTP protocol
operation coordinated by the request handler; local iteration-level
cooperation is a separate capability. The client-side `cancelQuery` method
and the new iteration capability therefore solve different parts of the
lifecycle.

## Workbench routes and disconnect behavior

The Workbench maps `/query` to `QueryServlet`. It handles cancel actions in
`doPost`, so a regular cancel is:

```text
POST /repositories/{repository-id}/query
Content-Type: application/x-www-form-urlencoded

action=cancel-query&query-request-id={id}
```

For async explain cancellation, send `action=cancel-explain` and
`explain-request-id={id}`. Missing or blank IDs return 400; active cancellation
returns 204; an unknown/completed ID returns 404; forwarding failure returns
502. Handles are scoped to the Workbench repository reference. Standard query
requests with a nonblank query register an internal UUID if no ID was supplied;
an explicitly supplied blank ID is rejected. Async explain requires its ID.
The Workbench query response can include the registered query ID and result
status as metadata. The current browser keeps the cancel ID in the active page
while the query runs and clears stale cancel state on `pagehide`/`pageshow`.
Submission navigates the current page by GET unless the query path exceeds
2,048 characters or the full URL exceeds 2,083, when the current form uses POST;
the form has no named result-window target. Repeated execution cancels the
earlier request before tracking a fresh ID. Response metadata remains part of
the response contract but is not a cross-window lifecycle signal.

For `HTTPRepository`, the Workbench's handle carries a remote-cancel callback;
embedded operations have no remote HTTP callback. In either case the existing
coordinator can interrupt the worker and close the connection asynchronously.
When a safe-format response heartbeat detects a disconnect, its callback
reserves cancellation and dispatches it on a virtual thread so the output
writer does not block on HTTP or connection cleanup. The heartbeat uses a
one-second default interval, emits only a format-approved prefix before the
first result, synchronizes probes with serializer output, and disables probes
when payload begins. If an error follows a probe, it appends a NUL byte to
invalidate a whitespace-only committed body rather than leaving a response
that could parse as an empty result. These heartbeat details are existing
response machinery, not new timeout semantics.

Sources: [protocol parameter names](../../../core/http/protocol/src/main/java/org/eclipse/rdf4j/http/protocol/Protocol.java),
[HTTP query session](../../../core/http/client/src/main/java/org/eclipse/rdf4j/http/client/RDF4JProtocolSession.java),
[query request context](../../../core/http/client/src/main/java/org/eclipse/rdf4j/http/client/QueryRequestContext.java),
[explanation request context](../../../core/http/client/src/main/java/org/eclipse/rdf4j/http/client/QueryExplanationRequestContext.java),
[operation coordinator](../../../core/http/client/src/main/java/org/eclipse/rdf4j/http/client/CancellableOperationCoordinator.java),
[response heartbeat](../../../core/http/client/src/main/java/org/eclipse/rdf4j/http/client/QueryResponseHeartbeat.java),
[server query request handler](../../../tools/server-spring/src/main/java/org/eclipse/rdf4j/http/server/repository/handler/AbstractQueryRequestHandler.java),
[transaction request guard](../../../tools/server-spring/src/main/java/org/eclipse/rdf4j/http/server/repository/transaction/TransactionController.java),
[Workbench servlet](../../../tools/workbench/src/main/java/org/eclipse/rdf4j/workbench/commands/QueryServlet.java),
and [Workbench route mapping](../../../tools/workbench/src/main/webapp/WEB-INF/web.xml).
Named source coverage includes
[`RDF4JProtocolSessionTest`](../../../core/http/client/src/test/java/org/eclipse/rdf4j/http/client/RDF4JProtocolSessionTest.java),
[`CancellableOperationCoordinatorTest`](../../../core/http/client/src/test/java/org/eclipse/rdf4j/http/client/CancellableOperationCoordinatorTest.java),
[`QueryResponseHeartbeatTest`](../../../core/http/client/src/test/java/org/eclipse/rdf4j/http/client/QueryResponseHeartbeatTest.java),
[`QueryResponseHeartbeatHandlerTest`](../../../tools/server-spring/src/test/java/org/eclipse/rdf4j/http/server/repository/handler/QueryResponseHeartbeatHandlerTest.java),
and [`QueryServletTest`](../../../tools/workbench/src/test/java/org/eclipse/rdf4j/workbench/commands/QueryServletTest.java).
These links identify source coverage and do not imply passing results.

## Batch ingestion API extension

`AbstractRepositoryConnection` now dispatches an iterable add through a
protected `addWithoutCommit(Iterable, contexts)` hook instead of always
calling the single-statement hook in the base loop. Its default implementation
preserves the previous iteration behavior. `SailRepositoryConnection`
overrides the new hook and calls `SailConnection.addStatements`; that new
default Sail method also preserves repeated `addStatement` semantics while
letting a concrete Sail override it to retain batch structure. With no
replacement contexts, each statement's own context is retained; supplied
contexts replace it. This is an extension point for stores that can ingest a
batch efficiently without changing transaction or context semantics. It does
not itself promise a particular batching speedup. The bulk loader's storage
override and operational contract are covered in
[bulk loading](integration-bulk-loading.md).

Sources: [`AbstractRepositoryConnection`](../../../core/repository/api/src/main/java/org/eclipse/rdf4j/repository/base/AbstractRepositoryConnection.java),
[`SailRepositoryConnection`](../../../core/repository/sail/src/main/java/org/eclipse/rdf4j/repository/sail/SailRepositoryConnection.java),
and [`SailConnection`](../../../core/sail/api/src/main/java/org/eclipse/rdf4j/sail/SailConnection.java).

## Safe extension checklist

When adding an iteration wrapper, preserve `close()` ownership, and forward
cooperative cancellation only when the delegate can accept it. Forward seek
only when the wrapper preserves the agreed order and its buffering state can
honor the hint; otherwise keep the default no-op. Keep timeout signaling
distinct from immediate thread interruption. For request-level cancellation,
use a nonblank request ID with the right repository scope, ensure registration
spans evaluation and result serialization, and complete the handle on every
success and error path. Tests should cover cancellation before evaluation,
during evaluation, during result serialization, unknown/completed IDs,
duplicate IDs, remote-cancel retry, and close failures; the checked-in tests
map these paths as source coverage references; they do not claim a passing result.
