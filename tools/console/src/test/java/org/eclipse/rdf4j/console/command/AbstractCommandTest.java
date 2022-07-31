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
package org.eclipse.rdf4j.console.command;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.common.io.IOUtil;
import org.eclipse.rdf4j.console.ConsoleIO;
import org.eclipse.rdf4j.console.ConsoleState;
import org.eclipse.rdf4j.console.setting.ConsoleSetting;
import org.eclipse.rdf4j.console.setting.ConsoleWidth;
import org.eclipse.rdf4j.console.setting.Prefixes;
import org.eclipse.rdf4j.console.setting.QueryPrefix;
import org.eclipse.rdf4j.console.setting.ShowPrefix;
import org.eclipse.rdf4j.console.setting.WorkDir;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Dale Visser
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AbstractCommandTest {

	/*
	 * Switch off .silent() to debug specific tests and reenable it afterwards.
	 *
	 * Note, .silent() was added in Mockito 2, so has been removed until we update.
	 */

	@TempDir
	public File locationFile;

	protected RepositoryManager manager;

	@Mock
	protected ConsoleIO mockConsoleIO;

	@Mock
	protected ConsoleState mockConsoleState;

	protected Map<String, ConsoleSetting> defaultSettings = Stream.of(new Object[][] {
			{ ConsoleWidth.NAME, new ConsoleWidth() },
			{ Prefixes.NAME, new Prefixes() },
			{ QueryPrefix.NAME, new QueryPrefix() },
			{ ShowPrefix.NAME, new ShowPrefix() },
			{ WorkDir.NAME, new WorkDir() }
	}).collect(Collectors.toMap(m -> (String) m[0], m -> (ConsoleSetting) m[1]));

	@AfterEach
	public void tearDown() throws Exception {
		if (manager != null) {
			manager.shutDown();
		}
	}

	/**
	 * Copy file from resource to a specific path
	 *
	 * @param fromRes file to load from resources
	 * @param toFile  target file
	 * @throws IOException
	 */
	public void copyFromResource(String fromRes, File toFile) throws IOException {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		try (InputStream is = classLoader.getResourceAsStream(fromRes)) {
			Files.copy(is, toFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	/**
	 * Load triples or quads from a resource file into the repository
	 *
	 * @param repId repository ID
	 * @param data  URL of the resource
	 * @param file  name of the file
	 * @throws IOException
	 * @throws UnsupportedRDFormatException
	 */
	protected void loadData(String repId, URL data, String file) throws IOException, UnsupportedRDFormatException {
		RDFFormat fmt = Rio.getParserFormatForFileName(file)
				.orElseThrow(() -> new UnsupportedRDFormatException("No parser for " + file));

		try (RepositoryConnection connection = manager.getRepository(repId).getConnection()) {
			connection.add(data, null, fmt);
		}
	}

	/**
	 * Add one or more repositories to the repository manager, and load some content (if any).
	 *
	 * @param command    command / directory to load data from
	 * @param identities name of the repository / file to load
	 * @throws IOException
	 * @throws RDF4JException
	 */
	protected void addRepositories(String command, String... identities) throws IOException, RDF4JException {
		// file types to check for
		String[] filetypes = new String[2];
		filetypes[0] = "ttl";
		filetypes[1] = "trig";

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		for (String identity : identities) {
			InputStream cfg = classLoader.getResourceAsStream(command + "/" + identity + "-config.ttl");
			String repID = addRepository(cfg);

			for (String filetype : filetypes) {
				String file = identity + "." + filetype;
				URL res = classLoader.getResource(command + "/" + file);
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
		rdfParser.parse(
				new StringReader(IOUtil.readString(new InputStreamReader(configStream, StandardCharsets.UTF_8))),
				RepositoryConfigSchema.NAMESPACE);
		configStream.close();

		Resource repositoryNode = Models.subject(graph.filter(null, RDF.TYPE, RepositoryConfigSchema.REPOSITORY))
				.orElseThrow(() -> new RepositoryConfigException("could not find subject resource"));

		RepositoryConfig repoConfig = RepositoryConfig.create(graph, repositoryNode);
		repoConfig.validate();
		manager.addRepositoryConfig(repoConfig);

		String repId = Models.objectLiteral(graph.filter(repositoryNode, RepositoryConfigSchema.REPOSITORYID, null))
				.orElseThrow(() -> new RepositoryConfigException("missing repository id"))
				.stringValue();

		return repId;
	}

	/**
	 * Set working dir setting to root of temporarily folder
	 *
	 * @param cmd console command
	 */
	protected void setWorkingDir(ConsoleCommand cmd) {
		WorkDir location = new WorkDir(Paths.get(locationFile.getAbsolutePath()));
		cmd.settings.put(WorkDir.NAME, location);
	}
}
