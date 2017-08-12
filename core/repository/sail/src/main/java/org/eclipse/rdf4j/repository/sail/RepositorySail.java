package org.eclipse.rdf4j.repository.sail;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.Sail;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.AbstractNotifyingSail;

/**
 * <pre>
 * Repository repository = new SailRepository(new ForwardChainingRDFSInferencer(new MemoryStore()));
 * repository.initialize();
 * 
 * Sail sail = new RepositorySail(repository);
 * sail.initialize();
 * </pre>
 * 
 * @author Alfredo Serafini
 */
public class RepositorySail extends AbstractNotifyingSail implements Sail {

	private final Repository repository;

	public RepositorySail(final Repository repository) {
		this.repository = repository;
	}

	@Override
	public boolean isWritable()
		throws SailException
	{
		return repository.isWritable();
	}

	@Override
	public ValueFactory getValueFactory() {
		return repository.getValueFactory();
	}

	@Override
	protected NotifyingSailConnection getConnectionInternal()
		throws SailException
	{
		// FIXME
		throw new SailException("Not yet implemented!");
	}

	@Override
	protected void shutDownInternal()
		throws SailException
	{
		// FIXME
		throw new SailException("Not yet implemented!");
	}

	@Override
	public void setDataDir(final File dataDir) {
		repository.setDataDir(dataDir);
	}

	@Override
	public File getDataDir() {
		return repository.getDataDir();
	}

	@Override
	public void initialize()
		throws SailException
	{
		if (!repository.isInitialized())
			repository.initialize();
	}

	@Override
	public void shutDown()
		throws SailException
	{
		if (repository.isInitialized())
			repository.shutDown();
	}

	@Override
	public NotifyingSailConnection getConnection()
		throws SailException
	{
		return new RepositorySailConnection(repository.getConnection());
	}

	@Override
	public List<IsolationLevel> getSupportedIsolationLevels() {
		final IsolationLevel level = this.getDefaultIsolationLevel();
		return Arrays.asList(new IsolationLevel[] { level });
	}

	@Override
	public IsolationLevel getDefaultIsolationLevel() {
		final RepositoryConnection connection = repository.getConnection();
		final IsolationLevel level = connection.getIsolationLevel();
		connection.close();
		return level;
	}

}
