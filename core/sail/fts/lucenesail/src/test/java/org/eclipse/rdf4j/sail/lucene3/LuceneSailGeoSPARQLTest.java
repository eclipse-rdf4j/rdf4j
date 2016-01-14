/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lucene3;

import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.eclipse.rdf4j.sail.lucene.AbstractLuceneSailGeoSPARQLTest;
import org.eclipse.rdf4j.sail.lucene.LuceneSail;
import org.eclipse.rdf4j.sail.lucene3.LuceneIndex;

public class LuceneSailGeoSPARQLTest extends AbstractLuceneSailGeoSPARQLTest {
	private LuceneIndex index;

	@Override
	protected void configure(LuceneSail sail) throws IOException
	{
		index = new LuceneIndex(new RAMDirectory(), new StandardAnalyzer(Version.LUCENE_35));
		sail.setLuceneIndex(index);
	}
}
