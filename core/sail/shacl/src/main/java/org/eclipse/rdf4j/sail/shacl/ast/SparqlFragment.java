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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.sail.shacl.ast.paths.Path;
import org.eclipse.rdf4j.sail.shacl.ast.targets.EffectiveTarget;
import org.eclipse.rdf4j.sail.shacl.wrapper.data.ConnectionsGroup;

public class SparqlFragment {

	// This is currently experimental!
	private static final boolean USE_UNION_PRESERVING_JOIN = false;

	private final Set<Namespace> namespaces = new HashSet<>();

	private final String fragment;
	private final List<String> unionFragments = new ArrayList<>();
	private final List<StatementMatcher> statementMatchers = new ArrayList<>();
	private final TraceBack traceBackFunction;

	private boolean filterCondition;
	private boolean bgp;
	private boolean union;
	private final boolean supportsIncrementalEvaluation;

	private SparqlFragment(Collection<Namespace> namespaces, String fragment, boolean filterCondition, boolean bgp,
			List<StatementMatcher> statementMatchers, TraceBack traceBackFunction,
			boolean supportsIncrementalEvaluation) {
		this.namespaces.addAll(namespaces);
		this.fragment = fragment;
		this.filterCondition = filterCondition;
		this.bgp = bgp;
		this.statementMatchers.addAll(statementMatchers);
		this.traceBackFunction = traceBackFunction;
		this.supportsIncrementalEvaluation = supportsIncrementalEvaluation;
		assert filterCondition != bgp;
	}

	private SparqlFragment(Collection<Namespace> namespaces, List<String> unionFragments,
			List<StatementMatcher> statementMatchers,
			TraceBack traceBackFunction, boolean supportsIncrementalEvaluation) {
		this.namespaces.addAll(namespaces);
		this.fragment = null;
		this.unionFragments.addAll(unionFragments);
		this.union = true;
		this.statementMatchers.addAll(statementMatchers);
		this.traceBackFunction = traceBackFunction;
		this.supportsIncrementalEvaluation = supportsIncrementalEvaluation;
	}

	public static SparqlFragment filterCondition(Collection<Namespace> namespaces, String fragment,
			List<StatementMatcher> statementMatchers) {
		return new SparqlFragment(namespaces, fragment, true, false, statementMatchers, null, true);
	}

	public static SparqlFragment filterCondition(Collection<Namespace> namespaces, String fragment,
			List<StatementMatcher> statementMatchers,
			boolean supportsIncrementalEvaluation) {
		return new SparqlFragment(namespaces, fragment, true, false, statementMatchers, null,
				supportsIncrementalEvaluation);
	}

	public static SparqlFragment bgp(Collection<Namespace> namespaces, String query,
			boolean supportsIncrementalEvaluation) {
		return new SparqlFragment(namespaces, query, false, true, List.of(), null, supportsIncrementalEvaluation);
	}

	public static SparqlFragment bgp(Collection<Namespace> namespaces, String fragment,
			List<StatementMatcher> statementMatchers) {
		return new SparqlFragment(namespaces, fragment, false, true, statementMatchers, null, true);
	}

	public static SparqlFragment bgp(Collection<Namespace> namespaces, String fragment,
			List<StatementMatcher> statementMatchers,
			TraceBack traceBackFunction) {
		return new SparqlFragment(namespaces, fragment, false, true, statementMatchers, traceBackFunction, true);
	}

	public static SparqlFragment bgp(Collection<Namespace> namespaces, String fragment,
			List<StatementMatcher> statementMatchers,
			TraceBack traceBackFunction, boolean supportsIncrementalEvaluation) {
		return new SparqlFragment(namespaces, fragment, false, true, statementMatchers, traceBackFunction,
				supportsIncrementalEvaluation);
	}

	public static SparqlFragment bgp(Collection<Namespace> namespaces, String fragment,
			StatementMatcher statementMatcher) {
		return new SparqlFragment(namespaces, fragment, false, true, List.of(statementMatcher), null, true);
	}

