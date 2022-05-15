package org.eclipse.rdf4j.model.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.rdf4j.model.util.GraphComparisons.hashBag;
import static org.eclipse.rdf4j.model.util.Values.bnode;
import static org.eclipse.rdf4j.model.util.Values.iri;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

public class GraphComparisonsTest {

	private static final String ex = "http://example.org/";

	IRI p = iri(ex, "p"), q = iri(ex, "q");
	BNode a = bnode("a"), b = bnode("b"), c = bnode("c"), d = bnode("d"), e = bnode("e"), f = bnode("f"),
			g = bnode("g"), h = bnode("h"), i = bnode("i");

	@Test
	public void testCanonicalHashing() {
		Model example49 = buildExample49Model();

		Map<BNode, HashCode> mapping = GraphComparisons.hashBNodes(example49).getCurrentNodeMapping();

		assertThat(mapping.get(a))
				.isEqualTo(mapping.get(c))
				.isEqualTo(mapping.get(g))
				.isEqualTo(mapping.get(i));

		assertThat(mapping.get(b))
				.isEqualTo(mapping.get(d))
				.isEqualTo(mapping.get(f))
				.isEqualTo(mapping.get(h));

		assertThat(mapping.get(a)).isNotEqualTo(mapping.get(b));
		assertThat(mapping.get(e)).isNotEqualTo(mapping.get(a))
				.isNotEqualTo(mapping.get(b));
	}

	@Test
	public void testIsoCanonicalize() {
		Model example49 = buildExample49Model();

		Model canonicalized = GraphComparisons.isoCanonicalize(example49);

		assertThat(canonicalized.size()).isEqualTo(example49.size());
	}

	@Test
	public void testIsoCanonicalize_list() {
		List<String> list = Arrays.asList("b", "a", "c", "d", "e");

		Model rdfCollection1 = RDFCollections.asRDF(list, a, new TreeModel());

		rdfCollection1.add(p, q, a);

		Model rdfCollection2 = RDFCollections.asRDF(list, b, new LinkedHashModel());
		rdfCollection2.add(p, q, b);

		Model canonicalized1 = GraphComparisons.isoCanonicalize(rdfCollection1);
		Model canonicalized2 = GraphComparisons.isoCanonicalize(rdfCollection2);

		assertThat(canonicalized1).isEqualTo(canonicalized2);
	}

	@Test
	public void testIsomorphic() {
		Model example49 = buildExample49Model();
		Model isomorphic = buildExample49ModelIsomorphic();

		assertThat(GraphComparisons.isomorphic(example49, isomorphic));
	}

	@Test
	public void testHashTuple() {
		HashFunction hashFunction = Hashing.murmur3_128();

		HashCode hash1 = hashFunction.hashString("abcd", Charsets.UTF_8);
		HashCode hash2 = hashFunction.hashString("efgh", Charsets.UTF_8);

		HashCode sequence12 = GraphComparisons.hashTuple(hash1, hash2);
		HashCode sequence21 = GraphComparisons.hashTuple(hash2, hash1);

		// hashTuple is order-dependent
		assertThat(sequence12).isNotEqualTo(sequence21);
	}

	@Test
	public void testHashBag() {
		HashFunction hashFunction = Hashing.murmur3_128();

		HashCode hash1 = hashFunction.hashString("abcd", Charsets.UTF_8);
		HashCode hash2 = hashFunction.hashString("efgh", Charsets.UTF_8);
		HashCode hash3 = hashFunction.hashString("ijkl", Charsets.UTF_8);

		HashCode sequence12 = hashBag(hash1, hash2);
		HashCode sequence21 = hashBag(hash2, hash1);

		// hashBag is commutative
		assertThat(sequence12).isEqualTo(sequence21);

		// hashBag is associative
		assertThat(hashBag(hash1, hashBag(hash2, hash3)))
				.isEqualTo(hashBag(hashBag(hash1, hash2), hash3))
				.isEqualTo(hashBag(hash1, hash2, hash3));
	}

	/**
	 * Graph from example 4.9 in http://aidanhogan.com/docs/rdf-canonicalisation.pdf
	 *
	 */
	private Model buildExample49Model() {
		// @formatter:off
		Model example49 = new ModelBuilder(new LinkedHashModel())
				.subject(a).add(p, b).add(p, d)
				.subject(b).add(q, e)
				.subject(c).add(p, b).add(p, f)
				.subject(d).add(q, e)
				.subject(f).add(q, e)
				.subject(g).add(p, d).add(p, h)
				.subject(h).add(q, e)
				.subject(i).add(p, f).add(p, h)
				.build();
		// @formatter:on

		return example49;
	}

	/**
	 * Graph from example 4.9 in http://aidanhogan.com/docs/rdf-canonicalisation.pdf - differently ordered
	 */
	private Model buildExample49ModelIsomorphic() {
		// @formatter:off
		Model example49 = new ModelBuilder(new LinkedHashModel())
				.subject(bnode("other-i")).add(p, bnode("other-f")).add(p, bnode("other-h"))
				.subject(bnode("other-a")).add(p, bnode("other-b")).add(p, bnode("other-d"))
				.subject(bnode("other-b")).add(q, bnode("other-e"))
				.subject(bnode("other-c")).add(p, bnode("other-b")).add(p, bnode("other-f"))
				.subject(bnode("other-f")).add(q, bnode("other-e"))
				.subject(bnode("other-d")).add(q, bnode("other-e"))
				.subject(bnode("other-g")).add(p, bnode("other-d")).add(p, bnode("other-h"))
				.subject(bnode("other-h")).add(q, bnode("other-e"))
				.build();
		// @formatter:on
		return example49;
	}
}
