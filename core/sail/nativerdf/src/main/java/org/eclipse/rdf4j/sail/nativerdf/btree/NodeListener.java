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
package org.eclipse.rdf4j.sail.nativerdf.btree;

import java.io.IOException;

interface NodeListener {

	/**
	 * Signals to registered node listeners that a value has been added to a node.
	 *
	 * @param node  The node which the value has been added to.
	 * @param index The index where the value was inserted.
	 * @return Indicates whether the node listener should be deregistered as a result of this event.
	 */
	boolean valueAdded(Node node, int index);

	/**
	 * Signals to registered node listeners that a value has been removed from a node.
	 *
	 * @param node  The node which the value has been removed from.
	 * @param index The index where the value was removed.
	 * @return Indicates whether the node listener should be deregistered as a result of this event.
	 */
	boolean valueRemoved(Node node, int index);

	boolean rotatedLeft(Node node, int index, Node leftChildNode, Node rightChildNode) throws IOException;

	boolean rotatedRight(Node node, int index, Node leftChildNode, Node rightChildNode) throws IOException;

	/**
	 * Signals to registered node listeners that a node has been split.
	 *
	 * @param node      The node which has been split.
	 * @param newNode   The newly allocated node containing the "right" half of the values.
	 * @param medianIdx The index where the node has been split. The value at this index has been moved to the node's
	 *                  parent.
	 * @return Indicates whether the node listener should be deregistered as a result of this event.
	 */
	boolean nodeSplit(Node node, Node newNode, int medianIdx) throws IOException;

	/**
	 * Signals to registered node listeners that two nodes have been merged. All values from the source node have been
	 * appended to the value of the target node.
	 *
	 * @param sourceNode The node that donated its values to the target node.
	 * @param targetNode The node in which the values have been merged.
	 * @param mergeIdx   The index of <var>sourceNode</var>'s values in <var>targetNode</var> .
	 * @return Indicates whether the node listener should be deregistered with the <em>source node</em> as a result of
	 *         this event.
	 */
	boolean nodeMergedWith(Node sourceNode, Node targetNode, int mergeIdx) throws IOException;
}
