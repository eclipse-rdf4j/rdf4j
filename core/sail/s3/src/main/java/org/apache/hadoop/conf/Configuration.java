/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
// Minimal stub for org.apache.hadoop.conf.Configuration.
// Parquet-hadoop references this class in abstract method signatures
// (WriteSupport.init, ParquetWriter.Builder.getWriteSupport). Our code
// overrides the ParquetConfiguration variants instead, so this class is
// never instantiated or used at runtime. It exists only to satisfy the
// JVM class loader.
package org.apache.hadoop.conf;

public class Configuration {
	public Configuration() {
	}
}
