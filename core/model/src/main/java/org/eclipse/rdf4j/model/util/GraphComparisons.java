/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.model.util;

import static org.eclipse.rdf4j.model.util.Values.bnode;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.DynamicModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

/**
 * Functions for canonicalizing RDF models and computing isomorphism.
 *
 * @implNote The algorithms used in this class are based on the iso-canonical algorithm as described in: Hogan, A.
 *           (2017). Canonical forms for isomorphic and equivalent RDF graphs: algorithms for leaning and labelling
 *           blank nodes. ACM Transactions on the Web (TWEB), 11(4), 1-62.
 *
 * @author Jeen Broekstra
 */
class GraphComparisons {

	private static final Logger logger = LoggerFactory.getLogger(GraphComparisons.class);

	private static final HashFunction hashFunction = Hashing.sha256();

	private static final HashCode initialHashCode = hashFunction.hashString("", StandardCharsets.UTF_8);
	private static final HashCode outgoing = hashFunction.hashString("+", StandardCharsets.UTF_8);
	private static final HashCode incoming = hashFunction.hashString("-", StandardCharsets.UTF_8);
	private static final HashCode distinguisher = hashFunction.hashString("@", StandardCharsets.UTF_8);

	/**
	 * Compares two RDF models, and returns <var>true</var> if they consist of isomorphic graphs and the isomorphic
	 * graph identifiers map 1:1 to each other. RDF graphs are isomorphic graphs if statements from one graphs can be
	 * mapped 1:1 on to statements in the other graphs. In this mapping, blank nodes are not considered mapped when
	 * having an identical internal id, but are mapped from one graph to the other by looking at the statements in which
	 * the blank nodes occur. A Model can consist of more than one graph (denoted by context identifiers). Two models
	 * are considered isomorphic if for each of the graphs in one model, an isomorphic graph exists in the other model,
	 * and the context identifiers of these graphs are identical.
	 *
	 * @implNote The algorithm used by this comparison is a depth-first search for an iso-canonical blank node mapping
	 *           for each model, and using that as a basis for comparison. The algorithm is described in detail in:
	 *           Hogan, A. (2017). Canonical forms for isomorphic and equivalent RDF graphs: algorithms for leaning and
	 *           labelling blank nodes. ACM Transactions on the Web (TWEB), 11(4), 1-62.
	 *
	 * @see <a href="http://www.w3.org/TR/rdf11-concepts/#graph-isomorphism">RDF Concepts &amp; Abstract Syntax, section
	 *      3.6 (Graph Comparison)</a>
	 * @see <a href="http://aidanhogan.com/docs/rdf-canonicalisation.pdf">Hogan, A. (2017). Canonical forms for
	 *      isomorphic and equivalent RDF graphs: algorithms for leaning and labelling blank nodes. ACM Transactions on
	 *      the Web (TWEB), 11(4), 1-62. Technical Paper (PDF )</a>
	 *
	 */
	public static boolean isomorphic(Model model1, Model model2) {
		if (model1 == model2) {
			return true;
		}

		if (model1.size() != model2.size()) {
			return false;
		}

		if (model1.contexts().size() != model2.contexts().size()) {
			return false;
		}

		if (model1.contexts().size() > 1) {
			// model contains more than one context (including the null context). We compare per individual context.
			for (Resource context : model1.contexts()) {
				Model contextInModel1 = model1.filter(null, null, null, context);
				if (context != null && context.isBNode()) {
					// context identifier is a blank node. We to find blank node identifiers in the other model that map
					// iso-canonically.
					Map<BNode, HashCode> mapping1 = getIsoCanonicalMapping(model1);
					Multimap<HashCode, BNode> partitionMapping2 = partitionMapping(getIsoCanonicalMapping(model2));

					Collection<BNode> contextCandidates = partitionMapping2.get(mapping1.get(context));
					if (contextCandidates.isEmpty()) {
						return false;
					}

					boolean foundIsomorphicBlankNodeContext = false;
					for (BNode context2 : contextCandidates) {
						Model contextInModel2 = model2.filter(null, null, null, context2);
						if (contextInModel1.size() != contextInModel2.size()) {
							continue;
						}
						if (isomorphicSingleContext(contextInModel1, contextInModel2)) {
							foundIsomorphicBlankNodeContext = true;
							break;
						}
					}
					if (!foundIsomorphicBlankNodeContext) {
						return false;
					}
				} else {
					// context identifier is an iri. Simple per-context check will suffice.
					Model contextInModel2 = model2.filter(null, null, null, context);
					if (contextInModel1.size() != contextInModel2.size()) {
						return false;
					}
					final Model canonicalizedContext1 = isoCanonicalize(contextInModel1);
					final Model canonicalizedContext2 = isoCanonicalize(contextInModel2);
					if (!canonicalizedContext1.equals(canonicalizedContext2)) {
						return false;
					}
				}
			}
			return true;
		} else {
			// only one context (the null context), so we're dealing with one graph only.
			return isomorphicSingleContext(model1, model2);
		}
	}

