/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.exceptions;

import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.model.Model;

/**
 * MAY BE MOVED IN THE FUTURE!
 */
@Experimental
public interface ValidationException {

	/**
	 * @return A Model containing the validation report as specified by the SHACL Recommendation
	 */
	Model validationReportAsModel();

}
