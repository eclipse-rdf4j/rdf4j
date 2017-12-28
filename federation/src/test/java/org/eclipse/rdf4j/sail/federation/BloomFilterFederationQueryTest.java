/*******************************************************************************
 * Copyright (c) 2016 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.sail.federation;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.filters.AccurateRepositoryBloomFilter;
import org.eclipse.rdf4j.repository.filters.InaccurateRepositoryBloomFilter;
import org.eclipse.rdf4j.repository.filters.RepositoryBloomFilter;

public class BloomFilterFederationQueryTest extends FederationQueryTest {

	public BloomFilterFederationQueryTest(String name, String pattern) {
		super(name, pattern);
	}

	protected void configure(Federation federation)
		throws Exception
	{
		super.configure(federation);
		List<Repository> members = federation.getMembers();
		assertThat(members.size(), is(3));
		RepositoryBloomFilter bf1 = AccurateRepositoryBloomFilter.INCLUDE_INFERRED_INSTANCE;
		RepositoryBloomFilter bf2 = new InaccurateRepositoryBloomFilter();
		RepositoryBloomFilter bf3 = AccurateRepositoryBloomFilter.INCLUDE_INFERRED_INSTANCE;
		federation.setBloomFilter(members.get(0), bf1);
		federation.setBloomFilter(members.get(1), bf2);
		federation.setBloomFilter(members.get(2), bf3);
		Map<Repository, RepositoryBloomFilter> filters = federation.getBloomFilters();
		assertThat(filters.get(members.get(0)), is(bf1));
		assertThat(filters.get(members.get(1)), is(bf2));
		assertThat(filters.get(members.get(2)), is(bf3));
	}
}
