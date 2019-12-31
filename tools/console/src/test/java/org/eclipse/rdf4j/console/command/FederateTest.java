/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console.command;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.http.config.HTTPRepositoryConfig;
import org.eclipse.rdf4j.repository.http.config.HTTPRepositoryFactory;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.sail.config.ProxyRepositoryConfig;
import org.eclipse.rdf4j.repository.sail.config.ProxyRepositoryFactory;
import org.eclipse.rdf4j.repository.sail.config.SailRepositoryConfig;
import org.eclipse.rdf4j.repository.sparql.config.SPARQLRepositoryConfig;
import org.eclipse.rdf4j.repository.sparql.config.SPARQLRepositoryFactory;
import org.eclipse.rdf4j.sail.federation.config.FederationConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.mockito.InjectMocks;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Unit tests for {@link Federate}.
 *
 * @author Dale Visser
 */
public class FederateTest extends AbstractCommandTest {

	private static final String FED_ID = "fedID";

	private static final String MEMORY_MEMBER_ID1 = "alien";

	private static final String MEMORY_MEMBER_ID2 = "scary";

	private static final String HTTP_MEMBER_ID = "http";

	private static final String HTTP2_MEMBER_ID = "http2";

	private static final String SPARQL_MEMBER_ID = "sparql";

	private static final String SPARQL2_MEMBER_ID = "sparql2";

	private static final String FED_DESCRIPTION = "Test Federation Title";

	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();

	@InjectMocks
	private Federate federate;

	@Before
	public void setUp() throws Exception {
		manager = new LocalRepositoryManager(tempDir.newFolder("federate-test-repository-manager"));
		addRepositories("federate", MEMORY_MEMBER_ID1, MEMORY_MEMBER_ID2, HTTP_MEMBER_ID, HTTP2_MEMBER_ID,
				SPARQL_MEMBER_ID, SPARQL2_MEMBER_ID);
		when(mockConsoleState.getManager()).thenReturn(manager);
		when(mockConsoleIO.readln("Federation Description (optional): ")).thenReturn(FED_DESCRIPTION);
	}

	private void execute(String... args) throws Exception {
		List<String> execArgs = new ArrayList<>(args.length + 1);
		execArgs.add("federate");
		Collections.addAll(execArgs, args);
		federate.execute(execArgs.toArray(new String[execArgs.size()]));
	}

	@Test
	public void testNoArgumentsPrintsHelp() throws Exception {
		execute();
		verify(mockConsoleIO).writeln(federate.getHelpLong());
	}

	@Test
	public void testOneArgumentPrintsHelp() throws Exception {
		execute(FED_ID);
		verify(mockConsoleIO).writeln(federate.getHelpLong());
	}

	@Test
	public void testTwoArgumentsPrintsHelp() throws Exception {
		execute(FED_ID, MEMORY_MEMBER_ID1);
		verify(mockConsoleIO).writeln(federate.getHelpLong());
	}

	@Test
	public void testInvalidArgumentPrintsError() throws Exception {
		execute("type=memory", FED_ID, MEMORY_MEMBER_ID1, MEMORY_MEMBER_ID2);
		verifyFailure();
	}

	@Test
	public void testDuplicateMembersPrintsError() throws Exception {
		execute(FED_ID, MEMORY_MEMBER_ID1, MEMORY_MEMBER_ID1);
		verifyFailure();
	}

	@Test
	public void testFedSameAsMemberPrintsError() throws Exception {
		execute(FED_ID, MEMORY_MEMBER_ID1, FED_ID, MEMORY_MEMBER_ID1);
		verifyFailure();
	}

	@Test
	public void testSparqlAndNotReadOnlyPrintsError() throws Exception {
		execute("readonly=false", FED_ID, SPARQL_MEMBER_ID, SPARQL2_MEMBER_ID);
		verifyFailure(SPARQL_MEMBER_ID + " is read-only.");
		verifyFailure(SPARQL2_MEMBER_ID + " is read-only.");
	}

	@Test
	public void testFedAlreadyExistsPrintsSpecificError() throws Exception {
		execute(MEMORY_MEMBER_ID1, FED_ID, MEMORY_MEMBER_ID2);
		verifyFailure(MEMORY_MEMBER_ID1 + " already exists.");
	}

	@Test
	public void testNonexistentMemberPrintsSpecificError() throws Exception {
		execute(FED_ID, MEMORY_MEMBER_ID1, "FreeLunch");
		verifyFailure("FreeLunch does not exist.");
	}

	@Test
	public void testFederateMemoryMembersSuccess() throws Exception {
		execute(FED_ID, MEMORY_MEMBER_ID1, MEMORY_MEMBER_ID2);
		verifySuccess(ProxyRepositoryFactory.REPOSITORY_TYPE, ProxyRepositoryFactory.REPOSITORY_TYPE);
		long expectedSize = getSize(MEMORY_MEMBER_ID1) + getSize(MEMORY_MEMBER_ID2);
		assertThat(getSize(FED_ID)).isEqualTo(expectedSize);
	}

