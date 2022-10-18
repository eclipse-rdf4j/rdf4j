---
title: "Integration with Spring"
weight: 7
toc: true
autonumbering: true
---

The {{< javadoc "rdf4j-spring" "spring/" >}} module allows for using an RDF4J repository as the data backend of a spring application.
<!--more-->

A self-contained demo application can be found at {{< javadoc "rdf4j-spring-demo" "spring/demo" >}}

## Getting Started

To use RDF as the data backend of a spring application built with maven, use these dependencies: 

```xml
    <dependency>
        <groupId>org.eclipse.rdf4j</groupId>
        <artifactId>rdf4j-spring</artifactId>
        <version>${rdf4j.version}</version>
    </dependency>
```  
... setting the property `rdf4j.version` is set to the RDF4J version you want (minimum `4.0.0`).

In order for the application to run, a repository has to be configured:

To configure the application to access an existing repository, set the following configuration properties, e.g. in `application.properties`:
```properties
 rdf4j.spring.repository.remote.manager-url=http://localhost:7200
 rdf4j.spring.repository.remote.name=myrepo

 # Optional with username and password
 rdf4j.spring.repository.remote.username=admin
 rdf4j.spring.repository.remote.password=1234
```

To use an in-memory repository (for example, for unit tests), use
```properties
rdf4j.spring.repository.inmemory.enabled=true
```


## Programming with RDF4J-Spring

