/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.repository;

import static org.eclipse.rdf4j.http.protocol.Protocol.QUERY_PARAM_NAME;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.common.lang.FileFormat;
import org.eclipse.rdf4j.http.server.repository.transaction.ActiveTransactionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.View;

/**
 * Base class for rendering query results.
 * 
 * @author Herko ter Horst
 * @author Arjohn Kampman
 */
public abstract class QueryResultView implements View {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	/**
	 * Key by which the query result is stored in the model.
	 */
	public static final String QUERY_RESULT_KEY = "queryResult";

	/**
	 * Key by which the query result writer factory is stored in the model.
	 */
	public static final String FACTORY_KEY = "factory";

	/**
	 * Key by which a filename hint is stored in the model. The filename hint may
	 * be used to present the client with a suggestion for a filename to use for
	 * storing the result.
	 */
	public static final String FILENAME_HINT_KEY = "filenameHint";

	/**
	 * Key by which the id of the current transaction is stored in the model. If
	 * this is present, the QueryResultView will take care to release the
	 * connection back to the
	 * {@link org.eclipse.rdf4j.http.server.repository.transaction.ActiveTransactionRegistry}
	 * after processing the query result.
	 * 
	 * @since 2.8.5
	 */
	public static final String TRANSACTION_ID_KEY = "transactionID";

	public static final String HEADERS_ONLY = "headersOnly";

	@SuppressWarnings("rawtypes")
	public final void render(Map model, HttpServletRequest request, HttpServletResponse response)
		throws IOException
	{
		UUID txnId = null; 
		try {
			txnId = (UUID)model.get(TRANSACTION_ID_KEY);
			renderInternal(model, request, response);
		}
		finally {
			if (txnId != null) {
				ActiveTransactionRegistry.INSTANCE.returnTransactionConnection(txnId);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	protected abstract void renderInternal(Map model, HttpServletRequest request, HttpServletResponse response)
		throws IOException;

	protected void setContentType(HttpServletResponse response, FileFormat fileFormat)
		throws IOException
	{
		String mimeType = fileFormat.getDefaultMIMEType();
		if (fileFormat.hasCharset()) {
			Charset charset = fileFormat.getCharset();
			mimeType += "; charset=" + charset.name();
		}
		response.setContentType(mimeType);
	}

	@SuppressWarnings("rawtypes")
	protected void setContentDisposition(Map model, HttpServletResponse response, FileFormat fileFormat)
		throws IOException
	{
		// Report as attachment to make use in browser more convenient
		String filename = (String)model.get(FILENAME_HINT_KEY);

		if (filename == null || filename.length() == 0) {
			filename = "result";
		}

		if (fileFormat.getDefaultFileExtension() != null) {
			filename += "." + fileFormat.getDefaultFileExtension();
		}

		response.setHeader("Content-Disposition", "attachment; filename=" + filename);
	}

	protected void logEndOfRequest(HttpServletRequest request) {
		if (logger.isInfoEnabled()) {
			String queryStr = request.getParameter(QUERY_PARAM_NAME);
			int qryCode = String.valueOf(queryStr).hashCode();
			logger.info("Request for query {} is finished", qryCode);
		}
	}

}
