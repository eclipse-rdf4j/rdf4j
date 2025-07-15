package org.eclipse.rdf4j.rio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import java.util.Collection;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.base.CoreDatatype;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.ParseErrorCollector;
import org.eclipse.rdf4j.rio.helpers.SimpleParseLocationListener;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.jupiter.api.BeforeEach;

/*
* Testing base class for Turtle, TriG, NTriples, and NQuads parsers
 */
public abstract class AbstractParserTest {
	protected RDFParser parser;
	protected ParseErrorCollector errorCollector = new ParseErrorCollector();
	protected StatementCollector statementCollector = new StatementCollector();
	protected TestParseLocationListener locationListener = new TestParseLocationListener();

	@BeforeEach
	public void setUp() {
		parser = createRDFParser();
		parser.setParseErrorListener(errorCollector);
		parser.setRDFHandler(statementCollector);
		parser.setParseLocationListener(locationListener);
	}

	protected void dirLangStringTestHelper(String data, String expectedLangString, boolean normalize,
			boolean shouldFail) {
		parser.getParserConfig().set(BasicParserSettings.FAIL_ON_UNKNOWN_LANGUAGES, true);
		parser.getParserConfig().set(BasicParserSettings.NORMALIZE_LANGUAGE_TAGS, normalize);

		try {
			parser.parse(new StringReader(data));

			if (shouldFail) {
				fail("default config should result in fatal error / parse exception");
			}

			assertThat(errorCollector.getErrors()).isEmpty();

			Collection<Statement> stmts = statementCollector.getStatements();

			assertThat(stmts).hasSize(1);

			Statement stmt = stmts.iterator().next();

			assertEquals(CoreDatatype.RDF.DIRLANGSTRING.getIri(), ((Literal) stmt.getObject()).getDatatype());
			assertTrue(((Literal) stmt.getObject()).getLanguage().isPresent());
			assertEquals(expectedLangString, ((Literal) stmt.getObject()).getLanguage().get());
		} catch (Exception e) {
			if (!shouldFail) {
				fail("parse error on correct data: " + e.getMessage());
			}
		}
	}

	protected void dirLangStringNoLanguageTestHelper(String data) {
		try {
			parser.parse(new StringReader(data));

			assertThat(errorCollector.getErrors()).isEmpty();

			Collection<Statement> stmts = statementCollector.getStatements();

			assertThat(stmts).hasSize(1);

			Statement stmt = stmts.iterator().next();

			assertEquals(CoreDatatype.XSD.STRING.getIri(), ((Literal) stmt.getObject()).getDatatype());
		} catch (Exception e) {
			fail("parse error on correct data: " + e.getMessage());
		}
	}

	protected abstract RDFParser createRDFParser();

	protected static class TestParseLocationListener extends SimpleParseLocationListener {

		public void assertListener(int row, int col) {
			assertEquals(row, this.getLineNo(), "Unexpected last row");
			assertEquals(col, this.getColumnNo(), "Unexpected last col");
		}

	}

}
