/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.binary;


class BinaryRDFConstants {

	/**
	 * Magic number for Binary RDF Table Result files.
	 */
	static final byte[] MAGIC_NUMBER = new byte[] { 'B', 'R', 'D', 'F' };

	/**
	 * The version number of the current format.
	 */
	static final int FORMAT_VERSION = 1;

	/* RECORD TYPES */

	static final int NAMESPACE_DECL = 0;

	static final int STATEMENT = 1;

	static final int COMMENT = 2;

	static final int VALUE_DECL = 3;

	// public static final int ERROR = 126;

	static final int END_OF_DATA = 127;

	/* VALUE TYPES */

	static final int NULL_VALUE = 0;

	static final int URI_VALUE = 1;

	static final int BNODE_VALUE = 2;

	static final int PLAIN_LITERAL_VALUE = 3;

	static final int LANG_LITERAL_VALUE = 4;

	static final int DATATYPE_LITERAL_VALUE = 5;

	static final int VALUE_REF = 6;
}
