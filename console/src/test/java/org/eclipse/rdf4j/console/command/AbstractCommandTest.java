/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console.command;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.console.ConsoleIO;
import org.eclipse.rdf4j.console.ConsoleState;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

import org.junit.After;
import org.junit.Rule;

import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * @author Dale Visser
 */
public class AbstractCommandTest {

	/*
	 * Switch off .silent() to debug specific tests and reenable it afterwards.
	 * 
	 * Note, .silent() was added in Mockito 2, so has been removed until we update.
	 */
	@Rule
	public MockitoRule abstractCommandTestMockitoRule = MockitoJUnit.rule(); //.silent();

	protected RepositoryManager manager;

	@Mock
	protected ConsoleIO mockConsoleIO;

	@Mock
	protected ConsoleState mockConsoleState;

	@After
	public void tearDown() throws Exception {
		if (manager != null) {
			manager.shutDown();
		}
	}

	/**
	 * Load triples or quads from a resource file into the repository
	 * 
	 * @param repId repository ID
	 * @param data URL of the resource
	 * @param file name of the file
	 * @throws IOException 
	 * @throws UnsupportedRDFormatException
	 */
	protected void loadData(String repId, URL data, String file) 
										throws IOException, UnsupportedRDFormatException {
		RDFFormat fmt = Rio.getParserFormatForFileName(file).orElseThrow(() ->
						new UnsupportedRDFormatException("No parser for " + file));
		
		try (RepositoryConnection connection = manager.getRepository(repId).getConnection()) {
			connection.add(data, null, fmt);
		}
	}
	
	/**
	 * Add one or more repositories to the repository manager, and load some content (if any).
	 * 
	 * @param identities
	 * @throws IOException
	 * @throws RDF4JException 
	 */
	protected void addRepositories(String... identities) throws IOException, RDF4JException {
		// file types to check for
		String[] filetypes = new String[2];
		filetypes[0] = "ttl";
		filetypes[1] = "trig";

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
	
		for (String identity : identities) {
			InputStream cfg = classLoader.getResourceAsStream("federate/" + identity + "-config.ttl");
			String repID = addRepository(cfg);

			for(String filetype: filetypes) {
				String file = identity + "." + filetype;
 				URL res = classLoader.getResource("federate/" + file);
				if (res != null) {
					loadData(repID, res, file);
				}
			}
		}
	}

	/***
	 * Add a new repository to the manager.
	 * 
	 * @param configStream input stream of the repository configuration
	 * @return ID of the repository as string
	 * @throws IOException
	 * @throws RDF4JException 
	 */
	protected String addRepository(InputStream configStream) throws IOException, RDF4JException {
		RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE, SimpleValueFactory.getInstance());

		Model graph = new LinkedHashModel();
		rdfParser.setRDFHandler(new StatementCollector(graph));
		rdfParser.parse(new StringReader(IOUtil.readString(
								new InputStreamReader(configStream, StandardCharsets.UTF_8))),
						RepositoryConfigSchema.NAMESPACE);
		configStream.close();
		
		Resource repositoryNode = Models.subject(
				graph.filter(null, RDF.TYPE, RepositoryConfigSchema.REPOSITORY)).orElseThrow(
						() -> new RepositoryConfigException("could not find subject resource"));
		
		RepositoryConfig repoConfig = RepositoryConfig.create(graph, repositoryNode);
		repoConfig.validate();
		manager.addRepositoryConfig(repoConfig);
		
		String repId = Models.objectLiteral(
			graph.filter(repositoryNode, RepositoryConfigSchema.REPOSITORYID, null)).orElseThrow(() -> 
					new RepositoryConfigException("missing repository id")).stringValue();
		
		return repId;
	}
}
