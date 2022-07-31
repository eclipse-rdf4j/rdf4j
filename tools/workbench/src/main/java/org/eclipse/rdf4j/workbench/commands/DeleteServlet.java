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

import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.manager.RepositoryInfo;
import org.eclipse.rdf4j.workbench.base.TransformationServlet;
import org.eclipse.rdf4j.workbench.util.TupleResultBuilder;
import org.eclipse.rdf4j.workbench.util.WorkbenchRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Servlet responsible for presenting the list of repositories, and deleting the chosen one.
 */
public class DeleteServlet extends TransformationServlet {

	private static final ObjectMapper mapper = new ObjectMapper();

	/**
	 * Deletes the repository with the given ID, then redirects to the repository selection page. If given a "checkSafe"
	 * parameter, instead returns JSON response with safe field set to true if safe, false if not.
	 */
	@Override
	protected void doPost(WorkbenchRequest req, HttpServletResponse resp, String xslPath) throws Exception {
		dropRepository(req.getParameter("id"));
		resp.sendRedirect("../");
	}

	@Override
	protected void service(WorkbenchRequest req, HttpServletResponse resp, String xslPath) throws Exception {
		String checkSafe = req.getParameter("checkSafe");
		if (null == checkSafe) {
			// Display the form.
			super.service(req, resp, xslPath);
		} else {
			// Respond to 'checkSafe' XmlHttpRequest with JSON.
			ObjectNode jsonObject = mapper.createObjectNode();
			jsonObject.put("safe", manager.isSafeToRemove(checkSafe));
			final PrintWriter writer = new PrintWriter(new BufferedWriter(resp.getWriter()));
			writer.write(mapper.writeValueAsString(jsonObject));
			writer.flush();
		}

	}

	private void dropRepository(String identity) throws RepositoryException, RepositoryConfigException {
		manager.removeRepository(identity);
	}

	/**
	 * Presents a page where the user can choose a repository ID to delete.
	 */
	@Override
	public void service(TupleResultBuilder builder, String xslPath)
			throws RepositoryException, QueryResultHandlerException {
		builder.transform(xslPath, "delete.xsl");
		builder.start("readable", "writeable", "id", "description", "location");
		builder.link(List.of(INFO));
		for (RepositoryInfo info : manager.getAllRepositoryInfos()) {
			builder.result(info.isReadable(), info.isWritable(), info.getId(), info.getDescription(),
					info.getLocation());
		}
		builder.end();
	}

}
