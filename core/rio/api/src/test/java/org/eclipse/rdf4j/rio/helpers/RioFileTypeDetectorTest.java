/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.rio.helpers;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.spi.FileTypeDetector;
import java.util.ServiceLoader;

import org.junit.Test;

public class RioFileTypeDetectorTest {
	@Test
	public void correctClassIsRegisteredInServices() {
		assertThat(ServiceLoader.load(FileTypeDetector.class)).anyMatch(ftd -> ftd instanceof RioFileTypeDetector);
	}
}