	private static boolean isomorphicSingleContext(Model model1, Model model2) {
		final Map<BNode, HashCode> mapping1 = getIsoCanonicalMapping(model1);
		if (mapping1.isEmpty()) {
			// no blank nodes in model1 - simple collection equality will do
			return model1.equals(model2);
		}
		final Map<BNode, HashCode> mapping2 = getIsoCanonicalMapping(model2);

		if (mappingsIncompatible(mapping1, mapping2)) {
			return false;
		}

		// Compatible blank node mapping found. We need to check that statements not involving blank nodes are equal in
		// both models.
		Optional<Statement> missingInModel2 = model1.stream()
				.filter(st -> !(st.getSubject().isBNode() || st.getObject().isBNode()
						|| st.getContext() instanceof BNode))
				.filter(st -> !model2.contains(st))
				.findAny();

		// Because we have previously already checked that the models are the same size, we don't have to check both
		// ways to establish model equality.
		return !missingInModel2.isPresent();
	}

	private static boolean mappingsIncompatible(Map<BNode, HashCode> mapping1, Map<BNode, HashCode> mapping2) {
		if (mapping1.size() != mapping2.size()) {
			return true;
		}
		Set<HashCode> values1 = new HashSet<>(mapping1.values());
		Set<HashCode> values2 = new HashSet<>(mapping2.values());

		if (!(values1.equals(values2))) {
			return true;
		}

		return false;
	}

	protected static Model isoCanonicalize(Model m) {
		return labelModel(m, getIsoCanonicalMapping(m));
	}

	protected static Map<BNode, HashCode> getIsoCanonicalMapping(Model m) {
		Partitioning partitioning = hashBNodes(m);

		if (partitioning.isFine()) {
			return partitioning.getCurrentNodeMapping();
		}

		return distinguish(m, partitioning, null, new ArrayList<>(), new ArrayList<>());
	}

	protected static Set<BNode> getBlankNodes(Model m) {
		final Set<BNode> blankNodes = new HashSet<>();

		m.forEach(st -> {
			if (st.getSubject().isBNode()) {
				blankNodes.add((BNode) st.getSubject());
			}
			if (st.getObject().isBNode()) {
				blankNodes.add((BNode) st.getObject());
			}
			if (st.getContext() != null && st.getContext().isBNode()) {
				blankNodes.add((BNode) st.getContext());
			}
		});
		return blankNodes;
	}

