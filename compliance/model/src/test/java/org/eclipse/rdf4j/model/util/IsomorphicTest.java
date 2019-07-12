package org.eclipse.rdf4j.model.util;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class IsomorphicTest {

	static private Model empty;
	static private Model blankNodes;
	static private Model shacl;
	static private Model longChain;
	static private Model sparqlTestCase;
	static private Model spinFullForwardchained;
	static private Model bsbm;
	static private Model bsbmChanged;
	static private List<Statement> bsbm_arraylist;
	static private Model bsbmTree;
	static private Model list;
	static private Model internallyIsomorphic;
	static private Model manyProperties;
	static private Model manyProperties2;

	@BeforeClass
	public static void beforeClass() {
		empty = getModel("empty.ttl");
		blankNodes = getModel("blankNodes.ttl");
		shacl = getModel("shacl.ttl");
		longChain = getModel("longChain.ttl");
		sparqlTestCase = getModel("sparqlTestCase.ttl");
		spinFullForwardchained = getModel("spin-full-forwardchained.ttl");
		bsbm = getModel("bsbm-100.ttl");
		bsbmChanged = getModel("bsbm-100-changed.ttl");
		bsbm_arraylist = new ArrayList<>(bsbm);
		bsbmTree = new TreeModel(bsbm);
		list = getModel("list.ttl");
		internallyIsomorphic = getModel("internallyIsomorphic.ttl");
		manyProperties = getModel("manyProperties.ttl");
		manyProperties2 = getModel("manyProperties2.ttl");

	}

	@Test
	public void empty() {

		isomorphic(empty);

	}

	@Test
	public void blankNodes() {

		isomorphic(blankNodes);

	}

	@Test
	public void shacl() {

		isomorphic(shacl);

	}

	@Test
	public void longChain() {

		isomorphic(longChain);

	}

	@Test
	public void sparqlTestCase() {

		isomorphic(sparqlTestCase);

	}

	@Test
	public void bsbm() {

		isomorphic(bsbm);

	}

	@Test
	public void bsbmTree() {

		isomorphic(bsbmTree);

	}

	@Test
	public void bsbmArrayList() {

		boolean isomorphic = Models.isomorphic(bsbm_arraylist, bsbm_arraylist);
		if (!isomorphic) {
			throw new IllegalStateException("Not isomorphic");
		}

	}

	@Test
	public void spinFullForwardchained() {

		isomorphic(spinFullForwardchained);

	}

	@Test
	public void list() {

		isomorphic(list);

	}

	@Test
	public void internallyIsomorphic() {

		isomorphic(internallyIsomorphic);

	}

	@Test
	public void manyProperties() {

		isomorphic(manyProperties);

	}

	@Test
	public void manyProperties2() {

		isomorphic(manyProperties2);

	}

	@Test
	public void emptyNotIsomorphic() {

		notIsomorphic(empty, bsbm);

	}

	@Test
	public void bsbmNotIsomorphic() {

		notIsomorphic(bsbm, bsbmChanged);

	}

	private static Model getModel(String name) {
		try {
			try (InputStream resourceAsStream = IsomorphicTest.class.getClassLoader()
					.getResourceAsStream("benchmark/" + name)) {
				return Rio.parse(resourceAsStream, "http://example.com/", RDFFormat.TURTLE);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private boolean isomorphic(Model m) {

		boolean isomorphic = Models.isomorphic(m, m);
		if (!isomorphic) {
			throw new IllegalStateException("Not isomorphic");
		}

		return isomorphic;
	}

	private boolean notIsomorphic(Model m1, Model m2) {

		boolean isomorphic = Models.isomorphic(m1, m2);
		if (isomorphic) {
			throw new IllegalStateException("Should not be isomorphic");
		}

		return isomorphic;
	}

}
