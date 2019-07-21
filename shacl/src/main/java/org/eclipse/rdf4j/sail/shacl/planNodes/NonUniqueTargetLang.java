/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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
		this.parent = parent;
	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {

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
	public IteratorData getIteratorDataType() {
		return parent.getIteratorDataType();
	}

	@Override
	public void receiveLogger(ValidationExecutionLogger validationExecutionLogger) {
		this.validationExecutionLogger = validationExecutionLogger;
		parent.receiveLogger(validationExecutionLogger);
	}

}

class OnlyNonUnique extends LoggingCloseableIteration {

	private Tuple next;
	private Tuple previous;

	private Set<String> seenLanguages = new HashSet<>();

	private CloseableIteration<Tuple, SailException> parentIterator;

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
				if (!previous.line.get(0).equals(next.line.get(0))) {
					seenLanguages = new HashSet<>();
				}
			}

			previous = next;

			Value value = next.getlist().get(1);

			if (value instanceof Literal) {
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
	public void close() throws SailException {
		parentIterator.close();
	}

	@Override
	boolean localHasNext() throws SailException {
		calculateNext();
		return next != null;
	}

	@Override
	Tuple loggingNext() throws SailException {
		calculateNext();

		Tuple temp = next;
		next = null;
		return temp;
	}

	@Override
	public void remove() throws SailException {

	}

}