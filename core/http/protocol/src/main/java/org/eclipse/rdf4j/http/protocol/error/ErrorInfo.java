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
package org.eclipse.rdf4j.http.protocol.error;

/**
 * @author Herko ter Horst
 * @author Arjohn Kampman
 */
public class ErrorInfo {

	private final ErrorType errorType;

	private final String errMSg;

	public ErrorInfo(String errMsg) {
		this(null, errMsg);
	}

	public ErrorInfo(ErrorType errorType, String errMsg) {
		assert errMsg != null : "errMsg must not be null";
		this.errorType = errorType;
		this.errMSg = errMsg;
	}

	public ErrorType getErrorType() {
		return errorType;
	}

	public String getErrorMessage() {
		return errMSg;
	}

	@Override
	public String toString() {
		if (errorType != null) {
			StringBuilder sb = new StringBuilder(64);
			sb.append(errorType);
			sb.append(": ");
			sb.append(errMSg);
			return sb.toString();
		} else {
			return errMSg;
		}
	}

	/**
	 * Parses the string output that is produced by {@link #toString()}.
	 */
	public static ErrorInfo parse(String errInfoString) {
		String message = errInfoString;
		ErrorType errorType = null;

		int colonIdx = errInfoString.indexOf(':');
		if (colonIdx >= 0) {
			String label = errInfoString.substring(0, colonIdx).trim();
			errorType = ErrorType.forLabel(label);

			if (errorType != null) {
				message = errInfoString.substring(colonIdx + 1);
			}
		}

		return new ErrorInfo(errorType, message.trim());
	}
}
