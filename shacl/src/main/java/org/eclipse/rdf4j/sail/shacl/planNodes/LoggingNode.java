package org.eclipse.rdf4j.sail.shacl.planNodes;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.shacl.plan.PlanNode;
import org.eclipse.rdf4j.sail.shacl.plan.Tuple;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LoggingNode implements PlanNode{

	PlanNode parent;

	boolean pullAll = true;

	public LoggingNode(PlanNode parent) {
		this.parent = parent;
	}

	@Override
	public CloseableIteration<Tuple, SailException> iterator() {
		return new CloseableIteration<Tuple, SailException>() {


			CloseableIteration<Tuple, SailException> parentIterator = parent.iterator();

			{
				if(pullAll){
					parentIterator = cachedIterator(parentIterator);
				}
			}

			private CloseableIteration<Tuple, SailException> cachedIterator(CloseableIteration<Tuple, SailException> fromIterator) {
				try (Stream<Tuple> stream = Iterations.stream(fromIterator)) {
					List<Tuple> collect = stream.collect(Collectors.toList());

					return new CloseableIteration<Tuple, SailException>() {

						Iterator<Tuple> iterator = collect.iterator();


						@Override
						public void close() throws SailException {

						}

						@Override
						public boolean hasNext() throws SailException {
							return iterator.hasNext();
						}

						@Override
						public Tuple next() throws SailException {
							return iterator.next();
						}

						@Override
						public void remove() throws SailException {

						}
					};

				}
			}


			@Override
			public void close() throws SailException {

					parentIterator.close();

			}

			@Override
			public boolean hasNext() throws SailException {
				boolean hasNext = parentIterator.hasNext();

				//System.out.println(leadingSpace()+parent.getClass().getSimpleName()+".hasNext() : "+hasNext);
				return hasNext;
			}

			@Override
			public Tuple next() throws SailException {
				assert parentIterator.hasNext() : parentIterator.getClass().getSimpleName()+" does not have any more items but next was still called!!!";

				Tuple next = parentIterator.next();

				assert next != null;

				System.out.println(leadingSpace()+parent.getClass().getSimpleName()+".next(): "+" "+next.toString());

				return next;
			}

			@Override
			public void remove() throws SailException {

			}
		};
	}

	@Override
	public int depth() {
		return parent.depth()+1;
	}

	public String leadingSpace(){
		StringBuilder ret = new StringBuilder();
		int depth = depth();
		while(--depth > 0){
			ret.append("    ");
		}
		return ret.toString();
	}
}
