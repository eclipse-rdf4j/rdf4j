package org.eclipse.rdf4j.repository.sail;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.sail.SailException;

class SailResult<E> extends AbstractCloseableIteration<E, SailException> {

	private final List<E> list = new ArrayList<>();

	private Iterator<E> it;

	public SailResult() {
		it = list.iterator();
	}

	public void add(final E element) {
		list.add(element);
		it = list.iterator();
	}

	@Override
	public boolean hasNext()
		throws SailException
	{
		return !(this.isClosed()) && it.hasNext();
	}

	@Override
	public E next()
		throws SailException
	{
		return it.next();
	}

	@Override
	public void remove()
		throws SailException
	{
		it.remove();
	}

}
