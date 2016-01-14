/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.federation;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.sail.federation.Federation;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.Test;

/**
 * Tests for correct behavior when members define matching or conflicting
 * prefix/namespace maps.
 *
 * @author Dale Visser
 */
public class FederationNamespacesTest {

	private static String PREFIX = "test";

	private static String EXPECTED_NAME = "http://test/a#";

	private static Namespace EXPECTED_NAMESPACE = new SimpleNamespace(PREFIX, EXPECTED_NAME);

	@Test
	public void testTwoMatchingNamespaces()
		throws RepositoryException, RDFParseException, IOException
	{
		RepositoryConnection con = createFederationWithMemberNamespaces("a", "a");
		try {
			assertThat(con.getNamespace(PREFIX), is(equalTo(EXPECTED_NAME)));
			List<Namespace> asList = Iterations.asList(con.getNamespaces());
			assertThat(asList, hasItem(EXPECTED_NAMESPACE));
		}
		finally {
			con.close();
		}
	}

	@Test
	public void testThreeMatchingNamespaces()
		throws RepositoryException, RDFParseException, IOException
	{
		RepositoryConnection con = createFederationWithMemberNamespaces("a", "a", "a");
		try {
			assertThat(con.getNamespace(PREFIX), is(equalTo(EXPECTED_NAME)));
			List<Namespace> asList = Iterations.asList(con.getNamespaces());
			assertThat(asList, hasItem(EXPECTED_NAMESPACE));
		}
		finally {
			con.close();
		}
	}

	@Test
	public void testTwoMismatchedNamespaces()
		throws RepositoryException, RDFParseException, IOException
	{
		RepositoryConnection con = createFederationWithMemberNamespaces("a", "b");
		try {
			assertThat(con.getNamespace(PREFIX), is(nullValue()));
			List<Namespace> asList = Iterations.asList(con.getNamespaces());
			assertThat(asList, not(hasItem(EXPECTED_NAMESPACE)));
		}
		finally {
			con.close();
		}
	}

	@Test
	public void testThreeMismatchedNamespaces()
		throws RepositoryException, RDFParseException, IOException
	{
		RepositoryConnection con = createFederationWithMemberNamespaces("a", "b", "c");
		try {
			assertThat(con.getNamespace(PREFIX), is(nullValue()));
			List<Namespace> asList = Iterations.asList(con.getNamespaces());
			assertThat(asList, not(hasItem(EXPECTED_NAMESPACE)));
		}
		finally {
			con.close();
		}
	}

	private RepositoryConnection createFederationWithMemberNamespaces(String... paths)
		throws RepositoryException, RDFParseException, IOException
	{
		Federation federation = new Federation();
		for (int i = 0; i < paths.length; i++) {
			federation.addMember(createMember(Integer.toString(i), "http://test/" + paths[i] + "#"));
		}
		SailRepository repo = new SailRepository(federation);
		repo.initialize();
		return repo.getConnection();
	}

	private Repository createMember(String memberID, String name)
		throws RepositoryException, RDFParseException, IOException
	{
		SailRepository member = new SailRepository(new MemoryStore());
		member.initialize();
		SailRepositoryConnection con = member.getConnection();
		try {
			con.setNamespace(PREFIX, name);
		}
		finally {
			con.close();
		}
		return member;
	}
}
