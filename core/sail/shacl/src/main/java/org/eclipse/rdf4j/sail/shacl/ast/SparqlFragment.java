/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.shacl.ast;

import static org.eclipse.rdf4j.sail.shacl.ast.constraintcomponents.AbstractConstraintComponent.VALUES_INJECTION_POINT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SparqlFragment {

	final static Pattern REGEX_INDENT = Pattern.compile("(?m)^");

	private final String fragment;
	private final List<String> unionFragments = new ArrayList<>();

	private boolean filterCondition;
	private boolean bgp;
	private boolean union;

	// This is currently experimental!
	private static final boolean USE_UNION_PRESERVING_JOIN = false;

	private final List<StatementMatcher> statementMatchers = new ArrayList<>();

	private SparqlFragment(List<String> unionFragments, List<StatementMatcher> statementMatchers) {
		this.fragment = null;
		this.unionFragments.addAll(unionFragments);
		this.union = true;
		this.statementMatchers.addAll(statementMatchers);
	}

	private SparqlFragment(String fragment, boolean filterCondition, boolean bgp,
			List<StatementMatcher> statementMatchers) {
		this.fragment = fragment;
		this.filterCondition = filterCondition;
		this.bgp = bgp;
		this.statementMatchers.addAll(statementMatchers);

		assert filterCondition != bgp;
	}

	public static SparqlFragment filterCondition(String fragment, List<StatementMatcher> statementMatchers) {
		return new SparqlFragment(fragment, true, false, statementMatchers);
	}

	public static SparqlFragment bgp(String fragment, List<StatementMatcher> statementMatchers) {
		return new SparqlFragment(fragment, false, true, statementMatchers);
	}

	public static SparqlFragment and(List<SparqlFragment> sparqlFragments) {
		String collect = sparqlFragments.stream()
				.peek(s -> {
					assert s.filterCondition;
				})
				.map(SparqlFragment::getFragment)
				.collect(Collectors.joining(" ) && ( ", "( ",
						" )"));

		return filterCondition(collect,
				getStatementMatchers(sparqlFragments));
	}

	public static SparqlFragment or(List<SparqlFragment> sparqlFragments) {
		String collect = sparqlFragments.stream()
				.peek(s -> {
					assert s.filterCondition;
				})
				.map(SparqlFragment::getFragment)
				.collect(Collectors.joining(" ) || ( ", "( ",
						" )"));

		return filterCondition(collect,
				getStatementMatchers(sparqlFragments));
	}

	public static SparqlFragment join(List<SparqlFragment> sparqlFragments) {

		if (USE_UNION_PRESERVING_JOIN && sparqlFragments.stream().anyMatch(s1 -> s1.union)) {
			return unionPreservingJoin(sparqlFragments);

		} else {
			String collect = sparqlFragments.stream()
					.peek(s -> {
						assert !s.filterCondition;
					})
					.map(SparqlFragment::getFragment)
					.reduce((a, b) -> a + "\n" + b)
					.orElse("");

			return bgp(collect, getStatementMatchers(sparqlFragments));
		}
	}

	private static SparqlFragment unionPreservingJoin(List<SparqlFragment> sparqlFragments) {
		List<String> workingSet = new ArrayList<>();
		SparqlFragment firstSparqlFragment = sparqlFragments.get(0);
		if (firstSparqlFragment.union) {
			workingSet.addAll(firstSparqlFragment.unionFragments);
		} else {
			assert firstSparqlFragment.bgp;
			workingSet.add(firstSparqlFragment.fragment);
		}

		for (int i = 1; i < sparqlFragments.size(); i++) {
			SparqlFragment sparqlFragment = sparqlFragments.get(i);
			if (sparqlFragment.union) {

				List<String> newWorkingSet = new ArrayList<>();

				for (String unionFragment : sparqlFragment.unionFragments) {
					for (String workingSetFragment : workingSet) {
						newWorkingSet.add(workingSetFragment + "\n" + unionFragment);
					}
				}

				workingSet = newWorkingSet;

			} else {
				assert sparqlFragment.bgp;
				workingSet = workingSet
						.stream()
						.map(s -> sparqlFragment.fragment + "\n" + s)
						.collect(Collectors.toList());

			}
		}

		SparqlFragment union = union(workingSet.toArray(new String[0]));
		union.addStatementMatchers(getStatementMatchers(sparqlFragments));
		return union;
	}

	public static boolean isFilterCondition(List<SparqlFragment> sparqlFragments) {
		boolean isFilterCondtion = sparqlFragments
				.stream()
				.anyMatch(SparqlFragment::isFilterCondition);

		assert !isFilterCondtion || sparqlFragments.stream().allMatch(SparqlFragment::isFilterCondition);

		return isFilterCondtion;
	}

	public static SparqlFragment union(List<SparqlFragment> sparqlFragments) {
		List<String> sparqlFragmentString = sparqlFragments
				.stream()
				.map(SparqlFragment::getFragment)
				.collect(Collectors.toList());

		return new SparqlFragment(sparqlFragmentString, getStatementMatchers(sparqlFragments));
	}

	public static List<StatementMatcher> getStatementMatchers(List<SparqlFragment> sparqlFragments) {
		return sparqlFragments
				.stream()
				.flatMap(s -> s.statementMatchers.stream())
				.collect(Collectors.toList());
	}

	public static SparqlFragment union(String... query) {
		return new SparqlFragment(Arrays.asList(query), Collections.emptyList());
	}

	public String getFragment() {
		if (union) {
			return unionFragments.stream()
					.collect(
							Collectors.joining(
									"\n} UNION {\n" + VALUES_INJECTION_POINT + "\n",
									"{\n" + VALUES_INJECTION_POINT + "\n",
									"\n}"));
		}

		return fragment;
	}

	public boolean isFilterCondition() {
		return filterCondition;
	}

	public List<StatementMatcher> getStatementMatchers() {
		return statementMatchers;
	}

	public void addStatementMatchers(List<StatementMatcher> statementMatchers) {
		this.statementMatchers.addAll(statementMatchers);
	}

}
