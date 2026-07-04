/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.workbench.commands;

import java.util.List;

import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.manager.RepositoryInfo;
import org.eclipse.rdf4j.workbench.base.TransformationServlet;
import org.eclipse.rdf4j.workbench.util.TupleResultBuilder;

public class RepositoriesServlet extends TransformationServlet {

	@Override
	public void service(TupleResultBuilder builder, String xslPath)
			throws RepositoryException, QueryResultHandlerException {
		builder.transform(xslPath, "repositories.xsl");
		builder.start("readable", "writeable", "id", "description", "location");
		builder.link(List.of(INFO));
		for (RepositoryInfo info : manager.getAllRepositoryInfos()) {
			builder.result(info.isReadable(), info.isWritable(), info.getId(), info.getDescription(),
					info.getLocation().toString());
		}
		builder.end();
	}
}