	public static SparqlFragment bgp(Collection<Namespace> namespaces, String fragment,
			StatementMatcher statementMatcher, TraceBack traceBackFunction) {
		return new SparqlFragment(namespaces, fragment, false, true, List.of(statementMatcher), traceBackFunction,
				true);
	}

	public static SparqlFragment bgp(Collection<Namespace> namespaces, String fragment) {
		return new SparqlFragment(namespaces, fragment, false, true, List.of(), null, true);
	}

	public static SparqlFragment and(List<SparqlFragment> sparqlFragments) {
		String collect = sparqlFragments.stream()
				.peek(s -> {
					assert s.filterCondition;
				})
				.map(SparqlFragment::getFragment)
				.collect(Collectors.joining(" ) && ( ", "( ",
						" )"));

		boolean supportsIncrementalEvaluation = sparqlFragments.stream()
				.allMatch(SparqlFragment::supportsIncrementalEvaluation);

		Set<Namespace> namespaces = sparqlFragments.stream()
				.flatMap(s -> s.namespaces.stream())
				.collect(Collectors.toSet());

		return filterCondition(namespaces, collect,
				getStatementMatchers(sparqlFragments), supportsIncrementalEvaluation);
	}

	public static SparqlFragment or(List<SparqlFragment> sparqlFragments) {
		String collect = sparqlFragments.stream()
				.peek(s -> {
					assert s.filterCondition;
				})
				.map(SparqlFragment::getFragment)
				.collect(Collectors.joining(" ) || ( ", "( ",
						" )"));

		boolean supportsIncrementalEvaluation = sparqlFragments.stream()
				.allMatch(SparqlFragment::supportsIncrementalEvaluation);

		Set<Namespace> namespaces = sparqlFragments.stream()
				.flatMap(s -> s.namespaces.stream())
				.collect(Collectors.toSet());

		return filterCondition(namespaces, collect,
				getStatementMatchers(sparqlFragments), supportsIncrementalEvaluation);
	}

	public static SparqlFragment join(List<SparqlFragment> sparqlFragments) {
		return join(sparqlFragments, null);
	}

