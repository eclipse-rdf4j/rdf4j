package org.eclipse.rdf4j.query.resultio.sparqlxslx;

import java.io.OutputStream;

import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriter;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriterFactory;

public class SPARQLResultsXLSXWriterFactory implements TupleQueryResultWriterFactory {

	public SPARQLResultsXLSXWriterFactory() {
		super();
	}

	@Override
	public TupleQueryResultFormat getTupleQueryResultFormat() {
		return TupleQueryResultFormat.XSLX;
	}

	/**
	 * Returns a new instance of SPARQLResultsJSONWriter.
	 */
	@Override
	public TupleQueryResultWriter getWriter(OutputStream out) {
		return new SPARQLResultsXLSXWriter(out);
	}
}
