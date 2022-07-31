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
package org.eclipse.rdf4j.sail.extensiblestore.valuefactory;

import org.eclipse.rdf4j.model.Statement;

public interface ExtensibleStatementHelper {

	DefaultExtensibleStatementHelper defaultExtensibleStatementHelper = new DefaultExtensibleStatementHelper();

	static ExtensibleStatementHelper getDefaultImpl() {
		return defaultExtensibleStatementHelper;
	}

	ExtensibleStatement fromStatement(Statement statement, boolean inferred);

	class DefaultExtensibleStatementHelper implements ExtensibleStatementHelper {

		@Override
		public ExtensibleStatement fromStatement(Statement statement, boolean inferred) {
			if (statement instanceof ExtensibleStatement
					&& ((ExtensibleStatement) statement).isInferred() == inferred) {
				return (ExtensibleStatement) statement;
			}

			if (statement.getContext() != null) {
				return new ExtensibleContextStatement(statement.getSubject(), statement.getPredicate(),
						statement.getObject(), statement.getContext(), inferred);
			}

			return new ExtensibleStatementImpl(statement.getSubject(), statement.getPredicate(), statement.getObject(),
					inferred);

		}
	}

}
