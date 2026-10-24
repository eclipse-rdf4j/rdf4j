/**
 * LEO-style learned optimizer feedback contracts.
 * <p>
 * The package separates three concerns: runtime observation, evidence lookup, and planner application. Learned evidence
 * may correct cardinality or work estimates, but it must never rewrite query semantics. Query-local exact evidence and
 * fresh sketch evidence are expected to outrank persisted learned evidence; learned evidence is used when it is
 * applicable, calibrated, and more specific evidence is missing or untrusted.
 */
package org.eclipse.rdf4j.query.algebra.evaluation.optimizer.leo;
