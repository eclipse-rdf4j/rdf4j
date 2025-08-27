package org.eclipse.rdf4j.queryrender;

import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Wraps a query assertion. If it fails, runs the shrinker and rethrows with the minimized query.
 *
 * Usage inside a DynamicTest body:
 *   ShrinkOnFailure.wrap(q, () -> assertRoundTrip(q), failureOracle);
 */
public final class ShrinkOnFailure {
	private ShrinkOnFailure(){}

	public static void wrap(String query,
							Executable assertion,
							SparqlShrinker.FailureOracle oracle) {
		try {
			assertion.execute();
		} catch (Throwable t) {
			try {
				SparqlShrinker.Result r = SparqlShrinker.shrink(
						query,
						oracle,
						null, // or a ValidityOracle to enforce validity during shrinking
						new SparqlShrinker.Config()
				);
				String msg = "Shrunk failing query from " + query.length() + " to " + r.minimized.length() +
						" chars, attempts=" + r.attempts + ", accepted=" + r.accepted +
						"\n--- minimized query ---\n" + r.minimized + "\n------------------------\n" +
						String.join("\n", r.log);
				fail(msg, t);
			} catch (Exception e) {
				fail("Shrink failed: " + e.getMessage(), t);
			}
		}
	}
}
