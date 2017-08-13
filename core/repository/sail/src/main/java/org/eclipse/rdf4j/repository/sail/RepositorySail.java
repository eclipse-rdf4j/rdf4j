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
import org.eclipse.rdf4j.sail.SailChangedEvent;
import org.eclipse.rdf4j.sail.SailChangedListener;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.AbstractNotifyingSail;

/**
 * This class should offer a way for handling external third-party Repository implementation, wrapping them
 * into a working Sail instance, in order to being able to assemble them in a SailStack (eg: LuceneSail...).
 * Another possible scenario could be for creating a TransactionLog on the Sail interface, wrapping around
 * existing implementations which does not offer any Sail object by themselves. Note that the implementation
 * is still work-in-progress.
 * 
 * <pre>
 * Repository repository = new SailRepository(new new MemoryStore());
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
	public void addSailChangedListener(final SailChangedListener listener) {
		super.addSailChangedListener(listener);
	}

	@Override
	public NotifyingSailConnection getConnection()
		throws SailException
	{
		if (!repository.isInitialized()) {
			throw new SailException("Sail is not initialized or has been shut down");
		}
		return getConnectionInternal();
	}

	@Override
	protected NotifyingSailConnection getConnectionInternal()
		throws SailException
	{
		return new RepositorySailConnection(repository.getConnection());
	}

	@Override
	public File getDataDir() {
		return repository.getDataDir();
	}

	@Override
	public IsolationLevel getDefaultIsolationLevel() {
		final RepositoryConnection connection = repository.getConnection();
		final IsolationLevel level = connection.getIsolationLevel();
		connection.close();
		return level;
	}

	@Override
	public List<IsolationLevel> getSupportedIsolationLevels() {
		final IsolationLevel level = this.getDefaultIsolationLevel();
		return Arrays.asList(new IsolationLevel[] { level });
	}

	@Override
	public ValueFactory getValueFactory() {
		return repository.getValueFactory();
	}

	@Override
	public void initialize()
		throws SailException
	{
		if (!repository.isInitialized())
			repository.initialize();
	}

	@Override
	protected boolean isInitialized() {
		return super.isInitialized();
	}

	@Override
	public boolean isWritable()
		throws SailException
	{
		return repository.isWritable();
	}

	/**
	 * Notifies all registered SailChangedListener's of changes to the contents of this Sail.
	 */
	public void notifySailChanged(final SailChangedEvent event) {
		super.notifySailChanged(event);
	}

	@Override
	public void removeSailChangedListener(final SailChangedListener listener) {
		super.removeSailChangedListener(listener);
	}

	@Override
	public void setConnectionTimeOut(long connectionTimeOut) {
		super.setConnectionTimeOut(connectionTimeOut);
	}

	@Override
	public void setDataDir(final File dataDir) {
		repository.setDataDir(dataDir);
	}

	@Override
	public void shutDown()
		throws SailException
	{
		if (!repository.isInitialized()) {
			throw new SailException("Sail is not initialized or has been shut down");
		}
		this.shutDownInternal();
	}

	@Override
	protected void shutDownInternal()
		throws SailException
	{
		if (repository.isInitialized())
			repository.shutDown();
	}

}
