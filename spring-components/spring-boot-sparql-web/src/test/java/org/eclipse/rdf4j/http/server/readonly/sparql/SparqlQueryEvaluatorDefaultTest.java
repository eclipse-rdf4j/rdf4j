package org.eclipse.rdf4j.http.server.readonly.sparql;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Test;

public class SparqlQueryEvaluatorDefaultTest {

	private static final Resource CTX1 = Values.iri("http://example.com/ctx1");
	private static final IRI CTX2 = Values.iri("http://example.com/ctx2");
	private static final IRI TYP1 = Values.iri("http://example.com/typ1");
	private static final IRI TYP2 = Values.iri("http://example.com/typ2");

	@Test
	public void test() {

		Repository repo = new SailRepository(new MemoryStore());

		Model model = new ModelBuilder()
				.subject(Values.iri("http://example.com/user1/object1"))
					.add(TYP1, Values.literal("testValue_user1_obj1_1"))
					.add(TYP2, Values.literal("testValue_user1_obj1_2"))
				.subject(Values.iri("http://example.com/user1/object2"))
					.add(TYP1, Values.literal("testValue_user1_obj2_1"))
					.add(TYP2, Values.literal("testValue_user1_obj2_2"))
				.build();

		try (RepositoryConnection con = repo.getConnection()) {
			con.add(model, CTX1);
			con.getStatements(null, null, null).forEach(System.out::println);
		}
		
//		EvaluateResult evaluateResult = new EvaluateResultDefault(new ByteArrayOutputStream());
//		SparqlQueryEvaluator sparqlQueryEvaluator = new SparqlQueryEvaluatorDefault();
		
	}
}
