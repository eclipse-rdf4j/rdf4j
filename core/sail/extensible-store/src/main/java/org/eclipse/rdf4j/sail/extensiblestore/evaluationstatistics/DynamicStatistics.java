package org.eclipse.rdf4j.sail.extensiblestore.evaluationstatistics;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

public interface DynamicStatistics {

	void add(Statement statement, boolean inferred);

	void remove(Statement statement, boolean inferred);

	void removeByQuery(Resource subj, IRI pred, Value obj, boolean inferred, Resource... contexts);

	/**
	 *
	 * @return 1 if stale, 0 if not stale, 0.5 if 50% stale. Seen as, given a random statement (that has either been
	 *         added, or removed), what is the probability that the statistics will return an incorrect result?
	 */
	double staleness(int count);
}
