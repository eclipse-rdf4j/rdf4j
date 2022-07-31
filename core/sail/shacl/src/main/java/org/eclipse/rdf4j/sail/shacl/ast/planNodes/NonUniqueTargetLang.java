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

package org.eclipse.rdf4j.sail.shacl.ast.planNodes;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;

/**
 * This PlanNode takes a stream of Tuples like: (ex:companyA, "Company A"@en). It assumes that the stream is sorted on
 * index 0 (eg. ex:CompanyA). It will cache all non-empty languages from index 1 (eg. "en") and outputs any tuples where
 * the language has already been seen.
 *
 * If a Value on index 1 has no language because it is a literal without a language or because it is an IRI or BNode,
 * then its language is considered empty and not cached.
 *
 * @author Håvard Ottestad
 */
public class NonUniqueTargetLang implements PlanNode {
	PlanNode parent;
	private boolean printed = false;
	private ValidationExecutionLogger validationExecutionLogger;

	public NonUniqueTargetLang(PlanNode parent) {
		this.parent = PlanNodeHelper.handleSorting(this, parent);
	}

	@Override
	public CloseableIteration<? extends ValidationTuple, SailException> iterator() {

		return new OnlyNonUnique(parent, validationExecutionLogger);

	}

	@Override
	public int depth() {
		return parent.depth() + 1;
	}

	@Override
	public void getPlanAsGraphvizDot(StringBuilder stringBuilder) {
		if (printed) {
			return;
		}
		printed = true;
		stringBuilder.append(getId() + " [label=\"" + StringEscapeUtils.escapeJava(this.toString()) + "\"];")
				.append("\n");
		stringBuilder.append(parent.getId() + " -> " + getId()).append("\n");
		parent.getPlanAsGraphvizDot(stringBuilder);
	}

	@Override
	public String toString() {
		return "NonUniqueTargetLang";
	}

	@Override
	public String getId() {
		return System.identityHashCode(this) + "";
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		this.validationExecutionLogger = validationExecutionLogger;
		parent.receiveLogger(validationExecutionLogger);
	}

	@Override
	public boolean producesSorted() {
		return true;
	}

	@Override
	public boolean requiresSorted() {
		return true;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		NonUniqueTargetLang that = (NonUniqueTargetLang) o;
		return parent.equals(that.parent);
	}

	@Override
	public int hashCode() {
		return Objects.hash(parent);
	}
}

class OnlyNonUnique extends LoggingCloseableIteration {

	private ValidationTuple next;
	private ValidationTuple previous;

	private Set<String> seenLanguages = new HashSet<>();

	private final CloseableIteration<? extends ValidationTuple, SailException> parentIterator;

	OnlyNonUnique(PlanNode parent, ValidationExecutionLogger validationExecutionLogger) {
		super(parent, validationExecutionLogger);
		parentIterator = parent.iterator();
	}

	private void calculateNext() {
		if (next != null) {
			return;
		}

		while (next == null && parentIterator.hasNext()) {
			next = parentIterator.next();

			if ((previous != null)) {
				if (!previous.sameTargetAs(next)) {
					seenLanguages = new HashSet<>();
				}
			}

			previous = next;

			Value value = next.getValue();

			if (value.isLiteral()) {
				Optional<String> lang = ((Literal) value).getLanguage();

				if (!lang.isPresent()) {
					next = null;
				} else if (!seenLanguages.contains(lang.get())) {
					seenLanguages.add(lang.get());
					next = null;
				}

			} else {
				next = null;
			}

		}

	}

	@Override
	public void localClose() throws SailException {
		parentIterator.close();
	}

	@Override
	protected boolean localHasNext() throws SailException {
		calculateNext();
		return next != null;
	}

	@Override
	protected ValidationTuple loggingNext() throws SailException {
		calculateNext();

		ValidationTuple temp = next;
		next = null;
		return temp;
	}

}
