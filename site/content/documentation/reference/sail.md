---
title: "The SAIL API"
toc: true
weight: 2
---
The SAIL (Storage And Inference Layer) API is a collection of interfaces designed for low-level transactional access to RDF data.
<!--more-->
It functions as a decoupling point between specific database implementations and the functional modules (parsers, query engines, end-user API access, etc) of the rdf4j framework.

Here, we document the design of the API and explain the roles and rationale behind the various interfaces. We also explain how various abstract base classes provided as part of the API can be reused by third-party implementors, in order to make implementing a SAIL-compatible database easier.

WARNING: this document is currently in draft, and incomplete. Feedback and suggestions for change are welcome, either on our [GitHub repo](https://github.com/eclipse/rdf4j-doc), or on the [rdf4j Users Group](https://groups.google.com/forum/#!forum/rdf4j-users).

{{< gravizo "SAIL Main interfaces" >}}
  @startuml;
  interface Sail {;
    + init();
    + getConnection();
    + shutDown();
    + isWritable();
    ..;
    + getSupportedIsolationLevels();
    + getDefaultIsolationLevel();
    ..;
    + getValueFactory();
    + setDataDir();
    + getDatadir();
  };
  interface SailConnection {;
    + isOpen();
    + close();
    .. query ..;
    + evaluate();
    + getContextIDs();
    + getNamespace();
    + getNamespaces();
    + getStatements();
    + hasStatement();
    + size();
    .. txn management ..;
    + begin();
    + flush();
    + prepare();
    + commit();
    + rollback();
    + isActive();
    + startupdate();
    + endUpdate();
    .. data modification ..;
    + addStatement();
    + removeStatements();
    + clear();
    + setNamespace();
    + removeNamespace();
    + clearNamespaces;
  };
  Sail "1" *- "*" SailConnection;
  @enduml
{{< / gravizo >}}

In the above diagram we see an overview of the two main interfaces: `Sail`, and `SailConnection`. The `Sail` interface is the main access point for RDF storage. Roughly speaking, this is "the database". Each `Sail` object is composed of zero or more `SailConnection` objects. This is where all the actual database access functionality is concentrated. `SailConnection` provides methods to execute queries, retrieve and modify triples, and manage transactions.

## AbstractSail and AbstractSailConnection

rdf4j provides default (abstract) implementations for most of the SAIL functionality, which can be reused (and of course overridden) by any concrete implementation. 

{{< gravizo "Abstract base implementations" >}}
  @startuml;
  interface Sail;
  interface SailConnection;
  abstract class AbstractSail {;
     + setDataDir();
     + getDataDir();
     + setDefaultIsolationLevel();
     + getIterationCacheSyncThreshold();
     + setIterationCacheSyncThreshold();
     # addSupportedIsolationLevel();
     # removeSupportedIsolationLevel();
     # setSupportedIsolationLevels();
     # isInitialized();
     #  getConnectionInternal();
     #  shutDownInternal();
     #  initializeInternal ();
  };
  abstract class AbstractSailConnection {;
      # transactionActive();
      # endUpdateInternal();
      #  closeInternal();
      #  evaluateInternal();
      #  getContextIDsInternal();
      #  getStatementsInternal();
      #  sizeInternal();
      #  startTransactionInternal();
      # prepareInternal();
      #  commitInternal();
      #  rollbackInternal();
      #  addStatementInternal();
      #  removeStatementsInternal();
      #  clearInternal();
      #  getNamespacesInternal();
      #  getNamespaceInternal();
      #  setNamespaceInternal();
      #  removeNamespaceInternal();
      #  clearNamespacesInternal();
      # isActiveOperation();
  };
  Sail <|-- AbstractSail;
  SailConnection <|-- AbstractSailConnection;
  AbstractSail <- AbstractSailConnection;
  @enduml
{{< /gravizo >}}

The `AbstractSail` class  provides base implementations of all methods of the `Sail` interface. It provides the following benefits to concrete Sail implementations:

. implementations of all required basic getter/setter methods
. store shutdown management, including grace periods for active connections and eventual forced closure of active connections on store shutdown.
. thread-safety: take care of basic concurrency issues around opening multiple connections.
. ongoing compatibility: future rdf4j releases that introduce new functionality in `Sail` provide default implementations in `AbstractSail`.

Similarly, the `AbstractSailConnection` provides base implementations of all methods of the `SailConnection` interface. It provides the following benefits to concrete SailConnection implementations:

. handles all basic concurrency issues around starting / executing transactions
. (configurable) buffering of active changes in any transaction
. ongoing compatibility: future rdf4j releases that introduce new functionality in `SailConnection` provide default implementations in `AbstractSailConnection`.

The abstract base classes use the naming convention ``methodname**Internal**`` to indicate the methods that concrete subclasses should concentrate on implementing. The rationale is that the public method implementations in the abstract class implement basic concurrency handling and other book-keeping, and their corresponding (protected) `...Internal` methods can be implemented by the concrete subclass to provide the actual business logic of the method. 

For example, the query method `AbstractSailConnection.getStatements()` provides a lot of book keeping: it ensures pending updates are flushed, acquires a read lock on the connection, verifies the connection is still open, and takes care of internally registering the resulting `Iteration` from the query for resource management and concurrency purposes. In between all of this, it calls `getStatementsInternal`. The only job of this method is to answer the query by retrieving the relevant data from the data source.

## NotifyingSail and AbstractNotifyingSail

The `NotifyingSail` and `NotifyingSailConnection` interfaces provide basic event handling for SAIL implementations. The main goal of these interfaces is to provide a messaging mechanism for closely-linked SAIL implementations (for example, a "Sail stack" where a reasoner is to be kept informed of changes to the underlying database). 

{{< gravizo "NotifyingSail interfaces" >}}
  @startuml;
  interface Sail;
  interface SailConnection;
  interface NotifyingSail {;
      + addSailChangedListener(SailChangedListener);
      + removeSailChangedListener(SailChangedListener);
  };
  interface NotifyingSailConnection {;
      + addConnectionListener(SailConnectionListener);
      + removeConnectionListener(SailConnectionListener);
  };
  interface SailChangedListener {;
      + sailChanged(SailChangedEvent);
  };
  interface SailChangedEvent {;
      + Sail getSail();
      + boolean statementsAdded();
      + boolean statementsRemoved();
  };
  ;
  interface SailConnectionListener {;
      + statementAdded(Statement st);
      + statementRemoved(Statement st);
  };
  abstract class AbstractNotifyingSail;
  abstract class AbstractNotifyingSailConnection;
  Sail <|-- NotifyingSail;
  SailConnection <|-- NotifyingSailConnection;
  NotifyingSail "1" *- "*" NotifyingSailConnection;
  NotifyingSail "0..1" o-- "*" SailChangedListener;
  SailChangedListener -d- SailChangedEvent;
  NotifyingSailConnection "0..1" o- "*" SailConnectionListener;
  AbstractNotifyingSail -u-|> NotifyingSail;
  AbstractNotifyingSail -u-|> AbstractSail;
  AbstractNotifyingSailConnection -u-|> NotifyingSailConnection;
  AbstractNotifyingSailConnection -r-|> AbstractSailConnection;
  @enduml
{{< / gravizo >}}

As can be seen in this diagram, the `NotifyingSail` interface provides the option of registering one or more `SailChangedListener` implementations. When registered, the listener will be messaged via the `sailChanged` method. The contents of the message is a `SailChangedEvent` that provides basic info on what has been changed.

More fine-grained event data is available at the Connection level. The `NotifyingSailConnection` allows registering a `SailConnectionListener`, which receives a message for each individual statement added or removed on the connection. 

Rdf4j also provides base implementation classes for these two interfaces. These classes - `AbstractNotifyingSail` and `AbstractNotifyingSailConnection` - are extensions of the AbstractSail(Connection) classes that add 
default implementations of the methods defined in the NotifyingSail(Connection) interfaces.

## Stacking SAILs

{{< gravizo "Stackable SAIL interface" >}}
  @startuml
  interface Sail
  interface StackableSail {
      + setBaseSail(Sail)
      + getBaseSail()
  }

  Sail <|-- StackableSail
  @enduml
{{< / gravizo >}}

The SAIL API provides the `StackableSail` interface to allow SAIL implementations to "stack" on top of each other, providing a chain of responsility: each SAIL implementation in the stack implements a specific feature (reasoning, access control, data filtering, query expansion, etc. etc.). The last SAIL implementation in the stack is expected to _not_ implement `StackableSail`, and this Sail is responsible for the actual persistence of the data. rdf4j's `NativeStore` and `MemoryStore` are implementations of such a persistence SAIL, while the `SchemaCachingRDFSInferencer` (responsible for RDFS inferencing) and `LuceneSail` (responsible for full-text indexing) are examples of `StackableSail` implementations.

# Querying

The SAIL API has no knowledge of SPARQL queries. Instead, it operates on a query algebra, that is, an object representation of a (SPARQL) query as provided by the SPARQL query parser. 

`SailConnection` has a single `evaluate()` method, which accepts a `org.eclipse.rdf4j.queryalgebra.model.TupleExpr` object. This is the object representation of the query as produced by the query parser. 

# Transactions

TODO

