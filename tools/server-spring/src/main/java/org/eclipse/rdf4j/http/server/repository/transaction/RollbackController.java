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

import org.eclipse.rdf4j.common.webapp.views.EmptySuccessView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextException;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author jeen
 *
 */
public class RollbackController extends AbstractActionController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public RollbackController() throws ApplicationContextException {
		setSupportedMethods("DELETE");
	}

	@Override
	protected ModelAndView handleAction(HttpServletRequest request, HttpServletResponse response,
			Transaction transaction) throws Exception {
		logger.info("transaction rollback");
		try {
			transaction.rollback();
		} finally {
			try {
				transaction.close();
			} finally {
				ActiveTransactionRegistry.INSTANCE.deregister(transaction);
			}
		}
		logger.info("transaction rollback request finished.");

		return new ModelAndView(EmptySuccessView.getInstance());
	}
}
