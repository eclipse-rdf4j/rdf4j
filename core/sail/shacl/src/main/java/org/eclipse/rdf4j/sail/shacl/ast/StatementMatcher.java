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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SHACL;

public class StatementMatcher {

	private final Variable<? extends Resource> subject;
	private final Variable<IRI> predicate;
	private final Variable<? extends Value> object;

//	private final Set<String> varNames;
	private final Targetable origin;

	private final Set<String> inheritedVarNames;

	private List<StatementMatcher> subset = List.of();

	private final static Variable<Resource> NULL_SUBJECT = new Variable<>();
	private final static Variable<IRI> NULL_PREDICATE = new Variable<>();
	private final static Variable<Value> NULL_OBJECT = new Variable<>();

	public StatementMatcher(Variable<? extends Resource> subject, Variable<IRI> predicate,
			Variable<? extends Value> object, Targetable origin,
			Set<String> inheritedVarNames) {
		this.subject = Objects.requireNonNullElse(subject, NULL_SUBJECT);
		this.predicate = Objects.requireNonNullElse(predicate, NULL_PREDICATE);
		this.object = Objects.requireNonNullElse(object, NULL_OBJECT);

		this.origin = origin;
		this.inheritedVarNames = inheritedVarNames;

//		this.varNames = calculateVarNames(this.subject, this.predicate, this.object);

		assert this.subject.name == null || this.subject.value == null;
		assert this.predicate.name == null || this.predicate.value == null;
		assert this.object.name == null || this.object.value == null;

	}

	private static Set<String> calculateVarNames(Variable<?> subject, Variable<?> predicate, Variable<?> object) {
		if (subject.baseName == null && predicate.baseName == null && object.baseName == null) {
			if (subject.name != null) {
				if (predicate.name != null) {
					if (object.name != null) {
						return Set.of(subject.name, predicate.name, object.name);
					} else {
						return Set.of(subject.name, predicate.name);
					}
				} else {
					if (object.name != null) {
						return Set.of(subject.name, object.name);
					} else {
						return Set.of(subject.name);
					}
				}
			} else {
				if (predicate.name != null) {
					if (object.name != null) {
						return Set.of(predicate.name, object.name);
					} else {
						return Set.of(predicate.name);
					}
				} else {
					if (object.name != null) {
						return Set.of(object.name);
					} else {
						return Set.of();
					}
				}
			}
		} else {
			HashSet<String> varNames = new HashSet<>();
			if (subject.name != null) {
				varNames.add(subject.name);
			}
			if (subject.baseName != null) {
				varNames.add(subject.baseName);
			}

			if (predicate.name != null) {
				varNames.add(predicate.name);
			}
			if (predicate.baseName != null) {
				varNames.add(predicate.baseName);
			}

			if (object.name != null) {
				varNames.add(object.name);
			}
			if (object.baseName != null) {
				varNames.add(object.baseName);
			}
			return varNames;
		}
	}

	public static List<StatementMatcher> reduce(List<StatementMatcher> statementMatchers) {
		if (statementMatchers.size() == 1) {
			return statementMatchers;
		}

		List<StatementMatcher> wildcardMatchers = statementMatchers
				.stream()
				.filter(s -> s.subjectIsWildcard() || s.predicateIsWildcard() || s.objectIsWildcard())
				.collect(Collectors.toList());

		if (wildcardMatchers.isEmpty()) {
			return statementMatchers;
		}

		return statementMatchers
				.stream()
				.filter(s -> {
					for (StatementMatcher statementMatcher : wildcardMatchers) {
						if (statementMatcher != s && statementMatcher.covers(s)) {
							if (!s.hasSubset(statementMatcher)) {
								statementMatcher.addSubset(s);
								for (StatementMatcher matcher : s.subset) {
									statementMatcher.addSubset(matcher);
								}
								s.subset = List.of();
								return false;
							}
						}
					}

					return true;
				})
				.collect(Collectors.toList());

	}

	private void addSubset(StatementMatcher s) {
		if (subset.isEmpty()) {
			subset = List.of(s);
		} else if (subset.size() == 1) {
			subset = List.of(subset.get(0), s);
		} else if (subset.size() == 2) {
			subset = List.of(subset.get(0), subset.get(1), s);
		} else {
			if (subset.size() == 3) {
				subset = new ArrayList<>(subset);
			}
			subset.add(s);
		}

	}

