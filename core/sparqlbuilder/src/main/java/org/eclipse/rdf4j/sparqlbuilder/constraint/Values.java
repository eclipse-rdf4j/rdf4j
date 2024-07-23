/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sparqlbuilder.constraint;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfValue;

public class Values implements GraphPattern {
	Variable[] variables;
	RdfValue[][] solutionSequence;

	private static final RdfValue UNDEF = new RdfValue() {
		@Override
		public String getQueryString() {
			return "UNDEF";
		}
	};

	public Values(Variable[] variables, RdfValue[][] solutionSequence) {
		Objects.requireNonNull(solutionSequence);
		Objects.requireNonNull(solutionSequence);
		if (variables.length == 0) {
			throw new IllegalArgumentException("no variables provided for VALUES clause");
		}
		if (solutionSequence.length == 0
				|| solutionSequence[0] == null
				|| solutionSequence[0].length == 0) {
			throw new IllegalArgumentException("no values provided for VALUES clause");
		}
		if (solutionSequence[0].length != variables.length) {
			throw new IllegalArgumentException(
					solutionSequence[0].length
							+ " values provided  for "
							+ variables.length
							+ variables);
		}
		this.solutionSequence = solutionSequence;
		this.variables = variables;
	}

	@Override
	public String getQueryString() {
		StringBuilder sb = new StringBuilder();
		String parOpen = this.variables.length > 1 ? "( " : "";
		String parClose = this.variables.length > 1 ? ") " : "";
		sb.append("VALUES ").append(parOpen);
		for (int i = 0; i < variables.length; i++) {
			sb.append(variables[i].getQueryString()).append(" ");
		}
		sb.append(parClose).append("{").append(System.lineSeparator());
		for (int i = 0; i < solutionSequence.length; i++) {
			sb.append("  ").append(parOpen);
			for (int j = 0; j < solutionSequence[i].length; j++) {
				sb.append(solutionSequence[i][j].getQueryString()).append(" ");
			}
			sb.append(parClose).append(System.lineSeparator());
		}
		sb.append("}").append(System.lineSeparator());
		return sb.toString();
	}

	public static VariablesBuilder builder() {
		return new Builder();
	}

	public static class Builder implements VariablesBuilder, ValuesBuilder {
		public Builder() {
		}

		private List<Variable> variables = new ArrayList<>();

		private List<List<RdfValue>> values = new ArrayList<>();

		private List<RdfValue> currentValues = new ArrayList<>();

		@Override
		public VariablesBuilder variables(Variable... variable) {
			Arrays.stream(variable).forEach(this.variables::add);
			return this;
		}

		/**
		 * Provide another value. This will fill up the current solution sequence. If this value is the last one (i.e.
		 * the solution sequence now is of the same length as the list of variables), the current solution sequence is
		 * recorded and a new solution sequence begins.
		 *
		 * @param value
		 * @return
		 */
		@Override
		public ValuesBuilder value(RdfValue value) {
			this.currentValues.add(valueOrUndef(value));
			if (currentValues.size() >= variables.size()) {
				this.values.add(currentValues);
				currentValues = new ArrayList<>();
			}
			return this;
		}

		@Override
		public ValuesBuilder values(RdfValue... values) {
			if (this.variables.size() == 1) {
				for (int i = 0; i < values.length; i++) {
					this.values.add(List.of(valueOrUndef(values[i])));
				}
			} else if (this.variables.size() == values.length) {
				this.values.add(Stream.of(values).map(Values::valueOrUndef).collect(Collectors.toList()));
			} else {
				throw new IllegalArgumentException(
						"Provided list of values must match length of variables, or there must be only one variable.");
			}
			return this;
		}

		@Override
		public ValuesBuilder values(Collection<RdfValue> values) {
			return values(values.toArray(i -> new RdfValue[i]));
		}

		@Override
		public ValuesBuilder iriValue(IRI value) {
			return value(Rdf.iri(value));
		}

		@Override
		public ValuesBuilder iriValues(IRI... values) {
			return values(Stream.of(values).map(Rdf::iri).toArray(i -> new RdfValue[i]));
		}

		@Override
		public ValuesBuilder iriValues(Collection<IRI> values) {
			return iriValues(values.toArray(i -> new IRI[i]));
		}

		@Override
		public Values build() {
			if (this.values.isEmpty()) {
				throw new IllegalArgumentException("No values provided");
			}
			if (!this.currentValues.isEmpty()) {
				throw new IllegalArgumentException(
						"Current solution sequence is not finished - you added too few or too many values.");
			}
			RdfValue[][] values = new RdfValue[this.values.size()][this.variables.size()];
			for (int i = 0; i < this.values.size(); i++) {
				List<RdfValue> current = this.values.get(i);
				if (current.size() != this.variables.size()) {
					throw new IllegalArgumentException(
							String.format(
									"You provided $d values for $d variables",
									current.size(),
									this.variables.size()));
				}
				for (int j = 0; j < current.size(); j++) {
					values[i][j] = current.get(j);
				}
			}
			return new Values(this.variables.toArray(size -> new Variable[size]), values);
		}
	}

	public interface VariablesBuilder {

		public VariablesBuilder variables(Variable... variable);

		public ValuesBuilder value(RdfValue value);

		public ValuesBuilder values(RdfValue... values);

		public ValuesBuilder values(Collection<RdfValue> values);

		public ValuesBuilder iriValue(IRI value);

		public ValuesBuilder iriValues(IRI... values);

		public ValuesBuilder iriValues(Collection<IRI> values);
	}

	public interface ValuesBuilder {
		public ValuesBuilder value(RdfValue value);

		public ValuesBuilder values(RdfValue... values);

		public ValuesBuilder values(Collection<RdfValue> values);

		public ValuesBuilder iriValue(IRI value);

		public ValuesBuilder iriValues(IRI... values);

		public ValuesBuilder iriValues(Collection<IRI> values);

		public Values build();
	}

	private static RdfValue valueOrUndef(RdfValue value) {
		if (value == null) {
			return UNDEF;
		}
		return value;
	}

}
