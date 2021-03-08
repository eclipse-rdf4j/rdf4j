package org.eclipse.rdf4j.sail.shacl.ast;

public class SparqlFragment {

	String fragment;
	boolean filterCondition;
	boolean bgp;

	public SparqlFragment(String fragment) {
		this.fragment = fragment;
	}

	public static SparqlFragment filterCondition(String fragment) {
		SparqlFragment sparqlFragment = new SparqlFragment(fragment);
		sparqlFragment.filterCondition = true;
		return sparqlFragment;
	}

	public static SparqlFragment bgp(String fragment) {
		SparqlFragment sparqlFragment = new SparqlFragment(fragment);
		sparqlFragment.bgp = true;
		return sparqlFragment;
	}

	public String getFragment() {
		return fragment;
	}

	public boolean isFilterCondition() {
		return filterCondition;
	}

	public boolean isBgp() {
		return bgp;
	}
}
