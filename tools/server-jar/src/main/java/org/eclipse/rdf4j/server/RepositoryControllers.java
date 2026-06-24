package org.eclipse.rdf4j.server;

import org.eclipse.rdf4j.http.server.repository.RepositoryController;
import org.eclipse.rdf4j.http.server.repository.RepositoryInterceptor;
import org.eclipse.rdf4j.http.server.repository.RepositoryListController;
import org.eclipse.rdf4j.http.server.repository.config.ConfigController;
import org.eclipse.rdf4j.http.server.repository.contexts.ContextsController;
import org.eclipse.rdf4j.http.server.repository.graph.GraphController;
import org.eclipse.rdf4j.http.server.repository.namespaces.NamespaceController;
import org.eclipse.rdf4j.http.server.repository.namespaces.NamespacesController;
import org.eclipse.rdf4j.http.server.repository.size.SizeController;
import org.eclipse.rdf4j.http.server.repository.statements.StatementsController;
import org.eclipse.rdf4j.http.server.repository.transaction.TransactionController;
import org.eclipse.rdf4j.http.server.repository.transaction.TransactionStartController;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class RepositoryControllers {

	@NonNull
	@Autowired
	@Bean(name = "rdf4jRepositoryInterceptor")
	@RequestScope
	public RepositoryInterceptor rdf4jRepositoryInterceptor(
			@NonNull @Qualifier("rdf4jRepositoryManager") final RepositoryManager repositoryManager) {
		final RepositoryInterceptor interceptor = new RepositoryInterceptor();
		interceptor.setRepositoryManager(repositoryManager);

		return interceptor;
	}

	@NonNull
	@Autowired
	@Bean(name = "rdf4jRepositoryController")
	public RepositoryController repositoryController(
			@NonNull @Qualifier("rdf4jRepositoryManager") final RepositoryManager repositoryManager) {
		final RepositoryController controller = new RepositoryController();
		controller.setRepositoryManager(repositoryManager);

		return controller;
	}

	@NonNull
	@Autowired
	@Bean(name = "rdf4jRepositoryListController")
	public RepositoryListController repositoryListController(
			@NonNull @Qualifier("rdf4jRepositoryManager") final RepositoryManager repositoryManager) {
		final RepositoryListController controller = new RepositoryListController();
		controller.setRepositoryManager(repositoryManager);

		return controller;
	}

	@NonNull
	@Autowired
	@Bean(name = "rdf4jRepositoryConfigController")
	public ConfigController rdf4jRepositoryConfigController(
			@NonNull @Qualifier("rdf4jRepositoryManager") final RepositoryManager repositoryManager) {
		final ConfigController controller = new ConfigController();
		controller.setRepositoryManager(repositoryManager);

		return controller;
	}

	@NonNull
	@Bean(name = "rdf4jRepositoryContextsController")
	public ContextsController rdf4jRepositoryContextsController() {
		return new ContextsController();
	}

	@NonNull
	@Bean(name = "rdf4jRepositoryNamespacesController")
	public NamespacesController rdf4jRepositoryNamespacesController() {
		return new NamespacesController();
	}

	@NonNull
	@Bean(name = "rdf4jRepositoryNamespaceController")
	public NamespaceController rdf4jRepositoryNamespaceController() {
		return new NamespaceController();
	}

	@NonNull
	@Bean(name = "rdf4jRepositorySizeController")
	public SizeController rdf4jRepositorySizeController() {
		return new SizeController();
	}

	@NonNull
	@Bean(name = "rdf4jRepositoryStatementsController")
	public StatementsController rdf4jRepositoryStatementsController() {
		return new StatementsController();
	}

	@NonNull
	@Bean(name = "rdf4jRepositoryGraphController")
	public GraphController rdf4jRepositoryGraphController() {
		return new GraphController();
	}

	@NonNull
	@Bean(name = "rdf4jRepositoryTransactionController")
	public TransactionController rdf4jRepositoryTransactionController() {
		return new TransactionController();
	}

	@NonNull
	@Bean(name = "rdf4jRepositoryTransactionStartController")
	public TransactionStartController rdf4jRepositoryTransactionStartController(
			@Nullable @Value("${rdf4j.externalurl:#{null}}") final String externalUrl
	) {
		final TransactionStartController transactionStartController = new TransactionStartController();
		transactionStartController.setExternalUrl(externalUrl);

		return transactionStartController;
	}

}
