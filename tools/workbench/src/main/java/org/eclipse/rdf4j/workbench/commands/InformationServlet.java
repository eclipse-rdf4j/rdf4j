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
import org.eclipse.rdf4j.workbench.base.TransformationServlet;
import org.eclipse.rdf4j.workbench.util.TupleResultBuilder;

public class InformationServlet extends TransformationServlet {

	@Override
	public void service(final TupleResultBuilder builder, final String xslPath)
			throws RepositoryException, QueryResultHandlerException {
		// final TupleResultBuilder builder = getTupleResultBuilder(req, resp);
		builder.transform(xslPath, "information.xsl");
		builder.start("version", "os", "jvm", "user", "memory-used", "maximum-memory");
		builder.link(List.of(INFO));
		final String version = this.appConfig.getVersion().toString();
		final String osName = getOsName();
		final String jvm = getJvmName();
		final String user = System.getProperty("user.name");
		final long total = Runtime.getRuntime().totalMemory();
		final long free = Runtime.getRuntime().freeMemory();
		final String used = ((total - free) / 1024 / 1024) + " MB";
		final String max = (Runtime.getRuntime().maxMemory() / 1024 / 1024) + " MB";
		builder.result(version, osName, jvm, user, used, max);
		builder.end();
	}

	private String getOsName() {
		final StringBuilder builder = new StringBuilder();
		builder.append(System.getProperty("os.name")).append(" ");
		builder.append(System.getProperty("os.version")).append(" (");
		builder.append(System.getProperty("os.arch")).append(")");
		return builder.toString();
	}

	private String getJvmName() {
		final StringBuilder builder = new StringBuilder();
		builder.append(System.getProperty("java.vm.vendor")).append(" ");
		builder.append(System.getProperty("java.vm.name")).append(" (");
		builder.append(System.getProperty("java.version")).append(")");
		return builder.toString();
	}

}
