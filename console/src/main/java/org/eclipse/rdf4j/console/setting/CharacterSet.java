/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.console.setting;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Working directory setting
 * 
 * @author Bart Hanssens
 */
public class CharacterSet extends ConsoleSetting<Charset> {
	public final static String NAME = "charset";
	
	@Override
	public String getHelpLong() {
		return "set charSet=<set>              Set the default character set\n";
	}
	
	/**
	 * Constructor
	 * 
	 * Default dir is "."
	 */
	public CharacterSet() {
		super(StandardCharsets.UTF_8);
	}
	
	/**
	 * Constructor
	 * 
	 * @param initValue
	 */
	public CharacterSet(Charset initValue) {
		super(initValue);
	}
	
	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public void set(Charset value) throws IllegalArgumentException {
		if (value != null) {
			super.set(value);
		} else {
			throw new IllegalArgumentException("Charset is null");
		}
	}

	@Override
	public void setFromString(String value) throws IllegalArgumentException {
		try {
			set(Charset.forName(value));
		} catch(UnsupportedCharsetException | IllegalCharsetNameException e) {
			throw new IllegalArgumentException("Unknown charset");
		}
	}
}
