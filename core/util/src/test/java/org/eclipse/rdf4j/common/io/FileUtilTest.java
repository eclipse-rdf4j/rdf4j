/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.common.io;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.eclipse.rdf4j.common.io.FileUtil;
import org.junit.Test;


public class FileUtilTest {
	@Test
	public void getRelativePathWorksWithRootDirectory() {
		assertEquals(new File(""),
				FileUtil.getRelativeFile(new File("/"), new File("/")));
	}
}
