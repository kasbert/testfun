Project TestFun - JEE JUnit Testing Is Fun<br>(no server is needed!)
================================================================
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.testfun/jee/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.testfun/jee/)

Project TestFun's goal is to eliminate the common excuse for lack of good unit tests: "testing was too complicated".

TestFun-JEE is mixing existing libraries with our own goodies to deliver a robust but simple JEE junit experience when testing your DAOs (JPA), SLSBs, REST servers (JAX-RS) and more. 

### Features:
* **Injection of EJBs** (stateless sessions, singletons) and resources (JPA's entity manager, JDBC data-source, session-context) directly into JUnit test classes.
* **Injection of [Mockito](http://code.google.com/p/mockito/) mocks** into EJBs.
* **Transparent JDBC and JPA setup** - all you need is a persistence.xml file.
* **JAX-RS server testing** with EJB and mock injection.
* Simple transaction management.

### Release Notes
#### 0.10
1. Replaced the hardcoded 9095 port the JaxRsServer used for binding with "0" which means "get the next available port" - this allows the usage of multiple JaxRsServer instances concurrently towards concurrent tests.
2. Upgraded many libraries to latest version (its about time...).

#### 0.11
1. Enable finding the port the server that bound to.
2. Upgraded dependencies.

#### 0.12
1. Upgraded dependencies.
2. Enable registering @Provider classes.

#### 0.14
1. Fixed a bug in classpath scanning that failed to run on mac OS because classpath separtor is different then the one in windows.

#### 0.15
1. Added support for HTTP forms.
2. Added support for basic authentication.

#### 0.16
1. Fixed basic auth so it can handle special characters.

#### 0.17
1. Added injector for javax.inject.Inject annotation (Frank Seidinger)

#### 1.0
1. Upgrade to Java 8
2. Upgrade to Maven 3
3. Upgrade all dependencies

Usage
-----
### Getting started
#### Adding TestFun-JEE to your Maven project
Using TestFun-JEE requires adding the following dependency to your POM:
```XML
<dependency>
    <groupId>org.testfun</groupId>
    <artifactId>jee</artifactId>
    <version>1.0</version>
    <scope>test</scope>
</dependency>
```
*Project is distributed via https://oss.sonatype.org/content/groups/public/org/testfun/jee/1.0/*
#### Configuring JPA
JPA support is configured via the `src/test/resources/META-INF/persistence.xml`. 
This file defines the JDBC driver to be used as well as the "classes" folder containing the entities.
```xml
<persistence xmlns="http://java.sun.com/xml/ns/persistence"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://java.sun.com/xml/ns/persistence http://java.sun.com/xml/ns/persistence/persistence_1_0.xsd"
             version="1.0">

    <persistence-unit name="tests">

        <jar-file>../war/target/classes</jar-file>

        <properties>
            <property name="hibernate.connection.url" value="jdbc:mysql://localhost:3306?user=root&amp;password=******"/>

            <property name="hibernate.dialect" value="org.hibernate.dialect.MySQL5InnoDBDialect"/>

            <property name="hibernate.hbm2ddl.auto" value="create"/>
        </properties>
    </persistence-unit>

</persistence>
```
This will configure a MySQL driver to be used by JPA or JDBC as well as the path where Hibernate will be looking for entity classes.
**Note** that this example uses the `hbm2ddl.auto=create` settings, however it is possible (and much faster) to create the schema during Maven build - TestFun-JEE will always rollback all changes to DB done during the tests (except for "implicit commits" caused by DDL commands).
#### Injecting EJB which uses a Mock object
In the following example we test a "facade" EJB which is using a "DAO" EJB which is accessing the DB. This test is mocking the DAO:
```java
@Data @AllArgsConstructor
@Table(catalog = "tmp", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
@Entity
public class SomeEntity {

    @Id
    @GeneratedValue
    private int id;

    @Length.List({
            @Length(min = 4, message = "The name must be at least 4 characters"),
            @Length(max = 20, message = "The name must be less than 20 characters")
    })
    private String name;

    private String vcdApiAddress;

}
```
```java
@Local
public interface Facade {

    SomeEntity getFirstEntity();
}
```
```java
@Stateless
public class FacadeImpl implements Facade {

    @EJB
    private SomeDao dao;

    @Override
    public SomeEntity getFirstEntity() {
        List<SomeEntity> entities = dao.getAll();
        return entities.size() > 0 ? entities.get(0) : null;
    }
}
```
```java
@RunWith(EjbWithMockitoRunner.class)
public class GettingStartedTest {

    @EJB
    private Facade facade;

    @Mock
    private SomeDao dao;

    @Test
    public void notEntities() {
        when(dao.getAll()).thenReturn(Collections.<SomeEntity>emptyList());
        assertNull(facade.getFirstEntity());
    }

    @Test
    public void multipleEntities() {
        when(dao.getAll()).thenReturn(Arrays.asList(new SomeEntity("kuki", "puki")));
        assertEquals(new SomeEntity("kuki", "puki"), facade.getFirstEntity());
    }
}
```
**Note** that by mocking the `private SomeDao dao` member variable, any EJB asking for `SomeDao` to be injected will receive the same mock. 

### Testing EJBs which are using JPA and JDBC
As demonstrated above, all that is needed for testing an EJB (stateless/singleton/etc session-bean) which may be using JPA, JDBC and other EJBs is:

1. Replace the JUnit test runner by annotating the test class with `RunWith(EjbWithMockitoRunner.class)`.
2. Define a member variable using the interface of the EJB to be tested. Annotate this member with `@EJB`.
3. Mocking of dependencies of the EJB being tested is done by adding member variables to the tests whit the dependency's interface which are annotated with `@Mock`. Any reference in the test and in other EJBs to the mocked EJB will point to the mock object.

The following example demonstrates how to test your DAO without the hassle of configuring a JPA `EntityManager` - this example is inserting rows to the DB using an EJB which is uses JPA. The changes are rolled back after each test and the table needs to be created in advance or using the `hibernate.hbm2ddl.auto=true` configuration:
```java
@Local
public interface SomeDao {
    SomeEntity save(SomeEntity t);
    List<SomeEntity> getAll();
}
```
```java
@Stateless
public class SomeDaoImpl implements SomeDao {

    @PersistenceContext(unitName = "TestFun")
    private EntityManager entityManager;

    @Override
    public SomeEntity save(SomeEntity t) {
        if (t.getId() == 0) {
            entityManager.persist(t);
        } else {
            entityManager.merge(t);
        }
        return t;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<SomeEntity> getAll() {
        Query query = entityManager.createQuery("FROM SomeEntity AS be");
        return query.getResultList();
    }

}
```
```java
@RunWith(EjbWithMockitoRunner.class)
public class DaoTest {

    @EJB
    private SomeDao dao;

    @Test
    public void saveOne() {
        dao.save(new SomeEntity(0, "1. one", "s"));
        List<SomeEntity> entities = dao.getAll();
        assertEquals(1, entities.size());
    }

    @Test
    public void getMany() {
        dao.save(new SomeEntity(0, "1. one", "s"));
        dao.save(new SomeEntity(0, "2. two", "r"));
        List<SomeEntity> entities = dao.getAll();
        assertEquals(2, entities.size());
        assertThat(entities).
                onProperty("name").
                isEqualTo(Arrays.asList("1. one", "2. two"));//Note, this using org.fest.assertions.Assertions.assertThat
    }

}
```

### Bean validation / Hibernate Validator
To those lucky enough to use [Hibernate Validator](https://docs.jboss.org/hibernate/stable/validator/reference/en-US/html_single/), TestFun-JEE allows you to easily assert these validations are working (after all, if such annotation is accidentally deleted, the compiler will not complain).

Simply set your failure message expectation before calling the EJB, and don't forget to commit your transcation as that's when the validation is done:
```java
@RunWith(EjbWithMockitoRunner.class)
public class JpaValidationTest {

    @Rule
    public ExpectedConstraintViolation violationThrown = ExpectedConstraintViolation.none();

    @EJB
    private SomeDao someDao;

    @PersistenceContext(unitName = "TestFun")
    private EntityManager entityManager;

    @Test
    public void validName() {
        someDao.save(new SomeEntity(0, "Valid", null));
        assertEquals("Valid", someDao.getAll().get(0).getName());
    }

    @Test
    public void nameTooShort() {
        violationThrown.expectViolation("The name must be at least 4 characters");
        someDao.save(new SomeEntity(0, "srt", null));
        entityManager.getTransaction().commit();
    }

    @Test
    public void nameTooLong() {
        violationThrown.expectViolation("The name must be less than 20 characters");
        someDao.save(new SomeEntity(0, "This name should be longer than 20 characters", null));
        entityManager.getTransaction().commit();
    }
}
```

### Mocking SessionContext
If your EJBs are using the `SessionContext`, with TestFun-JEE mocking the context becomes very easy:
```java
@Local
public interface UserEjb {
    String getCurrentUser();
}
```
```java
@Stateless
public class UserEjbImpl implements UserEjb{

    @Resource
    private SessionContext sessionContext;

    @Override
    public String getCurrentUser() {
        return sessionContext.getCallerPrincipal().getName();
    }
}
```
```java
@RunWith(EjbWithMockitoRunner.class)
public class MockSessionContextTest {

    @Mock
    private SessionContext sessionContext;

    @EJB
    private UserEjb userEjb;

    @Test
    public void testSessionContextMock() {
        when(sessionContext.getCallerPrincipal()).thenReturn(new Principal() {
            @Override
            public String getName() {
                return "kuki";
            }
        });

        assertEquals("kuki", userEjb.getCurrentUser());
    }

}
```

### Testing JAX-RS resources
TestFun-JEE leverages RESTeasy's testing framework to enable very simple container-less tests.
Your resource classes are loaded into a very lightweight JAX-RS server (based on Undertow and RESTeasy) which is running in the same JVM as the test itself.

The server is configured and accessed using the JaxRsServer junit rule which is initialized with a list of resource classes to be scanned and deployed.

Dependencies injected into the resource class will be mocked if a member variable be added to the test which is annotated with `@Mock`.

Note how simple REST requests are being built and how esay the results are asserted.
```java
@RunWith(EjbWithMockitoRunner.class)
public class JaxRsExampleTest {

    @Mock
    private SomeDao someDao;

    @Rule
    public JaxRsServer jaxRsServer = JaxRsServer.forResources(ExampleResource.class);

    @Test
    public void get() throws Exception {
        JSONAssert.assertEquals(
                "{\"restData\":{\"key\":1,\"data\":\"Got 1\"}}",
                jaxRsServer.jsonRequest("/example/data/1").get(),
                JSONCompareMode.LENIENT
        );
    }

    @Test
    public void notFound() {
        assertEquals(
                "Data with ID 0 wasn't found",
                jaxRsServer.jsonRequest("/example/data/0").expectStatus(Response.Status.NOT_FOUND).get()
        );
    }

    @Test
    public void getAll() {
        with(jaxRsServer.jsonRequest("/example/data").queryParam("min", 2).queryParam("max", 6).get())
                .assertThat("$[*].restData.key", contains(2, 3, 4, 5));
    }

    @Test
    public void create() {
        jaxRsServer
                .jsonRequest("/example/data")
                .body(new RestData(12, "data..."))
                .expectStatus(Response.Status.CREATED)
                .expectLocation("/example/data/12")
                .post();
    }

    @Test
    public void withMock() {
        when(someDao.getAll()).thenReturn(Arrays.<SomeEntity>asList(new SomeEntity(0, "n1", "a1"), new SomeEntity(0, "n2", "a2")));

        assertEquals("n1", jaxRsServer.jsonRequest("/example/use_ejb").header("index", 0).get());
        assertEquals("n2", jaxRsServer.jsonRequest("/example/use_ejb").header("index", 1).get());
    }
}
```
```java
@Path("/example")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ExampleResource {

    @EJB
    private SomeDao someDao;

    @GET
    @Path("/data/{id}")
    public Response get(@PathParam("id") int id) {
        if (id <= 0) return Response.status(Response.Status.NOT_FOUND).entity("Data with ID " + id + " wasn't found").build();
        else return Response.ok(new RestData(id, "Got " + id)).build();
    }

    @GET
    @Path("/data")
    public List<RestData> getAll(@QueryParam("min") Integer minParam, @QueryParam("max") Integer maxParam) {
        int min = minParam == null ? 0 : minParam;
        int max = maxParam == null ? 10 : maxParam;

        List<RestData> data = new LinkedList<>();
        for (int i = min; i < max; ++i) {
            data.add(new RestData(i, "Got " + i));
        }

        return data;
    }

    @POST
    @Path("/data")
    public Response create(RestData data) {
        return Response.created(UriBuilder.fromPath("/example/data/").path(String.valueOf(data.getKey())).build()).build();
    }

    @GET
    @Path("/use_ejb")
    public String getEntityName(@HeaderParam("index") int index) {
        return someDao.getAll().get(index).getName();
    }

}
```
```java
@Data
@AllArgsConstructor
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class RestData {

    private int key;

    @NotNull
    private String data;

}
```

#### Application forms support
If a resource is expecting data from an "application/x-www-form-urlencoded" message, such as in:
```java
@POST
@Path("/form")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
public Response postParam(@FormParam("p1") String p1, @FormParam("p2") String p2)
{
    return Response.status(Status.OK).entity(p1 + "-" + p2).build();
}
```
It can be tested as:
```java
@Test
public void postWithForm() throws Exception {
    String response = jaxRsServer.
            formRequest("/example/form").
            withFormParam("p1", "ABCD").
            withFormParam("p2", "12345").
            expectStatus(Response.Status.OK).
            post();

    assertEquals("ABCD-12345", response);
}
```

#### Using SecurityContext
Testfun also supports the usage of ```@Context SecurityContext sc``` in resource methods.
To use it, your test-code must add basic-authentication to the HTTP request, which testfun's security-domain will always accept as valid credentials and make the SecurityContext return a ```Principal``` with the username specified in the request.
For example, the following resource method retrieves the user's name from the ```SecurityContext``` and return it as the response:
```java
@GET
@Path("/user_from_security_context")
public String getUserFromSecurityContext(@Context SecurityContext sc) {
    return sc.getUserPrincipal().getName();
}
```
The test will authenticate as follows:
```java
String response = jaxRsServer.
        jsonRequest("/example/user_from_security_context").
        basicAuth("kuki", "puki").
        expectStatus(Response.Status.OK).
        get();
```

Special thanks to...
--------------------
* [Mockito](http://code.google.com/p/mockito/) for its super cool mocking framework.
* [Junit](http://junit.org/) for setting the goal.
* [RESTEasy](http://www.jboss.org/resteasy) for its sleek JAX-RS implementation and powerful testing infrastructure (based on Undertow).
* [JsonPath](http://code.google.com/p/json-path/) and [JSONassert](http://jsonassert.skyscreamer.org/) for the awesome JSON parsing and asserting tools.

Advanced settings
-----------------
* By default, when testfun-JEE is scanning the classpath looking for EJBs, it'll skip JARS. Set the `org.testfun.jee.enable_jar_scanning` system property in order to force JAR scanning.
