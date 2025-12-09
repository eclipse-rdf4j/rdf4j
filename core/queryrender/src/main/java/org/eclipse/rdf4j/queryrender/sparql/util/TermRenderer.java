/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.queryrender.sparql.util;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.eclipse.rdf4j.queryrender.sparql.PrefixIndex;
import org.eclipse.rdf4j.queryrender.sparql.PrefixIndex.PrefixHit;

/** Shared rendering helpers for IRIs and RDF4J Values. */
public final class TermRenderer {
	private TermRenderer() {
	}

	public static String convertIRIToString(final IRI iri, final PrefixIndex index, final boolean usePrefixCompaction) {
		final String s = iri.stringValue();
		if (usePrefixCompaction) {
			final PrefixHit hit = index.longestMatch(s);
			if (hit != null) {
				final String local = s.substring(hit.namespace.length());
				if (SparqlNameUtils.isPNLocal(local)) {
					return hit.prefix + ":" + local;
				}
			}
		}
		return "<" + s + ">";
	}

	public static String convertValueToString(final Value val, final PrefixIndex index,
			final boolean usePrefixCompaction) {
		if (val instanceof IRI) {
			return convertIRIToString((IRI) val, index, usePrefixCompaction);
		} else if (val instanceof Literal) {
			final Literal lit = (Literal) val;
			if (lit.getLanguage().isPresent()) {
				return "\"" + TextEscapes.escapeLiteral(lit.getLabel()) + "\"@" + lit.getLanguage().get();
			}
			final IRI dt = lit.getDatatype();
			final String label = lit.getLabel();
			if (XSD.BOOLEAN.equals(dt)) {
				return ("1".equals(label) || "true".equalsIgnoreCase(label)) ? "true" : "false";
			}
			if (XSD.INTEGER.equals(dt)) {
				try {
					return new BigInteger(label).toString();
				} catch (NumberFormatException ignore) {
				}
			}
			if (XSD.DECIMAL.equals(dt)) {
				try {
					return new BigDecimal(label).toPlainString();
				} catch (NumberFormatException ignore) {
				}
			}
			if (dt != null && !XSD.STRING.equals(dt)) {
				return "\"" + TextEscapes.escapeLiteral(label) + "\"^^"
						+ convertIRIToString(dt, index, usePrefixCompaction);
			}
			return "\"" + TextEscapes.escapeLiteral(label) + "\"";
		} else if (val instanceof BNode) {
			return "_:" + ((BNode) val).getID();
		} else if (val instanceof Triple) {
			Triple t = (Triple) val;
			// Render components recursively; nested triples are allowed.
			String s = convertValueToString(t.getSubject(), index, usePrefixCompaction);
			String p = convertValueToString(t.getPredicate(), index, usePrefixCompaction);
			String o = convertValueToString(t.getObject(), index, usePrefixCompaction);
			return "<<" + s + " " + p + " " + o + ">>";
		}
		return "\"" + TextEscapes.escapeLiteral(String.valueOf(val)) + "\"";
	}
}