	public static List<StatementMatcher> swap(List<StatementMatcher> statementMatchers, Variable<?> existingVariable,
			Variable<?> newVariable) {
		if (statementMatchers.size() == 0) {
			return List.of();
		}
		if (statementMatchers.size() == 1) {
			StatementMatcher statementMatcher = statementMatchers.get(0);
			return List.of(statementMatcher.swap(existingVariable, newVariable));
		}

		return statementMatchers
				.stream()
				.map(statementMatcher -> statementMatcher.swap(existingVariable, newVariable))
				.collect(Collectors.toList());

	}

	private static String formatForToString(String field, String name, Value value) {
		if (value == null && name == null) {
			return field + "[*]";
		}
		StringBuilder ret = new StringBuilder(field).append("[");
		if (name != null) {
			ret.append("\"").append(name).append("\"").append("=");
		}

		if (value == null) {
			ret.append("*");
		} else if (value.isIRI()) {
			IRI iri = (IRI) value;
			if (iri.getNamespace().equals(RDF.NAMESPACE)) {
				ret.append(RDF.PREFIX + ":").append(iri.getLocalName());
			} else if (iri.getNamespace().equals(SHACL.NAMESPACE)) {
				ret.append(SHACL.PREFIX + ":").append(iri.getLocalName());
			} else if (iri.getNamespace().equals(RDFS.NAMESPACE)) {
				ret.append(RDFS.PREFIX + ":").append(iri.getLocalName());
			} else {
				ret.append("<").append(iri).append(">");
			}
		} else {
			ret.append(value);
		}
		return ret.append("]").toString();
	}

	private StatementMatcher swap(Variable<?> existingVariable, Variable<?> newVariable) {
		String subjectName = getSubjectName();
		Resource subjectValue = getSubjectValue();
		String predicateName = getPredicateName();
		IRI predicateValue = getPredicateValue();
		String objectName = getObjectName();
		Value objectValue = getObjectValue();
		boolean changed = false;

		if (Objects.equals(existingVariable.name, subjectName)
				&& Objects.equals(existingVariable.value, subjectValue)) {
			changed = true;
			subjectName = newVariable.name;
			subjectValue = (Resource) newVariable.value;
		}

		if (Objects.equals(existingVariable.name, predicateName)
				&& Objects.equals(existingVariable.value, predicateValue)) {
			changed = true;
			predicateName = newVariable.name;
			predicateValue = (IRI) newVariable.value;
		}

		if (Objects.equals(existingVariable.name, objectName) && Objects.equals(existingVariable.value, objectValue)) {
			changed = true;
			objectName = newVariable.name;
			objectValue = newVariable.value;
		}

		if (changed) {
			assert subset.isEmpty();
			return new StatementMatcher(new Variable<>(subjectName, subjectValue),
					new Variable<>(predicateName, predicateValue), new Variable<>(objectName, objectValue), origin,
					inheritedVarNames);
		}
		return this;

	}

	public boolean covers(StatementMatcher s) {
		return covers(subject, s.subject)
				&& covers(predicate, s.predicate)
				&& covers(object, s.object);
	}

	private static boolean covers(Variable<?> bigger, Variable<?> smaller) {
		return Objects.equals(bigger.name, smaller.name)
				&& (bigger.isWildcard() || Objects.equals(bigger.value, smaller.value));
	}

	public String getSubjectName() {
		return subject.name;
	}

	public Resource getSubjectValue() {
		return subject.value;
	}

	public boolean subjectIsWildcard() {
		return subject.isWildcard();
	}

	public String getPredicateName() {
		return predicate.name;
	}

	public IRI getPredicateValue() {
		return predicate.value;
	}

	public boolean predicateIsWildcard() {
		return predicate.isWildcard();
	}

	public String getObjectName() {
		return object.name;
	}

	public Value getObjectValue() {
		return object.value;
	}

