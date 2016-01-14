/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.repository.sail.nativerdf;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.rdf4j.common.io.FileUtil;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.parser.serql.SeRQLQueryTestCase;
import org.eclipse.rdf4j.sail.NotifyingSail;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;

import junit.framework.Test;

public class NativeSeRQLQueryTest extends SeRQLQueryTestCase {

	public static Test suite()
		throws Exception
	{
		return SeRQLQueryTestCase.suite(new Factory() {

			public Test createTest(String name, String dataFile, List<String> graphNames, String queryFile,
					String resultFile, String entailment)
			{
				return new NativeSeRQLQueryTest(name, dataFile, graphNames, queryFile, resultFile, entailment);
			}
		});
	}

	private File dataDir;

	public NativeSeRQLQueryTest(String name, String dataFile, List<String> graphNames, String queryFile,
			String resultFile, String entailment)
	{
		super(name, dataFile, graphNames, queryFile, resultFile, entailment);
	}

	@Override
	protected QueryLanguage getQueryLanguage() {
		return QueryLanguage.SERQL;
	}

	@Override
	protected NotifyingSail newSail()
		throws IOException
	{
		dataDir = FileUtil.createTempDir("nativestore");
		return new NativeStore(dataDir, "spoc");
	}
}
