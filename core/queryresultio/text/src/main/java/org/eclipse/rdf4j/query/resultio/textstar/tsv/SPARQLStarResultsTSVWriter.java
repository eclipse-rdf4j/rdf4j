/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.textstar.tsv;

import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.text.tsv.SPARQLResultsTSVWriter;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Writer for SPARQL* TSV results. This is equivalent to the SPARQL TSV writer with the addition of support for RDF*
 * triples. Triples will be serialized in Turtle* fashion with the notable exception that any embedded literals will not
 * use the triple quotes notation (as regular literals in SPARQL TSV).
 *
 * @author Pavel Mihaylov
 */
public class SPARQLStarResultsTSVWriter extends SPARQLResultsTSVWriter {
	public SPARQLStarResultsTSVWriter(OutputStream out) {
		super(out);
	}

	@Override
	public TupleQueryResultFormat getTupleQueryResultFormat() {
		return TupleQueryResultFormat.TSV_STAR;
	}

	@Override
	protected void writeValue(Value val) throws IOException {
		if (val instanceof Triple) {
			writer.write("<<");
			writeValue(((Triple) val).getSubject());
			writer.write(' ');
			writeValue(((Triple) val).getPredicate());
			writer.write(' ');
			writeValue(((Triple) val).getObject());
			writer.write(">>");
		} else {
			super.writeValue(val);
		}
	}
}