	private static Map<BNode, HashCode> distinguish(Model m, Partitioning partitioning,
			Map<BNode, HashCode> lowestFound, List<BNode> parentFixpoints,
			List<Map<BNode, HashCode>> finePartitionMappings) {

		for (BNode node : partitioning.getLowestNonTrivialPartition()) {
			List<BNode> fixpoints = new ArrayList<>(parentFixpoints);
			fixpoints.add(node);

			Partitioning clonedPartitioning = new Partitioning(partitioning.getCurrentNodeMapping(),
					partitioning.getStaticValueMapping());

			clonedPartitioning.setCurrentHashCode(node,
					hashTuple(clonedPartitioning.getCurrentHashCode(node), distinguisher));
			clonedPartitioning = hashBNodes(m, clonedPartitioning);

			if (clonedPartitioning.isFine()) {
				finePartitionMappings.add(clonedPartitioning.getCurrentNodeMapping());
				if (lowestFound == null
						|| clonedPartitioning.getMappingSize().compareTo(partitioning.getMappingSize()) < 0) {
					lowestFound = clonedPartitioning.getCurrentNodeMapping();
				}
			} else {
				Map<BNode, BNode> compatibleAutomorphism = findCompatibleAutomorphism(fixpoints, finePartitionMappings);
				if (compatibleAutomorphism != null) {
					// prune
					continue;
				}
				lowestFound = distinguish(m, clonedPartitioning, lowestFound, fixpoints, finePartitionMappings);
			}
		}

		return lowestFound;
	}

	protected static Map<BNode, BNode> findCompatibleAutomorphism(List<BNode> fixpoints,
			List<Map<BNode, HashCode>> partitionMappings) {
		// check if two mappings with identical hash codes exist
		for (Map<BNode, HashCode> mapping : partitionMappings) {
			Map<BNode, HashCode> compatibleMapping = null;
			for (Map<BNode, HashCode> om : partitionMappings) {
				if (om.equals(mapping)) {
					continue;
				}

				if (om.values().containsAll(mapping.values())) {
					compatibleMapping = om;
					break;
				}
			}

			if (compatibleMapping != null) {
				Map<HashCode, BNode> invertedMapping = mapping.entrySet()
						.stream()
						.collect(Collectors.toMap(Entry::getValue, Entry::getKey));

				Map<BNode, BNode> automorphism = new HashMap<>();
				for (Entry<BNode, HashCode> entry : compatibleMapping.entrySet()) {
					automorphism.put(entry.getKey(), invertedMapping.get(entry.getValue()));
				}
				// check if fixpoints all map, if so we have a compatible automorphism
				for (BNode fixpoint : fixpoints) {
					if (!automorphism.get(fixpoint).equals(fixpoint)) {
						break;
					}
					return automorphism;
				}
			}
		}
		return null;
	}

	protected static List<Collection<BNode>> partitions(Multimap<HashCode, BNode> partitionMapping) {
		List<Collection<BNode>> partition = new ArrayList<>();

		for (Entry<HashCode, Collection<BNode>> entry : partitionMapping.asMap().entrySet()) {
			partition.add(entry.getValue());
		}
		return partition;
	}

	protected static Multimap<HashCode, BNode> partitionMapping(Map<BNode, HashCode> blankNodeMapping) {
		return Multimaps.invertFrom(Multimaps.forMap(blankNodeMapping),
				MultimapBuilder.hashKeys(blankNodeMapping.keySet().size()).arrayListValues().build());
	}

	private static Model labelModel(Model original, Map<BNode, HashCode> hash) {
		Model result = new DynamicModelFactory().createEmptyModel();

		for (Statement st : original) {
			if (st.getSubject().isBNode() || st.getObject().isBNode()
					|| (st.getContext() != null && st.getContext().isBNode())) {
				Resource subject = st.getSubject().isBNode()
						? createCanonicalBNode((BNode) st.getSubject(), hash)
						: st.getSubject();
				IRI predicate = st.getPredicate();
				Value object = st.getObject().isBNode()
						? createCanonicalBNode((BNode) st.getObject(), hash)
						: st.getObject();
				Resource context = (st.getContext() != null && st.getContext().isBNode())
						? createCanonicalBNode((BNode) st.getContext(), hash)
						: st.getContext();

				result.add(subject, predicate, object, context);
			} else {
				result.add(st);
			}
		}
		return result;
	}

	protected static Partitioning hashBNodes(Model m) {
		return hashBNodes(m, null);
	}

