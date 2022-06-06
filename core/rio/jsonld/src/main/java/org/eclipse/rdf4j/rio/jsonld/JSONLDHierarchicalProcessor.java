/*******************************************************************************
 * Copyright (c) 2018 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.rio.jsonld;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Converts a JSON-LD object to a hierarchical form
 *
 * @author Yasen Marinov
 */
public class JSONLDHierarchicalProcessor {

	public static final String ID = "@id";
	public static final String GRAPH = "@graph";

	/**
	 * Converts a JSON-LD object to a hierarchical JSON-LD object
	 *
	 * @param jsonLdObject JSON-LD object to be converted. Gets modified during processing
	 * @return hierarchical JSON-LD object
	 */
	public static Object fromJsonLdObject(Object jsonLdObject) {
		return expandInDepth(jsonLdObject);
	}

	/**
	 * Expands the JSON-LD object to a hierarchical shape.
	 * <p>
	 * As the first level of nodes in the object can be either a triple or a whole graph we first expand the graph nodes
	 * and after that we expand the default graph.
	 * <p>
	 * The different graphs are processed independently to keep them in insulation.
	 *
	 * @param input the JSON-LD object. Gets modified during processing.
	 * @return
	 */
	private static Object expandInDepth(Object input) {
		for (Map<String, Object> graph : (ArrayList<Map<String, Object>>) input) {
			if (graph.containsKey(GRAPH)) {
				graph.compute(GRAPH, (key, o) -> expandContextInDepth(o));
			}
		}
		return expandContextInDepth(input);
	}

	/**
	 * Transforms a JSON-LD object to a more human-readable hierarchical form.
	 * <p>
	 * The steps performed are
	 * <ol>
	 * <li>Take all triples which will take part of the processing and add them to a separate map</li>
	 * <li>Create a separate list the triples sorted by number of predicates in the descending order</li>
	 * <li>Select a root to start</li>
	 * <li>Take this root from the graph and start a DFS traversing. For each traversed node
	 * <ol>
	 * <li>Mark this node as visited</li>
	 * <li>Find all sub-nodes (effectively objects in triples in which the current node is subject)</li>
	 * <li>Expand the sub-nodes (replace them with their full version) and add them to the traversing if the following
	 * conditions are met
	 * <ul>
	 * <li>sub-node is IRI or BlankNode</li>
	 * <li>sub-node has not been expanded already in the current path</li>
	 * <li>sub-node is not the same as it's parent</li>
	 * </ul>
	 * </li>
	 * </ol>
	 * </li>
	 * <li>If the visited list shows there are still unvisited nodes choose a new root from the list of sorted nodes and
	 * start another traversal</li>
	 * </ol>
	 *
	 * @param input JSON-LD object. Gets modified during processing
	 * @return the hierarchical JSON-LD object
	 */
	private static Object expandContextInDepth(Object input) {

		final Map<String, Object> graph = new LinkedHashMap<>();
		final List<Object> expanded = new ArrayList<>();
		for (Map<String, Object> jsonNode : (ArrayList<Map<String, Object>>) input) {
			if (jsonNode.containsKey(GRAPH)) {
				// Add graph nodes to the return result without further processing
				expanded.add(jsonNode);
			} else {
				graph.put(jsonNode.get(ID).toString(), jsonNode);
			}
		}

		LinkedList<TreeNode> frontier = new LinkedList<>();

		Set<String> visited = new HashSet<>();
		List<String> sortedNodes = getNodesOrder(graph);
		Set<String> children = new HashSet<>();

		while (visited.size() < graph.size()) {
			Object rootNode = graph.get(getNextRoot(visited, sortedNodes));
			frontier.add(new TreeNode((Map<String, Object>) rootNode));
			expanded.add(rootNode);

			while (!frontier.isEmpty()) {
				TreeNode currentTreeNode = frontier.removeLast();
				visited.add(currentTreeNode.getNodeID());
				Map<String, Object> currentNode = currentTreeNode.node;
				for (String predicate : currentNode.keySet()) {
					Object object = currentNode.get(predicate);
					if (object instanceof List<?>) {
						ArrayList<Map<String, Object>> objectsPredSubjPairs = (ArrayList<Map<String, Object>>) object;
						for (int i = 0; i < objectsPredSubjPairs.size(); i++) {
							if (objectsPredSubjPairs.get(i) instanceof Map
									&& objectsPredSubjPairs.get(i).get(ID) != null) {
								String objectsPredId = objectsPredSubjPairs.get(i).get(ID).toString();
								if (graph.containsKey(objectsPredId) && !currentNode.get(ID).equals(objectsPredId)
										&& !currentTreeNode.hasPassedThrough(objectsPredId)) {
									children.add(objectsPredId);
									objectsPredSubjPairs.set(i, (Map<String, Object>) graph.get(objectsPredId));
									frontier.add(new TreeNode(objectsPredSubjPairs.get(i), currentTreeNode));
								}
							}
						}
					}
				}
			}
		}

		expanded.removeIf(o -> {
			if (o instanceof Map<?, ?>) {
				return children.contains(((Map<String, Object>) o).get(ID).toString());
			}
			return false;
		});
		return expanded;
	}

	/**
	 * Returns the next node to be a root. Chooses the first non-visited node from the sortedNodes list.
	 *
	 * @param visited     contains the visited nodes so-far
	 * @param sortedNodes contains the nodes in a specific order. This list will get modified by the method!
	 * @return root for the next tree
	 */
	private static String getNextRoot(Set<String> visited, List<String> sortedNodes) {
		String rootOffer;
		while (!sortedNodes.isEmpty()) {
			rootOffer = sortedNodes.remove(0);
			if (!visited.contains(rootOffer)) {
				return rootOffer;
			}
		}
		return null;
	}

	/**
	 * Returns the nodes in a JSON-LD object ordered by the number of their subnodes (predicates in which the nodes are
	 * subjects)
	 *
	 * @param graph JSON-LD object
	 * @return List with the nodes' ids
	 */
	private static List<String> getNodesOrder(Map<String, Object> graph) {
		return graph.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByValue((o1, o2) -> ((Map) o2).size() - ((Map) o1).size()))
				.map(entry -> entry.getKey())
				.collect(Collectors.toList());
	}

	private static class TreeNode {
		private final TreeNode parent;
		private final Map<String, Object> node;

		public TreeNode(Map<String, Object> node) {
			this.node = node;
			this.parent = null;
		}

		public TreeNode(Map<String, Object> node, TreeNode parent) {
			this.parent = parent;
			this.node = node;
		}

		public String getNodeID() {
			return node.get(ID).toString();
		}

		public boolean hasPassedThrough(String nodeId) {
			TreeNode curr = this.parent;
			while (curr != null) {
				if (curr.getNodeID().equals(nodeId)) {
					return true;
				}
				curr = curr.parent;
			}
			return false;
		}

	}
}