	private long getSize(String memberID) throws Exception {
		try (RepositoryConnection connection = manager.getRepository(memberID).getConnection()) {
			return connection.size();
		}
	}

	@Test
	public void testFederateSucceedsWithHTTPandSPARQLmembers() throws Exception {
		execute(FED_ID, HTTP_MEMBER_ID, SPARQL_MEMBER_ID);
		verifySuccess(HTTPRepositoryFactory.REPOSITORY_TYPE, SPARQLRepositoryFactory.REPOSITORY_TYPE);
	}

	@Test
	public void testFederateHTTPtypeSucceeds() throws Exception {
		execute(FED_ID, HTTP_MEMBER_ID, HTTP2_MEMBER_ID);
		verifySuccess(HTTPRepositoryFactory.REPOSITORY_TYPE, HTTPRepositoryFactory.REPOSITORY_TYPE);
	}

	@Test
	public void testFederateSPARQLtypeSucceeds() throws Exception {
		execute(FED_ID, SPARQL_MEMBER_ID, SPARQL2_MEMBER_ID);
		verifySuccess(SPARQLRepositoryFactory.REPOSITORY_TYPE, SPARQLRepositoryFactory.REPOSITORY_TYPE);
	}

	@Test
	public void testSuccessWithNonDefaultReadonlyAndDistinct() throws Exception {
		execute(FED_ID, "distinct=true", "readonly=false", MEMORY_MEMBER_ID1, MEMORY_MEMBER_ID2);
		verifySuccess(false, true, ProxyRepositoryFactory.REPOSITORY_TYPE, ProxyRepositoryFactory.REPOSITORY_TYPE);
		long expectedSize = getSize(MEMORY_MEMBER_ID1) + getSize(MEMORY_MEMBER_ID2);
		assertThat(getSize(FED_ID)).isEqualTo(expectedSize);
	}

	@Test
	public void testFullyHeterogeneousSuccess() throws Exception {
		execute(FED_ID, SPARQL_MEMBER_ID, MEMORY_MEMBER_ID1, HTTP_MEMBER_ID);
		verifySuccess(SPARQLRepositoryFactory.REPOSITORY_TYPE, ProxyRepositoryFactory.REPOSITORY_TYPE,
				HTTPRepositoryFactory.REPOSITORY_TYPE);
	}

	private void verifySuccess(String... memberTypes) throws Exception {
		verifySuccess(true, false, memberTypes);
	}

	private void verifySuccess(boolean readonly, boolean distinct, String... memberTypes) throws Exception {
		assertThat(manager.hasRepositoryConfig(FED_ID)).isTrue();
		verify(mockConsoleIO, times(1)).readln("Federation Description (optional): ");
		verify(mockConsoleIO, times(1)).writeln("Federation created.");
		verify(mockConsoleIO, never()).writeError(anyString());
		assertThat(manager.getRepositoryInfo(FED_ID).getDescription()).isEqualTo(FED_DESCRIPTION);
		SailRepositoryConfig sailRepoConfig = (SailRepositoryConfig) manager.getRepositoryConfig(FED_ID)
				.getRepositoryImplConfig();
		FederationConfig fedSailConfig = (FederationConfig) sailRepoConfig.getSailImplConfig();
		assertThat(fedSailConfig.isReadOnly()).isEqualTo(readonly);
		assertThat(fedSailConfig.isDistinct()).isEqualTo(distinct);
		List<RepositoryImplConfig> members = fedSailConfig.getMembers();
		assertThat(members).hasSameSizeAs(memberTypes);
		int i = 0;
		for (RepositoryImplConfig ric : members) {
			String memberType = memberTypes[i];
			i++;
			assertThat(ric.getType()).isEqualTo(memberType);
			Class<? extends RepositoryImplConfig> implType;
			if (HTTPRepositoryFactory.REPOSITORY_TYPE.equals(memberType)) {
				implType = HTTPRepositoryConfig.class;
			} else if (SPARQLRepositoryFactory.REPOSITORY_TYPE.equals(memberType)) {
				implType = SPARQLRepositoryConfig.class;
			} else {
				implType = ProxyRepositoryConfig.class;
			}
			assertThat(ric).isInstanceOf(implType);
		}
	}

	private void verifyFailure(String... error) throws Exception {
		if (error.length > 0) {
			verify(mockConsoleIO).writeError(error[0]);
		} else {
			verify(mockConsoleIO).writeError(anyString());
		}
		assertThat(manager.hasRepositoryConfig(FED_ID)).isFalse();
	}
}
