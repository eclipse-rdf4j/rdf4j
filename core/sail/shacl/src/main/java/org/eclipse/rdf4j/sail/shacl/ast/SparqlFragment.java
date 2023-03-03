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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.sail.shacl.ast.paths.Path;
import org.eclipse.rdf4j.sail.shacl.ast.targets.EffectiveTarget;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;

public class SparqlFragment {

	// This is currently experimental!
	private static final boolean USE_UNION_PRESERVING_JOIN = false;

	private final String fragment;
	private final List<String> unionFragments = new ArrayList<>();
	private final List<StatementMatcher> statementMatchers = new ArrayList<>();
	private final TraceBack traceBackFunction;

	private boolean filterCondition;
	private boolean bgp;
	private boolean union;

	private SparqlFragment(String fragment, boolean filterCondition, boolean bgp,
			List<StatementMatcher> statementMatchers, TraceBack traceBackFunction) {
		this.fragment = fragment;
		this.filterCondition = filterCondition;
		this.bgp = bgp;
		this.statementMatchers.addAll(statementMatchers);
		this.traceBackFunction = traceBackFunction;

		assert filterCondition != bgp;
	}

	private SparqlFragment(List<String> unionFragments, List<StatementMatcher> statementMatchers,
			TraceBack traceBackFunction) {
		this.fragment = null;
		this.unionFragments.addAll(unionFragments);
		this.union = true;
		this.statementMatchers.addAll(statementMatchers);
		this.traceBackFunction = traceBackFunction;
	}

	public static SparqlFragment filterCondition(String fragment, List<StatementMatcher> statementMatchers) {
		return new SparqlFragment(fragment, true, false, statementMatchers, null);
	}

	public static SparqlFragment bgp(String fragment, List<StatementMatcher> statementMatchers) {
		return new SparqlFragment(fragment, false, true, statementMatchers, null);
	}

	public static SparqlFragment bgp(String fragment, List<StatementMatcher> statementMatchers,
			TraceBack traceBackFunction) {
		return new SparqlFragment(fragment, false, true, statementMatchers, traceBackFunction);
	}

	public static SparqlFragment bgp(String fragment, StatementMatcher statementMatcher) {
		return new SparqlFragment(fragment, false, true, List.of(statementMatcher), null);
	}

	public static SparqlFragment bgp(String fragment, StatementMatcher statementMatcher, TraceBack traceBackFunction) {
		return new SparqlFragment(fragment, false, true, List.of(statementMatcher), traceBackFunction);
	}

	public static SparqlFragment bgp(String fragment) {
		return new SparqlFragment(fragment, false, true, List.of(), null);
	}

	public static SparqlFragment bgp(String fragment, TraceBack traceBackFunction) {
		return new SparqlFragment(fragment, false, true, List.of(), traceBackFunction);
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
		return join(sparqlFragments, null);
	}

	public static SparqlFragment join(List<SparqlFragment> sparqlFragments, TraceBack traceBackFunction) {

		if (USE_UNION_PRESERVING_JOIN && sparqlFragments.stream().anyMatch(s1 -> s1.union)) {
			return unionPreservingJoin(sparqlFragments, traceBackFunction);

		} else {
			String collect = sparqlFragments.stream()
					.peek(s -> {
						assert !s.filterCondition;
					})
					.map(SparqlFragment::getFragment)
					.reduce((a, b) -> a + "\n" + b)
					.orElse("");

			return bgp(collect, getStatementMatchers(sparqlFragments), traceBackFunction);
		}
	}

	private static SparqlFragment unionPreservingJoin(List<SparqlFragment> sparqlFragments,
			TraceBack traceBackFunction) {
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

		SparqlFragment union = unionQueryStrings(workingSet, traceBackFunction);
		union.addStatementMatchers(getStatementMatchers(sparqlFragments));
		return union;
	}

	public static boolean isFilterCondition(List<SparqlFragment> sparqlFragments) {
		for (SparqlFragment sparqlFragment : sparqlFragments) {
			if (sparqlFragment.isFilterCondition()) {
				return true;
			}
		}
		return false;
	}

	public static List<StatementMatcher> getStatementMatchers(List<SparqlFragment> sparqlFragments) {
		return sparqlFragments
				.stream()
				.flatMap(s -> s.statementMatchers.stream())
				.collect(Collectors.toList());
	}

	public static SparqlFragment unionQueryStrings(List<String> query, TraceBack traceBackFunction) {
		return new SparqlFragment(query, Collections.emptyList(), traceBackFunction);
	}

	public static SparqlFragment unionQueryStrings(List<String> query) {
		return new SparqlFragment(query, Collections.emptyList(), null);
	}

	public static SparqlFragment union(List<SparqlFragment> sparqlFragments) {
		List<String> sparqlFragmentString = sparqlFragments
				.stream()
				.map(SparqlFragment::getFragment)
				.collect(Collectors.toList());

		return new SparqlFragment(sparqlFragmentString, getStatementMatchers(sparqlFragments), null);
	}

	public static SparqlFragment union(List<SparqlFragment> sparqlFragments, TraceBack traceBackFunction) {
		List<String> sparqlFragmentString = sparqlFragments
				.stream()
				.map(SparqlFragment::getFragment)
				.collect(Collectors.toList());

		return new SparqlFragment(sparqlFragmentString, getStatementMatchers(sparqlFragments), traceBackFunction);
	}

	public static SparqlFragment unionQueryStrings(String query1, String query2, String query3) {
		return new SparqlFragment(List.of(query1, query2, query3), Collections.emptyList(), null);
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

	@Override
	public String toString() {
		return "SparqlFragment{" +
				"fragment='" + fragment + '\'' +
				", unionFragments=" + unionFragments +
				", statementMatchers=" + statementMatchers +
				", filterCondition=" + filterCondition +
				", bgp=" + bgp +
				", union=" + union +
				'}';
	}

	public Stream<EffectiveTarget.StatementsAndMatcher> getRoot(ConnectionsGroup connectionsGroup, Resource[] dataGraph,
			Path path, StatementMatcher currentStatementMatcher, List<Statement> currentStatements) {
		assert traceBackFunction != null;
		return traceBackFunction.getRoot(connectionsGroup, dataGraph, path, currentStatementMatcher, currentStatements);
	}

	public interface TraceBack {
		Stream<EffectiveTarget.StatementsAndMatcher> getRoot(ConnectionsGroup connectionsGroup, Resource[] dataGraph,
				Path path, StatementMatcher currentStatementMatcher, List<Statement> currentStatements);
	}
}
