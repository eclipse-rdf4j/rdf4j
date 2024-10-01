/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.http.server.repository.transaction;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;

/**
 * @author jeen
 *
 */
public class CommitController extends AbstractActionController {

	@Override
	protected ModelAndView handleAction(HttpServletRequest request, HttpServletResponse response,
			Transaction transaction) throws Exception {

		transaction.commit();
		// If commit fails with an exception, deregister should be skipped so the user
		// has a chance to do a proper rollback. See #725.
		ActiveTransactionRegistry.INSTANCE.deregister(transaction);

		return emptyOkResponse();
	}
}
