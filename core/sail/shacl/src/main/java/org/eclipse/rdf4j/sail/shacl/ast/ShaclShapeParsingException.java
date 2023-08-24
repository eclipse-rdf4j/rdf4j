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

package org.eclipse.rdf4j.sail.shacl.ast;

import org.eclipse.rdf4j.common.exception.RDF4JException;
import org.eclipse.rdf4j.model.Resource;

/**
 * An exception indicating that something went wrong when parsing a shape. The id field contains the subject of the
 * shape statements.
 */
public class ShaclShapeParsingException extends RDF4JException {

	Resource id;

	public ShaclShapeParsingException(String msg, Resource id) {
		super(msg + " - Caused by shape with id: " + resourceToString(id));
		this.id = id;
	}

	public ShaclShapeParsingException(String msg, Throwable t, Resource id) {
		super(msg + " - Caused by shape with id: " + resourceToString(id), t);
		this.id = id;
	}

	public ShaclShapeParsingException(Throwable t, Resource id) {
		super(t.getMessage() + " - Caused by shape with id: " + resourceToString(id), t);
		this.id = id;
	}

	public Resource getId() {
		return id;
	}

	private static String resourceToString(Resource id) {
		assert id != null;
		if (id == null) {
			return "null";
		}
		if (id.isIRI()) {
			return "<" + id.stringValue() + ">";
		}
		if (id.isBNode()) {
			return id.toString();
		}
		if (id.isTriple()) {
			return "TRIPLE " + id;
		}
		return id.toString();
	}
}
