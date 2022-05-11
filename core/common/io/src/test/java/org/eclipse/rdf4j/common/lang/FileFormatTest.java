/*******************************************************************************
 * Copyright (c) $2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.common.lang;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class FileFormatTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testEmptyMimeType() {
        thrown.expect(IllegalArgumentException.class);
        new FileFormat("PLAIN TEXT", new ArrayList<String>(), null, Arrays.asList("txt"));
    }
}