	public static SparqlFragment join(List<SparqlFragment> sparqlFragments, TraceBack traceBackFunction) {

		if (USE_UNION_PRESERVING_JOIN && sparqlFragments.stream().anyMatch(s1 -> s1.union)) {
			return unionPreservingJoin(sparqlFragments, traceBackFunction);

		} else {
			String queryFragment = sparqlFragments.stream()
					.peek(s -> {
						assert !s.filterCondition;
					})
					.map(SparqlFragment::getFragment)
					.reduce((a, b) -> a + "\n" + b)
					.orElse("");

			boolean supportsIncrementalEvaluation = sparqlFragments.stream()
					.allMatch(SparqlFragment::supportsIncrementalEvaluation);

			Set<Namespace> namespaces = sparqlFragments.stream()
					.flatMap(s -> s.namespaces.stream())
					.collect(Collectors.toSet());

			return bgp(namespaces, queryFragment, getStatementMatchers(sparqlFragments), traceBackFunction,
					supportsIncrementalEvaluation);
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

		boolean supportsIncrementalEvaluation = sparqlFragments.stream()
				.allMatch(SparqlFragment::supportsIncrementalEvaluation);

		Set<Namespace> namespaces = sparqlFragments.stream()
				.flatMap(s -> s.namespaces.stream())
				.collect(Collectors.toSet());

		SparqlFragment union = unionQueryStrings(namespaces, workingSet, traceBackFunction,
				supportsIncrementalEvaluation);
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

	public static SparqlFragment unionQueryStrings(Set<Namespace> namespaces, List<String> query,
			TraceBack traceBackFunction,
			boolean supportsIncrementalEvaluation) {
		return new SparqlFragment(namespaces, query, Collections.emptyList(), traceBackFunction,
				supportsIncrementalEvaluation);
	}

	public static SparqlFragment union(List<SparqlFragment> sparqlFragments) {
		List<String> sparqlFragmentString = sparqlFragments
				.stream()
				.map(SparqlFragment::getFragment)
				.collect(Collectors.toList());

		boolean supportsIncrementalEvaluation = sparqlFragments.stream()
				.allMatch(SparqlFragment::supportsIncrementalEvaluation);

		Set<Namespace> namespaces = sparqlFragments.stream()
				.flatMap(s -> s.namespaces.stream())
				.collect(Collectors.toSet());

		return new SparqlFragment(namespaces, sparqlFragmentString, getStatementMatchers(sparqlFragments), null,
				supportsIncrementalEvaluation);
	}

	public static SparqlFragment union(List<SparqlFragment> sparqlFragments, TraceBack traceBackFunction) {
		List<String> sparqlFragmentString = sparqlFragments
				.stream()
				.map(SparqlFragment::getFragment)
				.collect(Collectors.toList());

		boolean supportsIncrementalEvaluation = sparqlFragments.stream()
				.allMatch(SparqlFragment::supportsIncrementalEvaluation);

		Set<Namespace> namespaces = sparqlFragments.stream()
				.flatMap(s -> s.namespaces.stream())
				.collect(Collectors.toSet());

		return new SparqlFragment(namespaces, sparqlFragmentString, getStatementMatchers(sparqlFragments),
				traceBackFunction,
				supportsIncrementalEvaluation);
	}

	public static SparqlFragment unionQueryStrings(Set<Namespace> namespaces, String query1, String query2,
			String query3,
			boolean supportsIncrementalEvaluation) {
		return new SparqlFragment(namespaces, List.of(query1, query2, query3), Collections.emptyList(), null,
				supportsIncrementalEvaluation);
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

	public boolean supportsIncrementalEvaluation() {
		return supportsIncrementalEvaluation;
	}

	public String getNamespacesForSparql() {
		return ShaclPrefixParser.toSparqlPrefixes(namespaces);
	}

	public Stream<EffectiveTarget.StatementsAndMatcher> getRoot(ConnectionsGroup connectionsGroup, Resource[] dataGraph,
			Path path, StatementMatcher currentStatementMatcher, List<Statement> currentStatements) {
		assert traceBackFunction != null;
		return traceBackFunction.getRoot(connectionsGroup, dataGraph, path, currentStatementMatcher, currentStatements);
	}

	@Override
	public String toString() {
		return "SparqlFragment{" +
				"fragment='" + fragment + '\'' +
				", unionFragments=" + unionFragments +
				", statementMatchers=" + statementMatchers +
				", traceBackFunction=" + traceBackFunction +
				", filterCondition=" + filterCondition +
				", bgp=" + bgp +
				", union=" + union +
				", supportsIncrementalEvaluation=" + supportsIncrementalEvaluation +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		SparqlFragment that = (SparqlFragment) o;

		if (filterCondition != that.filterCondition) {
			return false;
		}
		if (bgp != that.bgp) {
			return false;
		}
		if (union != that.union) {
			return false;
		}
		if (supportsIncrementalEvaluation != that.supportsIncrementalEvaluation) {
			return false;
		}
		if (!namespaces.equals(that.namespaces)) {
			return false;
		}
		if (!Objects.equals(fragment, that.fragment)) {
			return false;
		}
		if (!unionFragments.equals(that.unionFragments)) {
			return false;
		}
		if (!statementMatchers.equals(that.statementMatchers)) {
			return false;
		}
		return Objects.equals(traceBackFunction, that.traceBackFunction);
	}

	@Override
	public int hashCode() {
		int result = namespaces.hashCode();
		result = 31 * result + (fragment != null ? fragment.hashCode() : 0);
		result = 31 * result + unionFragments.hashCode();
		result = 31 * result + statementMatchers.hashCode();
		result = 31 * result + (traceBackFunction != null ? traceBackFunction.hashCode() : 0);
		result = 31 * result + (filterCondition ? 1 : 0);
		result = 31 * result + (bgp ? 1 : 0);
		result = 31 * result + (union ? 1 : 0);
		result = 31 * result + (supportsIncrementalEvaluation ? 1 : 0);
		return result;
	}

	public interface TraceBack {

		Stream<EffectiveTarget.StatementsAndMatcher> getRoot(
				ConnectionsGroup connectionsGroup,
				Resource[] dataGraph,
				Path path,
				StatementMatcher currentStatementMatcher,
				List<Statement> currentStatements);
	}

}
