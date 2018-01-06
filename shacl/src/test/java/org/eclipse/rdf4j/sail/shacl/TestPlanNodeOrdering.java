package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.shacl.mock.MockConsumePlanNode;
import org.eclipse.rdf4j.sail.shacl.plan.Tuple;
import org.eclipse.rdf4j.sail.shacl.planNodes.Select;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class TestPlanNodeOrdering {


	@Test
	public void testSelect() {
		SailRepository repository = new SailRepository(new MemoryStore());
		repository.initialize();

		try (SailRepositoryConnection connection = repository.getConnection()) {
			connection.begin(IsolationLevels.NONE);
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDFS.CLASS, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDFS.SUBCLASSOF, RDF.TYPE, RDFS.RESOURCE);
			connection.commit();
		}

		Select select = new Select(repository, "?a <" + RDF.TYPE + "> []");
		List<Tuple> tuples = new MockConsumePlanNode(select).asList();

		String actual = Arrays.toString(tuples.toArray());

		Collections.sort(tuples);

		String expected = Arrays.toString(tuples.toArray());


		assertEquals(expected, actual);

	}
}
