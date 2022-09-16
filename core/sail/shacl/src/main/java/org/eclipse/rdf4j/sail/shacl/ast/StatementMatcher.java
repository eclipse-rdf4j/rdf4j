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

	private final String subjectName;
	private final Resource subjectValue;

	private final String predicateName;
	private final IRI predicateValue;

	private final String objectName;
	private final Value objectValue;
	private final Set<String> varNames;

	public StatementMatcher(String subjectName, Resource subjectValue, String predicateName, IRI predicateValue,
			String objectName, Value objectValue) {
		this.subjectName = subjectName;
		this.subjectValue = subjectValue;
		this.predicateName = predicateName;
		this.predicateValue = predicateValue;
		this.objectName = objectName;
		this.objectValue = objectValue;
		this.varNames = calculateVarNames(subjectName, predicateName, objectName);
	}

	private static Set<String> calculateVarNames(String subjectName, String predicateName, String objectName) {
		if (subjectName != null) {
			if (predicateName != null) {
				if (objectName != null) {
					return Set.of(subjectName, predicateName, objectName);
				} else {
					return Set.of(subjectName, predicateName);
				}
			} else {
				if (objectName != null) {
					return Set.of(subjectName, objectName);
				} else {
					return Set.of(subjectName);
				}
			}
		} else {
			if (predicateName != null) {
				if (objectName != null) {
					return Set.of(predicateName, objectName);
				} else {
					return Set.of(predicateName);
				}
			} else {
				if (objectName != null) {
					return Set.of(objectName);
				} else {
					return Set.of();
				}
			}
		}
	}

	public StatementMatcher(Variable subject, Variable predicate, Variable object) {
		this.subjectName = subject == null ? null : subject.name;
		this.subjectValue = subject == null ? null : (Resource) subject.value;
		this.predicateName = predicate == null ? null : predicate.name;
		this.predicateValue = predicate == null ? null : (IRI) predicate.value;
		this.objectName = object == null ? null : object.name;
		this.objectValue = object == null ? null : object.value;
		this.varNames = calculateVarNames(subjectName, predicateName, objectName);
	}

	public static List<StatementMatcher> reduce(Set<String> varNames, List<StatementMatcher> statementMatchers) {
		statementMatchers = statementMatchers
				.stream()
				.map(s -> {
					String subjectName = s.subjectName;
					if (subjectName != null && !varNames.contains(subjectName)) {
						subjectName = null;
					}

					String predicateName = s.predicateName;
					if (predicateName != null && !varNames.contains(predicateName)) {
						predicateName = null;
					}

					String objectName = s.objectName;
					if (objectName != null && !varNames.contains(objectName)) {
						objectName = null;
					}
					return new StatementMatcher(subjectName, s.subjectValue, predicateName, s.predicateValue,
							objectName, s.objectValue);

				})
				.collect(Collectors.toList());

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
							return false;
						}
					}

					return true;
				})
				.collect(Collectors.toList());

	}

	private boolean covers(StatementMatcher s) {

		if (subjectIsWildcard()) {
			if (s.subjectName != null) {
				return false;
			}
		} else {
			if (!Objects.equals(subjectName, s.subjectName)) {
				return false;
			}
			if (!Objects.equals(subjectValue, s.subjectValue)) {
				return false;
			}
		}

		if (predicateIsWildcard()) {
			if (s.predicateName != null) {
				return false;
			}
		} else {
			if (!Objects.equals(predicateName, s.predicateName)) {
				return false;
			}
			if (!Objects.equals(predicateValue, s.predicateValue)) {
				return false;
			}
		}

		if (objectIsWildcard()) {
			return s.objectName == null;
		} else {
			if (!Objects.equals(objectName, s.objectName)) {
				return false;
			}
			return Objects.equals(objectValue, s.objectValue);
		}

	}

	public String getSubjectName() {
		return subjectName;
	}

	public Resource getSubjectValue() {
		return subjectValue;
	}

	public boolean subjectIsWildcard() {
		return subjectName == null && subjectValue == null;
	}

	public String getPredicateName() {
		return predicateName;
	}

	public IRI getPredicateValue() {
		return predicateValue;
	}

	public boolean predicateIsWildcard() {
		return predicateName == null && predicateValue == null;
	}

	public String getObjectName() {
		return objectName;
	}

	public Value getObjectValue() {
		return objectValue;
	}

	public boolean objectIsWildcard() {
		return objectName == null && objectValue == null;
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
		return Objects.equals(subjectName, that.subjectName) &&
				Objects.equals(subjectValue, that.subjectValue) &&
				Objects.equals(predicateName, that.predicateName) &&
				Objects.equals(predicateValue, that.predicateValue) &&
				Objects.equals(objectName, that.objectName) &&
				Objects.equals(objectValue, that.objectValue);
	}

	@Override
	public int hashCode() {
		return Objects.hash(subjectName, subjectValue, predicateName, predicateValue, objectName, objectValue);
	}

	public String getSparqlValuesDecl() {
		StringBuilder sb = new StringBuilder("VALUES ( ");
		if (subjectName != null) {
			sb.append("?").append(subjectName).append(" ");
		}
		if (predicateName != null) {
			sb.append("?").append(predicateName).append(" ");
		}
		if (objectName != null) {
			sb.append("?").append(objectName).append(" ");
		}
		sb.append("){}\n");
		return sb.toString();
	}

	public Set<String> getVarNames() {
		return varNames;
	}

	public static class StableRandomVariableProvider {

		// We just need a random base that isn't used elsewhere in the ShaclSail, but we don't want it to be stable so
		// we can compare the SPARQL queries where these variables are used
		private static final String BASE = UUID.randomUUID().toString().replace("-", "") + "_";
		private final String prefix;

		// Best effort to store the highest value of all counters
		private static volatile int max = 0;

		private int counter = -1;

		public Variable next() {
			counter++;

			// this isn't really threadsafe, but that is ok because the variable is just used as a guide
			if (counter > max) {
				max = counter;
			}
			return current();
		}

		public Variable current() {
			if (counter < 0) {
				throw new IllegalStateException("next() has not been called");
			}
			return new Variable(prefix + BASE + counter + "_");
		}

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
	}

	public static class Variable {
		public static final Variable VALUE = new Variable("value");

		String name;
		Value value;

		public Variable(String name, Value value) {
			this.name = name;
			this.value = value;
		}

		public Variable(String name) {
			this.name = name;
		}

		public Variable(Value value) {
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public Value getValue() {
			return value;
		}

		public boolean isWildcard() {
			return name == null && value == null;
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
			Variable variable = (Variable) o;
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

	@Override
	public String toString() {
		return "StatementMatcher{ " +
				formatForToString("s", subjectName, subjectValue) + ", " +
				formatForToString("p", predicateName, predicateValue) + ", " +
				formatForToString("o", objectName, objectValue) + " }";
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
}
