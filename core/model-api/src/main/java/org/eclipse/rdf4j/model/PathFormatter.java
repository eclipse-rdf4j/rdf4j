/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 ******************************************************************************/

package org.eclipse.rdf4j.model;

import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Property path formatter.
 *
 * @author Alessandro Bollini
 * @since 3.7.0
 */
final class PathFormatter extends Path.Visitor<StringBuilder> {

	private final StringBuilder builder = new StringBuilder(10);

	@Override
	protected StringBuilder visit(Path.Hop hop) {

		inverse(hop);
		edge(hop.getIRI());
		count(hop);

		return builder;
	}

	@Override
	protected StringBuilder visit(Path.Seq seq) {

		inverse(seq);
		list(seq.getPaths(), '/', path -> path.accept(this));
		count(seq);
		return builder;
	}

	@Override
	protected StringBuilder visit(Path.Alt alt) {

		inverse(alt);
		list(alt.getPaths(), '|', path -> path.accept(this));
		count(alt);

		return builder;
	}

	@Override
	protected StringBuilder visit(Path.Not not) {

		inverse(not);
		builder.append('!');
		list(not.getSteps(), '|', this::step);
		count(not);

		return builder;
	}

	private void inverse(Link link) {
		if (link.isInverse()) {
			builder.append('^');
		}
	}

	private void step(Step step) {
		inverse(step);
		edge(step.getIRI());
	}

	private void edge(IRI iri) {
		builder.append('<').append(iri.stringValue()).append('>');
	}

	private void count(Path path) {
		if (path.isRepeatable()) {
			builder.append(path.isOptional() ? '*' : '+');
		} else if (path.isOptional()) {
			builder.append('?');
		}
	}

	private <T> void list(Iterable<T> paths, char separator, Consumer<T> formatter) {

		builder.append('(');

		for (final Iterator<T> iterator = paths.iterator(); iterator.hasNext();) {

			formatter.accept(iterator.next());

			if (iterator.hasNext()) {
				builder.append(separator);
			}

		}

		builder.append(')');
	}

}
