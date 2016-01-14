/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import org.eclipse.rdf4j.OpenRDFException;
import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.console.ConsoleIO;
import org.eclipse.rdf4j.model.Graph;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.GraphUtil;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema;
import org.eclipse.rdf4j.repository.config.RepositoryConfigUtil;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

/**
 * @author Dale Visser
 */
public class AbstractCommandTest {

	protected RepositoryManager manager;

	protected ConsoleIO streams = mock(ConsoleIO.class);

	protected final void addRepositories(String... identities)
		throws UnsupportedEncodingException, IOException, OpenRDFException
	{
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		for (String identity : identities) {
			addRepository(classLoader.getResourceAsStream("federate/" + identity + "-config.ttl"),
					classLoader.getResource("federate/" + identity + ".ttl"));
		}
	}

	protected void addRepository(InputStream configStream, URL data)
		throws UnsupportedEncodingException, IOException, OpenRDFException
	{
		Repository systemRepo = manager.getSystemRepository();
		RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE, systemRepo.getValueFactory());
		Model graph = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(graph));
		rdfParser.parse(new StringReader(IOUtil.readString(new InputStreamReader(configStream, "UTF-8"))),
				RepositoryConfigSchema.NAMESPACE);
		configStream.close();
		Resource repositoryNode = Models.subject(
				graph.filter(null, RDF.TYPE, RepositoryConfigSchema.REPOSITORY)).orElseThrow(
						() -> new RepositoryConfigException("could not find subject resource"));
		RepositoryConfig repoConfig = RepositoryConfig.create(graph, repositoryNode);
		repoConfig.validate();
		RepositoryConfigUtil.updateRepositoryConfigs(systemRepo, repoConfig);
		if (null != data) { // null if we didn't provide a data file
			final String repId = Models.objectLiteral(
					graph.filter(repositoryNode, RepositoryConfigSchema.REPOSITORYID, null)).orElseThrow(
							() -> new RepositoryConfigException("missing repository id")).stringValue();
			RepositoryConnection connection = manager.getRepository(repId).getConnection();
			try {
				connection.add(data, null, RDFFormat.TURTLE);
			}
			finally {
				connection.close();
			}
		}
	}

}