The main purpose of `rdf4j-spring` is to support accessing an RDF4J repository using the [DAO pattern](https://en.wikipedia.org/wiki/Data_access_object). 
DAOs are subclasses of {{< javadoc "RDF4JDao" "spring/dao/RDF4JDao.html" >}} and use 
the {{< javadoc "RDF4JTemplate" "/spring/support/RDF4JTemplate.html" >}} for accessing 
the [RDF4J repository configured for the application]({{< relref "spring.md#configuring-a-repository" >}}). 

### RDF4JTemplate

The `RDF4JTemplate` is the class used to access a `Repository` in `rdf4j-spring`. A bean of this type is configured
at start up and available for wiring into beans. The `RDF4JTemplate` accesses the `Repository` through a `RepositoryConnection` 
that it obtains from a {{< javadoc "RepositoryConnectionFactory" "spring/support/connectionFactory/RepositoryConnectionFactory.html" >}}. 
This indirection allows for using a connection pool, connect RDF4J to spring's transaction management, and provide 
query logging to a file or exposing query statistics via JMX. These features can be enabled/disabled using 
configuration properties (see [Configuration]({{< relref "spring.md#configuration" >}}))

#### Wiring into a spring bean
To use the `RDF4JTemplate` in a bean, define that bean in the spring application's configuration and wire the `RDF4JTemplate` in:
```java
@Configuration
@Import(RDF4JConfig.class)
public class MyAppConfig {
	@Bean
	public MyBeanClass getMyBean(@Autowired RDF4JTemplate template){
        return new MyBeanClass(template);
    }    
}
```
```java
public class MyBeanClass {

	private RDF4JTemplate rdf4JTemplate;
    
    public MyBeanClass(RDF4JTemplate template){
        this.rdf4jTemplate = template;
    }
}
``` 
 
#### Evaluating queries and executing updates 
 
The RDF4JTemplate offers various ways to access the repository. 
For example, to evaluate a `TupleQuery` using the `RDF4JTemplate` (in this case, counting all triples):

```java
int count = rdf4JTemplate
				.tupleQuery("SELECT (count(?a) as ?cnt) WHERE { ?a ?b ?c }")
				.evaluateAndConvert()
				.toSingleton(bs -> TypeMappingUtils.toInt(QueryResultUtils.getValue(bs, "cnt")));
``` 

The query, provided through the `tupleQuery` method, is executed with the call to `evaluateAndConvert()`, which returns 
a {{< javadoc "TupleQueryResultConverter" "spring/dao/support/operation/TupleQueryResultConverter.html" >}}. 
The latter provides methods for converting the `TupleQueryResult` of the query into an object, an `Optional`, a `Map`, 
`Set`, `List`, or `Stream`. In the example, we are just interested in the count as an `int` - one single object - so we use 
the `toSingleton()` method and convert the value of the projection variable to an int. The conversion is done 
using {{< javadoc "TypeMappingUtils" "spring/util/TypeMappingUtils.html" >}}; the 
extraction of the variable's value from the `BindingSet bs` is done using 
{{< javadoc "QueryResultUtils" "spring/util/QueryResultUtils.html" >}}.

#### Pre-binding variables

For binding variables before executing a query or update, use the {{< javadoc "OperationBuilder" "spring/dao/support/opbuilder/OperationBuilder.html" >}}
returned by the `tupleQuery()`, `graphQuery`, or `update` methods. It provides various `withBinding()` methods following 
the builder pattern, allowing for binding variables, as illustrated in the following example.
```java
Set<IRI> artists = rdf4JTemplate
                    .tupleQuery("PREFIX ex: <http://example.org/>"
				            + "SELECT distinct ?artist "
				            + "WHERE { ?artist a ?type }")
				    .withBinding("type", EX.Artist)
				    .evaluateAndConvert()
				    .toSet(bs -> QueryResultUtils.getIRI(bs, "artist"));
```   

#### Using the RepositoryConnection directly

For using the {{< javadoc "RepostoryConnection" "repository/RepositoryConnection.html" >}} directly, 
without the need to generate a result, the `consumeConnection()` method is used:

```java 
rdf4JTemplate.consumeConnection(con -> con.remove(EX.Picasso, RDF.TYPE, EX.Artist);
```

Alternatively, to generate a result, the `applyToConnection()` method is used:
```java 
boolean isPresent = rdf4JTemplate.applyToConnection(
                        con -> con.hasStatement(EX.Picasso, RDF.TYPE, EX.Artist, true);
```

#### Using SPARQL queries/updates from external files

For running queries or updates from external resources, the `[(tupleQuery|graphQuery|update)FromResource]` methods can be used.

For example, the `sparql/construct-artists.rq` file on the classpath might contain this query:
```sparql
PREFIX ex: <http://example.org/>
CONSTRUCT {?artist ?p ?o } WHERE { ?artist a ex:Artist; ?p ?o }
```
and could be evaluated using
```java
Model model = rdf4JTemplate.graphQueryFromResource(
                        getClass(), 
                        "classpath:sparql/construct-artists.rq")
				        .evaluateAndConvert()
				        .toModel();
```

The resource to be read is resolved by spring's [ResourceLoader](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/io/ResourceLoader.html), 
which supports fully qualified URLs (e.g., `file://` URLs, relative paths and `classpath:` pseudo-URLs.) 

### Implementing a DAO 

Any spring bean that uses the `RDF4JTemplate` can be seen as a DAO and participates in transactionality, query logging, 
caching, etc. However, `rdf4j-spring` provides a few base classes that provide frequently used functionality.

#### RDF4JDao
{{< javadoc "RDF4JDao" "spring/dao/RDF4JDao.html" >}} is a suitable base class for a general-purpose DAO. It provides 
two functionalities to subclasses: 
    
* The `RDF4JTemplate` is automatically wired into the bean and it is available through `getRDF4JTemplate()`
    
* It provides a simple management facility for SPARQL query/update strings. This allows for SPARQL queries being
generated only once (by String concatenation, read from a file, or built with the [SparqlBuilder](../../tutorials/sparqlbuilder)).
The queries are prepared in the template method `prepareNamedSparqlSuppliers()`:  

In the following example, we
* create a DAO, extending `RDF4JDao`
* annotate it with [`@Component`](https://docs.spring.io/spring-framework/docs/current/javadoc-api/index.html?org/springframework/core/io/package-summary.html) 
so it gets auto-detected during Spring's component scan
* create an inner class, `QUERY_KEYS`, as a container for `String` constants we use for query keys
* implement the {{< javadoc "prepareNamedSparqlSuppliers" "spring/support/RDF4JDao.html#prepareNamedSparqlSuppliers(org.eclipse.rdf4j.spring.dao.RDF4JDao.NamedSparqlSupplierPreparer)" >}} method and add one query
* use the prepared query in a DAO method (`getArtistsWithoutPaintings()`). We access the prepared query with {{< javadoc "getNamedTupleQuery(String)" "spring/support/RDF4JDao.html#getNamedTupleQuery(java.lang.String)" >}}, passing the constant we defined in `QUERY_KEYS`.

  
```java

@Component // make the DAO a spring component so it's auto-detected in the classpath scan
public class ArtistDao extends RDF4JDao {
    
    // constructor, other methods etc

    // recommended: encapsulate the keys for queries in an object
    // so it's easier to find them when you need them
	static abstract class QUERY_KEYS {
		public static final String ARTISTS_WITHOUT_PAINTINGS = "artists-without-paintings";
	}

    // prepare the named queries, assigning each one of the keys
	@Override
	protected NamedSparqlSupplierPreparer prepareNamedSparqlSuppliers(NamedSparqlSupplierPreparer preparer) {
		return preparer
            .forKey(QUERY_KEYS.ARTISTS_WITHOUT_PAINTINGS)
            .supplySparql(Queries.SELECT(
                            ARTIST_ID)
                            .where(
                                ARTIST_ID.isA(iri(EX.Artist))
                                .and(ARTIST_ID.has(iri(EX.creatorOf), Painting.PAINTING_ID).optional())
                                .filter(not(bound(Painting.PAINTING_ID)))).getQueryString()
            );
	}

    // use the named query with getNamedTupleQuery(String)
	public Set<Artist> getArtistsWithoutPaintings(){
		return getNamedTupleQuery(QUERY_KEYS.ARTISTS_WITHOUT_PAINTINGS)
						.evaluateAndConvert()
						.toStream()
						.map(bs -> QueryResultUtils.getIRI(bs, ARTIST_ID))
						.map(iri -> getById(iri))
						.collect(Collectors.toSet());
	}
    
    // ...

}
```
#### SimpleRDF4JCRUDDao

The {{< javadoc "SimpleRDF4JCRUDDao" "spring/dao/SimpleRDF4JCRUDDao.html" >}} is a suitable base class for a DAO for
creating, reading, updating, and deleting one class of entities. It requires two type parameters, `ENTITY` and `ID`.
It provides create, read, update, and delete functionality for the `ENTITY` class, using the `ID` class wherever the 
entity's identifier is required. 

Subclasses of `SimpleRDF4JCRUDDao` must implement a couple of template methods in order to customize the generic 
behaviour for the specific entity and id classes.

In the following, we use the entity {{< javadoc "Artist" "spring/demo/model/Artist.html">}} (as used in the demo 
application) as an example. Note that we define public constants of type {{< javadoc "Variable" "sparqlbuilder/core/Variable.html" >}}, 
one corresponding to each of the entity's fields.
 
```java
public class Artist {
    // recommended pattern: use a public Variable constant for each of the entities fields 
    // for use in queries and result processing. 
	public static final Variable ARTIST_ID = SparqlBuilder.var("artist_id"); 
	public static final Variable ARTIST_FIRST_NAME = SparqlBuilder.var("artist_firstName");
	public static final Variable ARTIST_LAST_NAME = SparqlBuilder.var("artist_lastName");
	private IRI id;
	private String firstName;
	private String lastName;
    // getter, setter, constructor, ...
    // be sure to implement equals() and hashCode() for proper behaviour of collections!
}
``` 

The {{< javadoc "ArtistDao" "spring/demo/dao/ArtistDao.html">}} is shown in the following code snippets.

We recommend to use `@Component` for auto-detection. Implementing the constructor is required. 
```java
@Component // again, make it a component (see above) 
public class ArtistDao extends SimpleRDF4JCRUDDao<Artist, IRI> {

	public ArtistDao(RDF4JTemplate rdf4JTemplate) {
		super(rdf4JTemplate);
	}
``` 

The `populateIdBindings` method is called by the superclass to bind the id to variable(s) in a SPARQL query.
```java
	@Override
	protected void populateIdBindings(MutableBindings bindingsBuilder, IRI iri) {
		bindingsBuilder.add(ARTIST_ID, iri);
	}
```

The `populateBindingsForUpdate` method is called by the superclass to bind all non-id variables when performing an update.
```java
	@Override
	protected void populateBindingsForUpdate(MutableBindings bindingsBuilder, Artist artist) {
		bindingsBuilder
				.add(ARTIST_FIRST_NAME, artist.getFirstName())
				.add(ARTIST_LAST_NAME, artist.getLastName());
	}
```

The `mapSolution` method converts a query solution, i.e., a {{< javadoc "BindingSet" "query/BindingSet.html">}}, to an instance of the entity.
```java
	@Override
	protected Artist mapSolution(BindingSet querySolution) {
		Artist artist = new Artist();
		artist.setId(QueryResultUtils.getIRI(querySolution, ARTIST_ID));
		artist.setFirstName(QueryResultUtils.getString(querySolution, ARTIST_FIRST_NAME));
		artist.setLastName(QueryResultUtils.getString(querySolution, ARTIST_LAST_NAME));
		return artist;
	}
```

The `getReadQuery` method provides the SPARQL string used to read one entity. Note that the variable names must be the 
same ones used in `mapSolution(BindingSet)`. It may be cleaner to use the SparqlBuilder for generating this string.
```java
	@Override
	protected String getReadQuery() {
		return "prefix foaf: <http://xmlns.com/foaf/0.1/> "
				+ "prefix ex: <http://example.org/> "
				+ "SELECT ?artist_id ?artist_firstName ?artist_lastName where {"
				+ "?artist_id a ex:Artist; "
				+ "    foaf:firstName ?artist_firstName; "
				+ "    foaf:surname ?artist_lastName ."
				+ " } ";
	}
```

The `getInsertSparql(ENTITY)` method provides the SPARQL string for inserting a new instance. This SPARQL operation
will also be used for updates. If updates require a different operation from inserts, it must be provided by implementing
`getUpdateSparql(ENTITY)`. 
```java
	@Override
	protected NamedSparqlSupplier getInsertSparql(Artist artist) {
		return NamedSparqlSupplier.of("insert", () -> Queries.INSERT(ARTIST_ID.isA(iri(EX.Artist))
				.andHas(iri(FOAF.FIRST_NAME), ARTIST_FIRST_NAME)
				.andHas(iri(FOAF.SURNAME), ARTIST_LAST_NAME))
				.getQueryString());
	}
```

The `getInputId(ENTITY)` method is used to generate the `id` of an entity to be inserted. Here, we use the `id` of the
specified `artist` object; if it is null we generate a new `IRI` using `getRdf4JTemplate().getNewUUID()`.  
```java
	@Override
	protected IRI getInputId(Artist artist) {
		if (artist.getId() == null) {
			return getRdf4JTemplate().getNewUUID();
		}
		return artist.getId();
	}
}
```

##### Composite Keys

If the entity uses a composite key, a class implementing {{< javadoc "CompositeKey" "spring/dao/support/key/CompositeKey.html">}} 
must be used for the `ID` type parameter. For a key consisting of two components, the {{< javadoc "CompositeKey2" "spring/dao/support/key/CompositeKey2.html">}}
class is available. If more components are needed, the key class can be modeled after that one.

##### RelationMapBuilder

It is not uncommon for an application to read a relation present in the repository data into a `Map`. For example, we
might want to group painting `id`s by artist `id`. The {{<javadoc "RelationMapBuilder" "spring/dao/support/RelationMapBuilder.html">}}
provides the necesary functionality for such cases:

```java
RelationMapBuilder b = new RelationMapBuilder(getRDF4JTemplate(), EX.creatorOf);
Map<IRI, Set<IRI>> paintingsByArtists = b.buildOneToMany();
```  
Additional Functionality:
* The `constraints(GraphPattern)` method restricts the relation 
* The `relationIsOptional()` method allows for the object to be missing, in which case an empty set is generated for the subject key.
* The `useRelationObjectAsKey()` method flips the map such that the objects of the relation are used as keys and the subjects are aggregated.
* The `buildOneToOne()` method returns a one to one mapping, which dies horribly if the data is not 1:1 

#### RDF4JCRUDDao

The {{< javadoc "RDF4JCRUDDao" "spring/dao/RDF4JCRUDDao.html" >}} is essentially the same as the `SimpleRDF4JCRUDDao`, 
with the one difference that it has three type parameters, `ENTITY`, `INPUT`, and `ID`. The class thus allows different 
classes for input and output: creation and updates use `INPUT`, e.g. `save(INPUT)`, reading methods use `ENTITY`, e.g.
`ENTITY getById(ID)`. 


### Service Layer

Usually, the functionality offered by DAOs is rather narrow, e.g. CRUD methods for one entity class. They 
are combined to provide a wider range of functionality in the *servcie layer*. The only thing one 
needs to know when implementing the service layer with `rdf4j-spring` DAOs is that its methods need to participate
in spring's transaction management. The most straightforward way to do this is to use the [`@Transactional`](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/annotation/Transactional.html)
method annotation, causing the service object to be wrapped with a proxy that takes care of transactionality. 
 
The following code snippet, taken from the demo's {{< javadoc "ArtService" "spring/demo/service/ArtService.html">}} class, 
shows part of a simple service.  
 
```java
@Component
public class ArtService {
	
    @Autowired
	private ArtistDao artistDao;

	@Autowired
	private PaintingDao paintingDao;

	@Transactional
	public Artist createArtist(String firstName, String lastName) {
		Artist artist = new Artist();
		artist.setFirstName(firstName);
		artist.setLastName(lastName);
		return artistDao.save(artist);
	}

	@Transactional
	public Painting createPainting(String title, String technique, IRI artist) {
		Painting painting = new Painting();
		painting.setTitle(title);
		painting.setTechnique(technique);
		painting.setArtistId(artist);
		return paintingDao.save(painting);
	}

	@Transactional
	public List<Painting> getPaintings() {
		return paintingDao.list();
	}

	@Transactional
	public List<Artist> getArtists() {
		return artistDao.list();
	}

	@Transactional
	public Set<Artist> getArtistsWithoutPaintings(){
		return artistDao.getArtistsWithoutPaintings();
	}
    
    // ...

}
```   

## Testing with Junit 5

Testing an application built with `rdf4j-spring` can be done at the DAO layer as well as on the service layer. Generally,
applications will have more than one test classes.

The common approach is to have a configuration for tests that is shared by all tests, and this configuration prepares
the spring context with all the required facilities. A minimal, shared test configuration is the following. Note that 
it imports {{< javadoc "RDF4JTestConfig" "spring/test/RDF4JTestConfig.html">}}:

```java
@TestConfiguration
@EnableTransactionManagement
@Import(RDF4JTestConfig.class)
@ComponentScan("com.example.myapp.dao")
public class TestConfig {

    @Bean
    DataInserter getDataInserter() {
        return new DataInserter();
    }
    
}
``` 

With this configuration, a test class can use the `dataInserter` bean to insert data into an inmemory repository before
each test:

```java
@ExtendWith(SpringExtension.class)
@Transactional
@ContextConfiguration(classes = { TestConfig.class })
@TestPropertySource("classpath:application.properties")
@TestPropertySource(
		properties = {
				"rdf4j.spring.repository.inmemory.enabled=true",
				"rdf4j.spring.repository.inmemory.use-shacl-sail=true",
				"rdf4j.spring.tx.enabled=true",
				"rdf4j.spring.resultcache.enabled=false",
				"rdf4j.spring.operationcache.enabled=false",
				"rdf4j.spring.pool.enabled=true",
				"rdf4j.spring.pool.max-connections=2"
		})
@DirtiesContext
public class ArtistDaoTests {

    @Autowired
    private ArtistDao artistDao;       

	@BeforeAll
	public static void insertTestData(
			@Autowired DataInserter dataInserter,
			@Value("classpath:/data/my-testdata.ttl") Resource dataFile) {
		dataInserter.insertData(dataFile);
	}
    
    @Test
    public void testReadArtist(){
         // ...          
    }      

}
```

### Testing against a local database
 
The inmemory repository is likely to behave differently from any database used in production in some edge cases. It
is recommended to test against a local installation of the database that is used in production in addition to testing
against the inmemory repository. 

With `rdf4j-spring` this is quite straightforward:
1. install the database locally and create a repository for the tests
2. provide a property file on the classpath with the necessary properties to connect to that repository 
(`rdf4j.spring.repository.remote.*` properties)
3. Create a subclass of your test class and provide the properties file through the `@TestPropertySource` annotation
4. Use the `@Tag` annotation, so you can easily switch the test on or off using the configuration of your test environment 
(most likely the Maven Surefire Plugin), as the local database installation will not be present in many build environments.

Example:
```java
@Tag("requires-local-database")
@TestPropertySource("classpath:/repository-localdb.properties")
public class ArtistDaoDbTests extends ArtistDaoTests {
// no code needed, the class is just created to run your ArtistDaoTests with a different configuration     
}
```
   
   
 
## Debugging

In addition to [query logging]({{< relref "spring.md#query-logging" >}}), if you need to get a close look at what's happening inside the `rdf4j-spring` code, 
set the loglevel for `org.eclipse.rdf4j.spring` to `DEBUG`. Sometimes it may be required to look into what spring is doing. 
In this case, set `org.springframework` to `DEBUG` or even `TRACE`.

One way to do this is to provide a `logback.xml` file on the classpath, as can be found in the source at `rdf4j-spring/src/test/resources/logback.xml`.

Another way to set the loglevel is to provide an application property starting with `logging.level.`, e.g.
```properties
logging.level.org.eclipse.rdf4j.spring=DEBUG
```
which can be provided in an `application.properties` (for details and other ways to do that, have a look at the documentation on [Externalied Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config) in Spring).

 

## Configuration 

`rdf4j-spring` makes use of the auto-configuration feature in Spring (configured in the source file `rdf4j-spring/META-INF/spring.factories`).
That means that bean creation at start up is governed by configuration properties, all of which are prefixed by `rdf4j.spring.` 

The following table shows all subsystems with their property prefixes, the packages they reside in, and the class holding their properties. 
  
| Subsystem | property prefix | package (links to reference) | Properties class |
| --------- | ----------------| --------| -----------------|
| [Repository]({{< relref "spring.md#repository" >}}) | `rdf4j.spring.repository.`| {{< javadoc "org.eclipse.rdf4j.spring.repository" "spring/repository/package-summary.html" >}} | {{< javadoc "RemoteRepositoryProperties" "spring/repository/remote/RemoteRepositoryProperties.html">}} and {{< javadoc "InMemoryRepositoryProperties" "spring/repository/inmemory/InMemoryRepositoryProperties.html">}} |
| [Transaction management]({{< relref "spring.md#transaction-management" >}}) | `rdf4j.spring.tx.` | {{< javadoc "org.eclipse.rdf4j.spring.tx" "spring/tx/package-summary.html" >}} | {{< javadoc "TxProperties" "spring/tx/TxProperties.html">}} |
| [Connection Pooling]({{< relref "spring.md#connection-pooling" >}}) | `rdf4j.spring.pool.` | {{< javadoc "org.eclipse.rdf4j.spring.pool" "spring/pool/package-summary.html" >}} | {{< javadoc "PoolProperties" "spring/pool/PoolProperties.html">}} | 
| [Operation caching]({{< relref "spring.md#operation-caching" >}}) | `rdf4j.spring.operationcache.` | {{< javadoc "org.eclipse.rdf4j.spring.operationcache" "spring/operationcache/package-summary.html" >}} | {{< javadoc "OperationCacheProperties" "spring/operationcache/OperationCacheProperties.html">}} |      
| [Operation logging]({{< relref "spring.md#operation-logging" >}}) | `rdf4j.spring.operationlog.` | {{< javadoc "org.eclipse.rdf4j.spring.operationlog" "spring/operationlog/package-summary.html" >}} | {{< javadoc "OperationLogProperties" "spring/operationlog/OperationLogProperties.html">}} and {{< javadoc "OperationLogJmxProperties" "spring/operationlog/log/jmx/OperationLogJmxProperties.html">}} |
| [Query result caching]({{< relref "spring.md#query-result-caching" >}}) | `rdf4j.spring.resultcache.` | {{< javadoc "org.eclipse.rdf4j.spring.resultcache" "spring/resultcache/package-summary.html" >}} | {{< javadoc "ResultCacheProperties" "spring/resultcache/ResultCacheProperties.html">}} |         
| [UUIDSource]({{< relref "spring.md#uuidsource" >}})| `rdf4j.spring.uuidsource.` |{{< javadoc "org.eclipse.rdf4j.spring.uuidsource" "spring/uuidsource/package-summary.html" >}} | {{< javadoc "SimpleUUIDSourceProperties" "spring/uuidsource/simple/SimpleUUIDSourceProperties.html">}}, {{< javadoc "NoveltyCheckingUUIDSourceProperties" "spring/uuidsource/noveltychecking/NoveltyCheckingUUIDSourceProperties.html">}}, {{< javadoc "UUIDSequenceProperties" "spring/uuidsource/sequence/UUIDSequenceProperties.html">}}, and {{< javadoc "PredictableUUIDSourceProperties" "spring/uuidsource/predictable/PredictableUUIDSourceProperties.html">}}  | 

These subsystems and their configuration are described in more detail below.

### Repository

As stated in the [Getting Started]({{< relref "spring.md#getting-started" >}}) section, to configure the application 
to access an existing repository, set the following configuration properties, e.g. in `application.properties`:
```properties
 rdf4j.spring.repository.remote.manager-url=[manager-url]
 rdf4j.spring.repository.remote.name=[name]
```
To use an in-memory repository (for example, for unit tests), use
```properties
rdf4j.spring.repository.inmemory.enabled=true
```

### Transaction management

By default, `rdf4j-spring` connects with Spring's PlatformTransactionManager. To disable this connection, use 
```properties
rdf4j.spring.tx.enabled=false
```

### Connection Pooling

Creating a `RepositoryConnection` has a certain overhead that many applications wish to avoid. `rdf4j-spring` allows for 
pooling of such connections. Several configuration options, such as the maximum number of connections, are available
(see {{< javadoc "PoolProperties" "spring/pool/PoolProperties.html" >}}).

To enable, use
```properties
rdf4j.spring.pool.enabled=true
``` 

### Operation caching

SPARQL operations (queries and updates) require some computation time to prepare from the SPARQL string they are based on.
In `rdf4j-spring`, this process is hidden from clients and happens in the `RDF4JTemplate`. By default, operations 
are not cached, and the same operation executed multiple times always has the overhead of parsing the SPARQL string and 
generating the operation. If this feature is enabled, operations are cached per connection.
 
Note: If [connection pooling]({{< relref "spring.md#connection-pooling" >}}) is enabled, it is possible that operations created in different threads will use different connections and will therefore
all generate their own instance of the SPARQL operation, thus reducing the speedup incurred by operation caching.  

To enable, use
```properties
rdf4j.spring.operationcache.enabled=true
```

### Operation logging 
(aka Query logging)

Two options are available for logging operations (queries and updates) sent to the repository:

#### Operation logging via SLF4J
Each operation is written to the logger `org.eclipse.rdf4j.spring.operationlog.log.slf4` with loglevel `DEBUG`.

To enable, use
````properties
rdf4j.spring.operationlog.enabled=true
````

#### Operation logging via JMX

Each operation is recorded (if identical operations are executed, statistics are aggregated) and exposed via JMX. 

To enable, use
```properties
rdf4j.spring.operationlog.jmx.enabled=true
``` 

### Query result caching

Applications that frequently execute the same queries might profit from result caching. If enabled, query results are 
cached on a per-connection basis. By default, this cache is cleared at the end of the ongoing transaction. The performance
impact of result caching is application-specific and is not unlikely to be negative. Measure carefully! 

However, if the application is the only one using the `repository`, and therefore no updates are possible that the 
application does not know about, the property `rdf4j.spring.resultcache.assumeNoOtherRepositoryClients=true` can be set. 
In this case, results are copied to a global cache that all connections have access to, and which is only cleared when
the application executes an update. 

To enable result caching, use
```properties
rdf4j.spring.resultcache.enabled=true
```
### UUIDSource

Using UUIDs as identifiers for entities is a common strategy for applications using an RDF store as their backend. Doing
this requires a source of new, previously unused UUIDs for new entities created by the application. Conversely, in
unit tests, it is sometimes required that the UUIDs are generated in a predictable manner, so that actual results
can be compared with expected results containing generated UUIDs.

The UUIDSource subsystem provides different implementations of the {{< javadoc "UUIDSource" "spring/support/UUIDSource.html">}} 
interface. The configuration of this subsystem determines which implementation is wired into the `RDF4JTemplate` at
start up and gets used by the application.

In our opinion, the default implementation, {{< javadoc "DefaultUUIDSource" "spring/support/DefaultUUIDSource.html">}} 
is sufficient for generating previously unused UUIDs. Collisions are possible but sufficiently unlikely, so using 
any one of `noveltychecking`, `sequence`, and `simple` subsystems should not be necessary. 

For using the `predictable` UUIDSource, which always produces the same sequence of UUIDs, use
```properties
rdf4j.spring.uuidsource.predictable.enabled=true
```  

## Acknowledgments

The RDF4J-Spring module, the RDF4J-Spring-Demo, and this documentation have been developed in the project 
'BIM-Interoperables Merkmalservice', funded by the Austrian Research Promotion Agency and Österreichische Bautechnik 
Veranstaltungs GmbH.
