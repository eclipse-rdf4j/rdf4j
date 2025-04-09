/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.resultio.sparqlxml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.impl.TupleQueryResultBuilder;
import org.eclipse.rdf4j.query.resultio.sparqlxslx.SPARQLResultsXLSXWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Jerven Bolleman
 */
public class SPARQLXLSXTupleTest {

	@Test
	void simpleCase(@TempDir Path dir) throws IOException {
		Files.createDirectories(dir);

		Path tf = dir.resolve("test.xlsx");
//		Path tf = Paths.get("/home/jbollema/test.xlsx");
		TupleQueryResultBuilder b = new TupleQueryResultBuilder();
		b.startQueryResult(List.of("boolean", "iri", "int"));
		MapBindingSet bs = new MapBindingSet();
		bs.setBinding("boolean", SimpleValueFactory.getInstance().createLiteral(true));
		bs.setBinding("iri", SimpleValueFactory.getInstance().createIRI("https://example.org/iri"));
		bs.setBinding("int", SimpleValueFactory.getInstance().createLiteral(1));
		b.handleSolution(bs);
		MapBindingSet bs2 = new MapBindingSet();
		bs2.setBinding("boolean", SimpleValueFactory.getInstance().createLiteral(false));
		bs2.setBinding("iri", SimpleValueFactory.getInstance().createIRI("https://example.org/iri/test"));
		bs2.setBinding("int", SimpleValueFactory.getInstance().createLiteral(-9));
		b.handleSolution(bs2);
		List<String> links = List.of("http://example.org/link1");
		b.handleLinks(links);
		try (var out = Files.newOutputStream(tf)) {
			SPARQLResultsXLSXWriter writer = new SPARQLResultsXLSXWriter(out);
			writer.handleLinks(links);
			writer.startQueryResult(new ArrayList<>(bs.getBindingNames()));
			Iterator<BindingSet> iterator = b.getQueryResult().iterator();
			while (iterator.hasNext())
				writer.handleSolution(iterator.next());
			writer.endQueryResult();
		}
		assertTrue(Files.size(tf) > 0);

		try (XSSFWorkbook wb = new XSSFWorkbook(Files.newInputStream(tf))) {
			XSSFSheet raw = wb.getSheet("raw");
			assertNotNull(raw);
			assertNotNull(wb.getSheet("nice"));

			XSSFRow headerRow = raw.getRow(0);
			assertEquals("boolean", headerRow.getCell(0).getStringCellValue());
			assertEquals("iri", headerRow.getCell(1).getStringCellValue());
			assertEquals("int", headerRow.getCell(2).getStringCellValue());
		}
	}

}
