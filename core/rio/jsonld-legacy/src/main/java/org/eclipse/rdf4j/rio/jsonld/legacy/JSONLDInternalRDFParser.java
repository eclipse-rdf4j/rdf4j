/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/
package org.eclipse.rdf4j.rio.jsonld.legacy;

import java.util.Set;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.NamespaceAware;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;

import com.github.jsonldjava.core.JsonLdConsts;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.RDFDataset;

/**
 * A package private internal implementation class.
 *
 * @author Peter Ansell
 */
class JSONLDInternalRDFParser implements com.github.jsonldjava.core.RDFParser {

	public void setPrefix(RDFDataset result, String fullUri, String prefix) {
		result.setNamespace(fullUri, prefix);
	}

	public void handleStatement(RDFDataset result, Statement nextStatement) {
		// TODO: from a basic look at the code it seems some of these could be
		// null
		// null values for IRIs will probably break things further down the line
		// and i'm not sure yet if this should be something handled later on, or
		// something that should be checked here
		final String subject = getResourceValue(nextStatement.getSubject());
		final String predicate = getResourceValue(nextStatement.getPredicate());
		final Value object = nextStatement.getObject();
		final String graphName = getResourceValue(nextStatement.getContext());

		if (object instanceof Literal) {
			final Literal literal = (Literal) object;
			final String value = literal.getLabel();

			String datatype = getResourceValue(literal.getDatatype());

			// In RDF-1.1, Language Literals internally have the datatype
			// rdf:langString
			if (literal.getLanguage().isPresent() && datatype == null) {
				datatype = RDF.LANGSTRING.stringValue();
			}

			// In RDF-1.1, RDF-1.0 Plain Literals are now Typed Literals with
			// type xsd:String
			if (!literal.getLanguage().isPresent() && datatype == null) {
				datatype = XSD.STRING.stringValue();
			}

			result.addQuad(subject, predicate, value, datatype, literal.getLanguage().orElse(null), graphName);
		} else {
			result.addQuad(subject, predicate, getResourceValue((Resource) object), graphName);
		}
	}

	private String getResourceValue(Resource subject) {
		if (subject == null) {
			return null;
		} else if (subject instanceof IRI) {
			return subject.stringValue();
		} else if (subject instanceof BNode) {
			return JsonLdConsts.BLANK_NODE_PREFIX + subject.stringValue();
		}

		throw new IllegalStateException("Did not recognise resource type: " + subject.getClass().getName());
	}

	@Override
	public RDFDataset parse(Object input) throws JsonLdError {
		final RDFDataset result = new RDFDataset();
		if (input instanceof Statement) {
			handleStatement(result, (Statement) input);
		} else if (input instanceof Model) {
			if (input instanceof NamespaceAware) {
				final Set<Namespace> namespaces = ((NamespaceAware) input).getNamespaces();
				for (final Namespace nextNs : namespaces) {
					result.setNamespace(nextNs.getName(), nextNs.getPrefix());
				}
			}

			for (final Statement nextStatement : (Model) input) {
				handleStatement(result, nextStatement);
			}
		}
		return result;
	}

}
