package org.eclipse.rdf4j.sail.shacl.ast.paths;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.DynamicModel;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.SHACL;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.Test;

public class PathTest {

	@Test
	public void simplePath() throws IOException {

		String pathString = "ex:1 sh:path ex:hasChild.";

		testPath(pathString);

	}

	@Test
	public void inversePath() throws IOException {

		String pathString = "ex:1 sh:path [sh:inversePath ex:childOf].";

		testPath(pathString);

	}

	@Test
	public void alternativePath() throws IOException {

		String pathString = "ex:1 sh:path [sh:alternativePath (ex:hasChild ex:child)].";

		testPath(pathString);

	}

	@Test
	public void sequencePath() throws IOException {

		String pathString = "ex:1 sh:path (ex:hasChild ex:hasChild) .";

		testPath(pathString);

	}

	@Test
	public void nestedSequencePath() throws IOException {

		String pathString = "ex:1 sh:path (ex:hasChild (ex:hasChild (((ex:hasChild ex:hasChild) (ex:hasChild ex:hasChild)) ex:hasChild))) .";

		testPath(pathString);

	}

	@Test
	public void zeroOrMorePath() throws IOException {

		String pathString = "ex:1 sh:path [sh:zeroOrMorePath ex:hasChild].";

		testPath(pathString);

	}

	@Test
	public void oneOrMorePath() throws IOException {

		String pathString = "ex:1 sh:path [sh:oneOrMorePath ex:hasChild].";

		testPath(pathString);

	}

	@Test
	public void zeroOrOnePath() throws IOException {

		String pathString = "ex:1 sh:path [sh:zeroOrOnePath ex:hasChild].";

		testPath(pathString);

	}

	@Test
	public void combination() throws IOException {

		String pathString = "ex:1 sh:path (ex:hasChild [sh:inversePath ex:childOf] [sh:zeroOrMorePath ex:hasChild]).";

		testPath(pathString);

	}

	@Test
	public void combination2() throws IOException {

		String pathString = "ex:1 sh:path [sh:inversePath (sh:childOf sh:childOf)].";

		testPath(pathString);

	}

	@Test
	public void combination3() throws IOException {

		String pathString = "ex:1 sh:path [sh:zeroOrMorePath [sh:inversePath ex:childOf]].";

		testPath(pathString);

	}

	private void testPath(String pathString) throws IOException {
		Model expected = Rio.parse(new StringReader("" +
				"@prefix sh: <http://www.w3.org/ns/shacl#>.\n" +
				"@prefix ex: <http://example.org/>.\n" +
				pathString), "", RDFFormat.TURTLE);

		DynamicModel actual = convertToPathAndBackToModel(expected);

		if (!Models.isomorphic(expected, actual)) {

			WriterConfig writerConfig = new WriterConfig();
			writerConfig.set(BasicWriterSettings.PRETTY_PRINT, true);
			writerConfig.set(BasicWriterSettings.INLINE_BLANK_NODES, true);

			expected.setNamespace(SHACL.PREFIX, SHACL.NAMESPACE);
			expected.setNamespace("ex", "http://example.org/");

			actual.setNamespace(SHACL.PREFIX, SHACL.NAMESPACE);
			actual.setNamespace("ex", "http://example.org/");

			System.out.println("Expected:");
			Rio.write(expected, System.out, RDFFormat.TURTLE, writerConfig);

			System.out.println("Actual:");
			Rio.write(actual, System.out, RDFFormat.TURTLE, writerConfig);

		}

		assertTrue(Models.isomorphic(actual, expected));
	}

	private DynamicModel convertToPathAndBackToModel(Model expected) {
		DynamicModel actual;
		SailRepository sailRepository = new SailRepository(new MemoryStore());
		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.add(expected);

			actual = connection.getStatements(null, SHACL.PATH, null).stream().map(s -> {
				Path path = Path.buildPath(connection, (Resource) s.getObject());
				DynamicModel model = new DynamicModelFactory().createEmptyModel();
				path.toModel((Resource) s.getObject(), null, model, new HashSet<>());

				model.add(s.getSubject(), SHACL.PATH, s.getObject());

				return model;
			}).reduce((m1, m2) -> {
				m1.addAll(m2);
				return m1;
			}).orElse(new DynamicModelFactory().createEmptyModel());
		} finally {
			sailRepository.shutDown();
		}

		return actual;
	}
}