	private static Partitioning hashBNodes(Model m, Partitioning partitioning) {
		if (partitioning == null) {
			final Set<BNode> blankNodes = getBlankNodes(m);
			partitioning = new Partitioning(blankNodes);
		}

		if (!partitioning.getNodes().isEmpty()) {
			do {
				partitioning.nextIteration();
				for (BNode b : partitioning.getNodes()) {
					for (Statement st : m.getStatements(b, null, null)) {
						HashCode c = hashTuple(
								partitioning.getPreviousHashCode(st.getObject()),
								partitioning.getPreviousHashCode(st.getPredicate()),
								outgoing);
						partitioning.setCurrentHashCode(b,
								hashBag(c, partitioning.getCurrentHashCode(b)));
					}
					for (Statement st : m.getStatements(null, null, b)) {
						HashCode c = hashTuple(
								partitioning.getPreviousHashCode(st.getSubject()),
								partitioning.getPreviousHashCode(st.getPredicate()),
								incoming);
						partitioning.setCurrentHashCode(b,
								hashBag(c, partitioning.getCurrentHashCode(b)));
					}
				}
			} while (!partitioning.isFullyDistinguished());
		}
		return partitioning;
	}

	protected static HashCode hashTuple(HashCode... hashCodes) {
		return Hashing.combineOrdered(Arrays.asList(hashCodes));
	}

	protected static HashCode hashBag(HashCode... hashCodes) {
		return Hashing.combineUnordered(Arrays.asList(hashCodes));
	}

	private static BNode createCanonicalBNode(BNode node, Map<BNode, HashCode> mapping) {
		return bnode("iso-" + mapping.get(node).toString());
	}

	/**
	 * Encapsulates the current partitioning state of the algorithm, keeping track of previous and current node:hashcode
	 * mappings as well as static value mappings.
	 *
	 */
	static class Partitioning {

		private final Map<Value, HashCode> staticValueMapping;

		private Map<BNode, HashCode> previousNodeMapping;

		private Map<BNode, HashCode> currentNodeMapping;

		private Multimap<HashCode, BNode> currentHashCodeMapping;

		private final int nodeCount;

		public Partitioning(Set<BNode> blankNodes) {
			this.staticValueMapping = new HashMap<>();
			this.nodeCount = blankNodes.size();
			this.currentNodeMapping = new HashMap<>(nodeCount);
			blankNodes.forEach(node -> currentNodeMapping.put(node, initialHashCode));
		}

		public Partitioning(Map<BNode, HashCode> nodeMapping, Map<Value, HashCode> staticValueMapping) {
			this.staticValueMapping = staticValueMapping;
			this.nodeCount = nodeMapping.keySet().size();
			this.currentNodeMapping = new HashMap<>(nodeMapping);
		}

		public Map<Value, HashCode> getStaticValueMapping() {
			return staticValueMapping;
		}

		public HashCode getCurrentHashCode(Value value) {
			if (value.isBNode()) {
				return currentNodeMapping.get((BNode) value);
			}
			if (value.isLiteral()) {
				return getStaticLiteralHashCode((Literal) value);
			}
			return staticValueMapping.computeIfAbsent(value,
					v -> hashFunction.hashString(v.stringValue(), StandardCharsets.UTF_8));
		}

		public Set<BNode> getNodes() {
			return currentNodeMapping.keySet();
		}

		public HashCode getPreviousHashCode(Value value) {
			if (value.isBNode()) {
				return previousNodeMapping.get((BNode) value);
			}
			if (value.isLiteral()) {
				return getStaticLiteralHashCode((Literal) value);
			}
			return staticValueMapping.computeIfAbsent(value,
					v -> hashFunction.hashString(v.stringValue(), StandardCharsets.UTF_8));

		}

		public void setCurrentHashCode(BNode bnode, HashCode hashCode) {
			currentNodeMapping.put(bnode, hashCode);
		}

		public Map<BNode, HashCode> getCurrentNodeMapping() {
			return Collections.unmodifiableMap(currentNodeMapping);
		}

		public void nextIteration() {
			previousNodeMapping = currentNodeMapping;
			currentNodeMapping = new HashMap<>(currentNodeMapping);
			currentHashCodeMapping = null;
		}

		/**
		 * A partitioning is fine if every hashcode maps to exactly one blank node.
		 *
		 * @return true if the partitioning is fine, false otherwise.
		 */
		public boolean isFine() {
			return getCurrentHashCodeMapping().asMap().values().stream().allMatch(member -> member.size() == 1);
		}

