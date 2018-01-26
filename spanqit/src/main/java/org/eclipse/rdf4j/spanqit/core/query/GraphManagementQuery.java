package org.eclipse.rdf4j.spanqit.core.query;

import org.eclipse.rdf4j.spanqit.core.QueryElement;

abstract class GraphManagementQuery<T extends GraphManagementQuery<T>> implements QueryElement {
	private static final String SILENT = "SILENT";
	
	private boolean silent = false; 

	GraphManagementQuery() { }

	/**
	 * Set the <code>SILENT</code> option to true on this query
	 * 
	 * @return this query instance
	 */
	public T silent() {
		return silent(true);
	}

	/**
	 * Specify if the <code>SILENT</code> option should be on for this query
	 * 
	 * @param isSilent if this should be a SILENT operation or not
	 * 
	 * @return this query instance
	 */
	@SuppressWarnings("unchecked")
	public T silent(boolean isSilent) {
		this.silent = isSilent;

		return (T) this;
	}
	
	protected void appendSilent(StringBuilder builder) {
		if (silent) {
			builder.append(SILENT).append(" ");
		}
	}
}
