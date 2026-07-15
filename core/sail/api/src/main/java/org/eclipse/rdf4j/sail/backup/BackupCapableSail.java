/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.backup;

import org.eclipse.rdf4j.sail.Sail;

/**
 * Marker interface for sails that expose a backup service.
 */
public interface BackupCapableSail extends Sail {

	SailBackupService getBackupService();
}
