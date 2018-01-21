package org.eclipse.rdf4j.spanqit.rdf;

import org.eclipse.rdf4j.spanqit.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.spanqit.graphpattern.TriplePattern;
import org.eclipse.rdf4j.spanqit.util.SpanqitUtils;

/**
 * Denotes an RDF Blank Node
 */
public interface RdfBlankNode extends RdfResource {	
	/**
	 * a labeled blank node, of the form "_:<code>label</code>"
	 */
	class LabeledBlankNode implements RdfBlankNode {
		private String label;
		
		LabeledBlankNode(String label) {
			this.label = label;
		}
		
		@Override
		public String getQueryString() {
			return "_:" + label;
		}
	}

	/**
	 * an anonymous blank node
	 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#QSynBlankNodes">
	 * 		Blank node syntax</a>
	 */
	class AnonymousBlankNode implements RdfBlankNode {
		@Override
		public String getQueryString() {
			return SpanqitUtils.getBracketedString("");
		}	
	}
	
	/**
	 * A blank node representing a resource that matches the contained set of predicate-object lists
	 * 
	 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#QSynBlankNodes">
	 * 		Blank node syntax</a>
	 */
	class PropertiesBlankNode implements RdfBlankNode {
		private RdfPredicateObjectListCollection predicateObjectLists = Rdf.predicateObjectListCollection();

		PropertiesBlankNode(RdfPredicate predicate, RdfObject... objects) {
			andHas(predicate, objects);
		}
		
		/**
		 * Using the predicate-object and object list mechanisms, expand this blank node's pattern to include
		 * triples consisting of this blank node as the subject, and the given predicate and object(s)
		 * 
		 * @param predicate the predicate of the triple to add
		 * @param objects the object or objects of the triple to add
		 * 
		 * @return this blank node
		 * 
		 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#predObjLists">
		 * 		Predicate-Object Lists</a>
		 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#objLists">
		 * 		Object Lists</a>
		 */
		public PropertiesBlankNode andHas(RdfPredicate predicate, RdfObject... objects) {
			predicateObjectLists.andHas(predicate, objects);
			
			return this;
		}
		
		/**
		 * Add predicate-object lists to this blank node's pattern
		 * 
		 * @param lists
		 * 		the {@link RdfPredicateObjectList}(s) to add
		 * @return this blank node
		 * 
		 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#predObjLists">
		 * 		Predicate-Object Lists</a>
		 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#objLists">
		 * 		Object Lists</a>
		 */
		public PropertiesBlankNode andHas(RdfPredicateObjectList... lists) {
			predicateObjectLists.andHas(lists);
			
			return this;
		}
		/**
		 * convert this blank node to a triple pattern
		 * 
		 * @return the triple pattern identified by this blank node
		 * 
		 * @see <a href="https://www.w3.org/TR/2013/REC-sparql11-query-20130321/#QSynBlankNodes">
		 * 		blank node syntax</a>
		 */
		public TriplePattern toTp() {
			return GraphPatterns.tp(this);
		}

		@Override
		public String getQueryString() {
			return SpanqitUtils.getBracketedString(predicateObjectLists.getQueryString());
		}
	}
}