	public boolean objectIsWildcard() {
		return object.isWildcard();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		StatementMatcher that = (StatementMatcher) o;
		return Objects.equals(subject.name, that.subject.name) &&
				Objects.equals(subject.value, that.subject.value) &&
				Objects.equals(predicate.name, that.predicate.name) &&
				Objects.equals(predicate.value, that.predicate.value) &&
				Objects.equals(object.name, that.object.name) &&
				Objects.equals(object.value, that.object.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(subject, predicate, object);
	}

	public String getSparqlValuesDecl(Set<String> varNamesRestriction, boolean addInheritedVarNames,
			Set<String> varNamesInQueryFragment) {
		StringBuilder sb = new StringBuilder("VALUES ( ");
		if (subject.name != null && varNamesRestriction.contains(subject.name) ||
				subject.baseName != null && varNamesRestriction.contains(subject.baseName)) {
			if (varNamesInQueryFragment.contains(subject.name)) {
				sb.append("?").append(subject.name).append(" ");
			}
		}
		if (predicate.name != null && varNamesRestriction.contains(predicate.name) ||
				predicate.baseName != null && varNamesRestriction.contains(predicate.baseName)) {
			if (varNamesInQueryFragment.contains(predicate.name)) {
				sb.append("?").append(predicate.name).append(" ");
			}
		}
		if (object.name != null && varNamesRestriction.contains(object.name) ||
				object.baseName != null && varNamesRestriction.contains(object.baseName)) {
			if (varNamesInQueryFragment.contains(object.name)) {
				sb.append("?").append(object.name).append(" ");
			}
		}
		if (addInheritedVarNames) {
			for (String inheritedVarName : inheritedVarNames) {
				if (!inheritedVarName.equals(subject.name) &&
						!inheritedVarName.equals(predicate.name) &&
						!inheritedVarName.equals(object.name) &&

						varNamesRestriction.contains(inheritedVarName)) {
					if (varNamesInQueryFragment.contains(inheritedVarName)) {
						sb.append("?").append(inheritedVarName).append(" ");
					}
				}

			}
		}

		sb.append("){}\n");
		return sb.toString();
	}

	public Set<String> getVarNames(Set<String> varNamesRestriction, boolean addInheritedVarNames,
			Set<String> varNamesInQueryFragment) {
		if (varNamesRestriction.isEmpty()) {
			return Set.of();
		}

		HashSet<String> ret = new HashSet<>();
		if (subject.name != null && varNamesRestriction.contains(subject.name)
				&& varNamesInQueryFragment.contains(subject.name)) {
			ret.add(subject.name);
		} else if (subject.baseName != null && varNamesRestriction.contains(subject.baseName)
				&& varNamesInQueryFragment.contains(subject.name)) {
			ret.add(subject.name);
		}

		if (predicate.name != null && varNamesRestriction.contains(predicate.name)
				&& varNamesInQueryFragment.contains(predicate.name)) {
			ret.add(predicate.name);
		} else if (predicate.baseName != null && varNamesRestriction.contains(predicate.baseName)
				&& varNamesInQueryFragment.contains(predicate.name)) {
			ret.add(predicate.name);
		}

		if (object.name != null && varNamesRestriction.contains(object.name)
				&& varNamesInQueryFragment.contains(object.name)) {
			ret.add(object.name);
		} else if (object.baseName != null && varNamesRestriction.contains(object.baseName)
				&& varNamesInQueryFragment.contains(object.name)) {
			ret.add(object.name);
		}

		if (addInheritedVarNames) {
			for (String inheritedVarName : inheritedVarNames) {
				if (varNamesRestriction.contains(inheritedVarName)
						&& varNamesInQueryFragment.contains(inheritedVarName)) {
					ret.add(inheritedVarName);
				}
			}
		}

		return ret;
	}

	@Override
	public String toString() {
		return "StatementMatcher{ " +
				formatForToString("s", subject.name, subject.value) + ", " +
				formatForToString("p", predicate.name, predicate.value) + ", " +
				formatForToString("o", object.name, object.value) + " }";
	}

	public boolean hasSubset(StatementMatcher currentStatementMatcher) {
		for (StatementMatcher statementMatcher : subset) {
			if (currentStatementMatcher == statementMatcher) {
				return true;
			}
		}
		return false;
	}

	public Targetable getOrigin() {
		return origin;
	}

	public boolean hasSubject(Variable<Resource> variable) {
		if (subject.name == null) {
			return false;
		}
		// noinspection StringEquality
		if (variable.name == subject.name) {
			return true;
		}
		return variable.name.equals(subject.name);
	}

	public boolean hasObject(Variable<Value> variable) {
		if (object.name == null) {
			return false;
		}
		// noinspection StringEquality
		if (variable.name == object.name) {
			return true;
		}
		return variable.name.equals(object.name);
	}

	public static class StableRandomVariableProvider {

		// We just need a random base that isn't used elsewhere in the ShaclSail, but we don't want it to be stable so
		// we can compare the SPARQL queries where these variables are used
		private static final String BASE = UUID.randomUUID().toString().replace("-", "") + "_";
		// Best effort to store the highest value of all counters
		private static volatile int max = 0;
		private final String prefix;
		private int counter = -1;

		public StableRandomVariableProvider() {
			this.prefix = "";
		}

		public StableRandomVariableProvider(String prefix) {
			this.prefix = prefix;
		}

		/**
		 * Normalize the use of random variables in a SPARQL query so that the numbering of queries starts at 0 in
		 * increments of one.
		 *
		 * @param inputQuery the query string that should be normalized
		 * @return a normalized query string
		 */
		public static String normalize(String inputQuery) {
			if (!inputQuery.contains(BASE)) {
				return inputQuery;
			}

			// We don't want to go too high for performance reasons, so capping it at 100.
			int max = Math.min(100, StableRandomVariableProvider.max);

			int lowest = max;
			int highest = 0;
			boolean incrementsOfOne = true;
			int prev = -1;
			for (int i = 0; i <= max; i++) {
				if (inputQuery.contains(BASE + i + "_")) {
					lowest = Math.min(lowest, i);
					highest = Math.max(highest, i);
					if (prev >= 0 && prev + 1 != i) {
						incrementsOfOne = false;
					}
					prev = i;
				}
			}

			if (lowest == 0 && incrementsOfOne) {
				return inputQuery;
			}

			return normalizeRange(inputQuery, lowest, highest);
		}

		private static String normalizeRange(String inputQuery, int lowest, int highest) {

			String normalizedQuery = inputQuery;
			for (int i = 0; i <= highest; i++) {
				if (!normalizedQuery.contains(BASE + i + "_")) {
					for (int j = Math.max(i + 1, lowest); j <= highest; j++) {
						if (normalizedQuery.contains(BASE + j + "_")) {
							normalizedQuery = normalizedQuery.replace(BASE + j + "_", BASE + i + "_");
							break;
						}
					}
				}
			}

			return normalizedQuery;
		}

		public Variable<Value> next() {
			counter++;

			// this isn't really threadsafe, but that is ok because the variable is just used as a guide
			if (counter > max) {
				max = counter;
			}
			return current();
		}

		public Variable<Value> current() {
			if (counter < 0) {
				throw new IllegalStateException("next() has not been called");
			}
			return new Variable<>(prefix + BASE + counter + "_");
		}
	}

	public static class Variable<T extends Value> {
		public static final Variable<Value> VALUE = new Variable<>("value");
		public static final Variable<Value> THIS = new Variable<>("this");

		String name;
		T value;

		// the original name used to generate a temporary variable for complex paths
		String baseName;

		public Variable(String name, T value) {
			this.name = name;
			this.value = value;
		}

		public Variable(String name) {
			this.name = name;
		}

		public Variable(Variable<?> baseVariable, String name) {
			this.name = name;
			this.baseName = baseVariable.name;
		}

		public Variable(T value) {
			this.value = value;
		}

		public Variable() {
		}

		public String getName() {
			return name;
		}

		public T getValue() {
			return value;
		}

		public boolean isWildcard() {
			return value == null;
		}

		public String asSparqlVariable() {
			if (value != null) {
				throw new IllegalStateException(
						"Can not produce SPARQL variable for variables that have fixed values!");
			}
			return "?" + name.replace("-", "__");
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			Variable<?> variable = (Variable<?>) o;
			return Objects.equals(name, variable.name) &&
					Objects.equals(value, variable.value);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, value);
		}

		@Override
		public String toString() {
			return "Variable{" +
					"name='" + name + '\'' +
					", value=" + value +
					'}';
		}
	}
}
