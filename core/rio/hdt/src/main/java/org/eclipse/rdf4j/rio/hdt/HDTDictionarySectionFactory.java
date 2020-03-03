/*******************************************************************************
 * Copyright (c) 2020 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.hdt;

import java.io.IOException;
import java.io.InputStream;

/**
 * HDT DictionarySection factory.
 * 
 * @author Bart Hanssens
 */
class HDTDictionarySectionFactory {
	protected static HDTDictionarySection parse(InputStream is) throws IOException {
		int dtype = is.read();
		if (dtype != HDTDictionarySection.Type.FRONT.getValue()) {
			throw new UnsupportedOperationException("Dictionary section: encoding " + Long.toHexString(dtype) +
					", but only front encoding is supported");
		}
		return new HDTDictionarySectionPFC();
	}
}
