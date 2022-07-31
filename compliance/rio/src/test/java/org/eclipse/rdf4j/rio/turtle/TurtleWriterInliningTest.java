/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.turtle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.query.algebra.evaluation.util.ValueComparator;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.WriterConfig;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.junit.Test;

/**
 * Integration tests on blank node inlining behavior of the TurtleWriter.
 *
 * @implNote added as integration test instead of unit test because we reuse code from rdf4j-queryalgebra-evaluation
 *           (for statement sorting), which would introduce a cyclic dependency in the rio-turtle module.
 *
 * @author Jeen Broekstra
 */
public class TurtleWriterInliningTest {

	@Test
	public void testInlineList_CustomOrder() throws Exception {
		String inlinedListTurtle = "[] <http://www.z.org#a> (<http://www.z.org#o> <http://www.z.org#z>) .";
		Model model = Rio.parse(new StringReader(inlinedListTurtle), RDFFormat.TURTLE);

		ArrayList<Statement> statements = new ArrayList<>(model);

		// do custom sorting using a SPARQL algebra ValueComparator
		ValueComparator valueComparator = new ValueComparator();
		statements.sort(Comparator.comparing(Statement::getObject, valueComparator));

		model = new LinkedHashModel(statements);

		WriterConfig writerConfig = new WriterConfig();
		writerConfig.set(BasicWriterSettings.INLINE_BLANK_NODES, true);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Rio.write(model, baos, RDFFormat.TURTLE, writerConfig);

		String actual = baos.toString(StandardCharsets.UTF_8);

		assertThat(actual).contains(inlinedListTurtle);
	}
}
