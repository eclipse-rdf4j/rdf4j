# Repository listing and server failure lifecycle

This guide records two non-LMDB operational changes in the historical
comparison from merge base
`4aec7e9a2223d873b1c1a7703aad4c87bf8354df` to inventory snapshot
`a869fe298dc4700ce956bcaf9fece3745c57fe05`. Neither change is an LMDB query
optimization. One isolates repository metadata failures during local
repository enumeration; the other closes the embedded server context after
an out-of-memory error reaches the MVC dispatch boundary.

## Local repository enumeration

`LocalRepositoryManager.getAllRepositoryInfos()` still obtains the repository
directory list and visits only child directories containing the repository
configuration file. It now wraps each individual `getRepositoryInfo(name)`
call in its own `catch (Exception)`. A failing repository is logged as
`Skipping repo <name> due to: ...`; iteration then continues and the method
returns metadata for the repositories that loaded successfully. The method
does not catch `Error`, and a failure to enumerate the repository directory
itself still follows the existing empty-result path when `File.list(...)`
returns null.

The observable tradeoff is partial discovery: one unreadable or malformed
repository configuration no longer aborts metadata enumeration for every
other configured repository. The skipped repository is not repaired and
remains unavailable through that returned list until its metadata can be
loaded. Because the exception is logged with its stack trace, administrators
can find the repository ID and cause in server logs. This is metadata-list
resilience, not a guarantee that the repository itself can be initialized or
queried.

Changed source: [`LocalRepositoryManager.getAllRepositoryInfos`](../../../core/repository/manager/src/main/java/org/eclipse/rdf4j/repository/manager/LocalRepositoryManager.java).
The existing [manager test class](../../../core/repository/manager/src/test/java/org/eclipse/rdf4j/repository/manager/LocalRepositoryManagerTest.java)
is a source map, not evidence that this particular branch behavior was newly
tested or executed.

## Out-of-memory handling in the server servlet

`LoggingDispatcherServlet.doDispatch` now checks failures escaping Spring MVC
dispatch for an `OutOfMemoryError`, including one nested in up to 100
`getCause()` links. Its exception-resolver hook also rethrows an exception
that contains such an error so the standard resolver does not turn it into an
ordinary application response. When an out-of-memory error is found,
`doDispatch` walks to the root `ApplicationContext`; if it is a
`ConfigurableApplicationContext`, it closes it, then rethrows the same
`OutOfMemoryError`. The error is not converted into a success response and
the servlet does not attempt to continue serving requests with a potentially
exhausted heap. Other non-OOME exceptions continue through Spring's ordinary
dispatch/resolution behavior. The changed `doDispatch` catch handles
`Exception` and `OutOfMemoryError`; unrelated `Error` subclasses are outside
that branch.

Closing the root context asks Spring-managed server resources to shut down
after catastrophic heap exhaustion. That is a terminal containment action,
not a way to recover memory or make an OOME harmless. The source test
[`LoggingDispatcherServletTest`](../../../tools/server-boot/src/test/java/org/eclipse/rdf4j/tools/serverboot/LoggingDispatcherServletTest.java)
constructs a root and child context, injects an `OutOfMemoryError` from a
controller, and asserts that the same error escapes and the root context is
closed. The test source is a coverage reference, not a current passing result.

Changed source: [`LoggingDispatcherServlet`](../../../tools/server-boot/src/main/java/org/eclipse/rdf4j/tools/serverboot/LoggingDispatcherServlet.java).
The adjacent LMDB query read-handle timeout test is in
[`LmdbTimedOutQueryReadHandleTest`](../../../tools/server-boot/src/test/java/org/eclipse/rdf4j/tools/serverboot/LmdbTimedOutQueryReadHandleTest.java);
it exercises native query cleanup context and is covered with the
[LMDB result lifecycle](query-memory-and-result-lifecycle.md), not as proof of
the servlet's OOME policy.
