package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.ShaclSailConnection;
import org.eclipse.rdf4j.sail.shacl.plan.PlanNode;
import org.eclipse.rdf4j.sail.shacl.plan.Tuple;

public class ExternalTypeFilterNode implements PlanNode {

	ShaclSailConnection shaclSailConnection;
	Resource filterOnType;
	PlanNode parent;

	public ExternalTypeFilterNode(ShaclSailConnection shaclSailConnection, Resource filterOnType, PlanNode parent) {
		this.shaclSailConnection = shaclSailConnection;
		this.filterOnType = filterOnType;
		this.parent = parent;
	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {
		return new CloseableIteration<Tuple, SailException>() {


			Tuple next = null;


			CloseableIteration<Tuple, SailException> parentIterator = parent.iterator();


			void calculateNext() {
				while (next == null && parentIterator.hasNext()) {
					Tuple temp = parentIterator.next();

					Resource subject = (Resource) temp.line.get(0);

					if (shaclSailConnection.hasStatement(subject, RDF.TYPE, filterOnType, true)) {
						next = temp;
					}

				}
			}

			@Override
			public void close() throws SailException {
				parentIterator.close();
			}

			@Override
			public boolean hasNext() throws SailException {
				calculateNext();
				return next != null;
			}

			@Override
			public Tuple next() throws SailException {
				calculateNext();

				Tuple temp = next;
				next = null;

				return temp;
			}

			@Override
			public void remove() throws SailException {

			}
		};
	}
}
