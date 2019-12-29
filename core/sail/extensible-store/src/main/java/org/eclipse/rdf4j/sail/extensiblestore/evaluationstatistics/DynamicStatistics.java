package org.eclipse.rdf4j.sail.extensiblestore.evaluationstatistics;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

public interface DynamicStatistics {

	void add(Statement statement, boolean inferred);

	void remove(Statement statement, boolean inferred);

	void removeByQuery(Resource subj, IRI pred, Value obj, boolean inferred, Resource... contexts);
}
