/*******************************************************************************
 * Copyright (c) 2022 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.common.lang;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class FileFormatTest {

    @Test
    public void testEmptyMimeType() {
        assertThrows(IllegalArgumentException.class, () -> {
            new FileFormat("PLAIN TEXT", new ArrayList<String>(), null, Arrays.asList("txt"));
        });
    }
}