		public boolean isFullyDistinguished() {
			if (isFine()) { // no two terms share a hash
				return true;
			}

			return currentUnchanged();
		}

		public Collection<BNode> getLowestNonTrivialPartition() {
			final List<Collection<BNode>> sortedPartitions = new ArrayList<>(
					getCurrentHashCodeMapping().asMap().values());
			Collections.sort(sortedPartitions, new Comparator<Collection<BNode>>() {
				public int compare(Collection<BNode> a, Collection<BNode> b) {
					int result = a.size() - b.size();
					if (result == 0) {
						// break tie by comparing value hash
						HashCode hashOfA = currentNodeMapping.get(a.iterator().next());
						HashCode hashOfB = currentNodeMapping.get(b.iterator().next());

						BigInteger difference = new BigInteger(1, hashOfA.asBytes())
								.subtract(new BigInteger(1, hashOfB.asBytes()));
						result = difference.compareTo(BigInteger.ZERO);
					}
					return result;
				}
			});

			Collection<BNode> lowestNonTrivialPartition = sortedPartitions.stream()
					.filter(part -> part.size() > 1)
					.findFirst()
					.orElseThrow(RuntimeException::new);

			return lowestNonTrivialPartition;
		}

		/**
		 * Return a mapping size to determine a canonical lowest mapping.
		 */
		public BigInteger getMappingSize() {
			BigInteger size = currentNodeMapping.values()
					.stream()
					.map(h -> new BigInteger(1, h.asBytes()))
					.reduce(BigInteger.ZERO, (v1, v2) -> v1.add(v2));
			return size;
		}

		private HashCode getStaticLiteralHashCode(Literal value) {
			// GH-2834: we need to include language and datatype when computing a unique hash code for literals
			return staticValueMapping.computeIfAbsent(value,
					v -> {
						Literal l = (Literal) v;
						List<HashCode> hashSequence = new ArrayList<>(3);

						hashSequence.add(hashFunction.hashString(l.getLabel(), StandardCharsets.UTF_8));

						// Per BCP47, language tags are case-insensitive. Use normalized form to ensure consistency if
						// possible, otherwise just use lower-case.
						l.getLanguage()
								.map(lang -> hashFunction.hashString(
										Literals.isValidLanguageTag(lang) ? Literals.normalizeLanguageTag(lang)
												: lang.toLowerCase(),
										StandardCharsets.UTF_8))
								.ifPresent(h -> hashSequence.add(h));
						hashSequence
								.add(hashFunction.hashString(l.getDatatype().stringValue(), StandardCharsets.UTF_8));
						return Hashing.combineOrdered(hashSequence);
					}
			);
		}

		private Multimap<HashCode, BNode> getCurrentHashCodeMapping() {
			if (currentHashCodeMapping == null) {
				currentHashCodeMapping = Multimaps.invertFrom(Multimaps.forMap(currentNodeMapping),
						HashMultimap.create());
			}
			return currentHashCodeMapping;
		}

		/**
		 * Verify if the current node mapping is unchanged compared to the previous node mapping.
		 * <p>
		 * it is unchanged if: all bnodes that have the same hashcode in current also shared the same hashcode in
		 * previous, and all bnodes that have different ones in current also have different ones in previous.
		 *
		 * @return true if unchanged, false otherwise
		 */
		private boolean currentUnchanged() {

			final Multimap<HashCode, BNode> previous = Multimaps.invertFrom(Multimaps.forMap(previousNodeMapping),
					HashMultimap.create());
			for (Collection<BNode> currentSharedHashNodes : getCurrentHashCodeMapping().asMap().values()) {
				// pick a BNode, doesn't matter which: they all share the same hashcode
				BNode node = currentSharedHashNodes.iterator().next();
				HashCode previousHashCode = previousNodeMapping.get(node);
				if (!previous.get(previousHashCode).equals(currentSharedHashNodes)) {
					return false;
				}
			}
			return true;
		}
	}
}
