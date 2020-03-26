package org.testfun.jee.runner;

import org.apache.logging.log4j.LogManager;
import org.testfun.jee.runner.inject.MockInitialContextFactory;
import org.testfun.jee.runner.inject.MockInitialContextFactory.MockContext;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SingletonDataSource {

    public static final String JNDI_NAME = "java:/DataSource";

    private SingletonDataSource () {};

    private static Connection delegateConnection;

    public static synchronized DataSource getDataSource() {
        MockContext context = MockInitialContextFactory.getMockContext();
        if (context.contains(JNDI_NAME)) {
            return (DataSource) context.get(JNDI_NAME);
        }
        DataSource dataSource = createDataSource();
        context.rebind(JNDI_NAME, dataSource);
        return dataSource;
    }

    private static DataSource createDataSource() {
        try {
            DataSource dataSource;
            if (PersistenceXml.getInstnace().isJtaDataSource()) {
                InitialContext ic = new InitialContext();
                dataSource = (DataSource) ic.lookup(PersistenceXml.getInstnace().getJtaDataSource());
                ic.close();
            } else {
                Connection connection = DriverManager.getConnection(PersistenceXml.getInstnace().getConnectionUrl());
                connection.setAutoCommit(false);
                dataSource = (DataSource) Proxy.newProxyInstance(SingletonDataSource.class.getClassLoader(),
                        new Class[] { DataSource.class }, new NotClosableDataSource(connection));
            }
            LogManager.getLogger(SingletonDataSource.class).info("Data source initialized successfully");
            return dataSource;

        } catch (Exception e) {
            LogManager.getLogger(SingletonDataSource.class).error("Data source initialization failed", e);
            throw new EjbWithMockitoRunnerException("Data source initialization failed", e);
        }
    }

    /**
     * A DataSource proxy that always return the same JDBC connection which doesn't close when "close" is called.
     * This is needed so JDBC calls will be using the same connection and transaction as JPA calls.
     * Note that the connection returned from the data-source is never closed as it is up to the entity manager to
     * close its connection
     */
    private static class NotClosableDataSource implements InvocationHandler {

        private NotClosableDataSource(Connection connection) throws SQLException {
            delegateConnection = connection;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object... args) throws Throwable {
            if ("getConnection".equals(method.getName())) {
                return getNotClosableConnection();
            }

            else if ("toString".equals(method.getName())) {
                return "NotClosableDataSource";
            }

            else {
                throw new IllegalArgumentException("Unsupported method: " + method.getName());
            }
        }

        private Object getNotClosableConnection() {
            return Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{Connection.class}, new NotClosableConnectionProxy(delegateConnection));
        }

    }

    /**
     * A JDBC Connection proxy that ignores calls to close() - used when the connection is retrieved from the entity manager.
     */
    private static class NotClosableConnectionProxy implements InvocationHandler {

        private Connection delegate;

        private NotClosableConnectionProxy(Connection delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object... args) throws Throwable {
            if ("close".equals(method.getName())) {
                return null;
            }

            else if ("toString".equals(method.getName())) {
                return "NotClosableConnectionProxy{" + delegate + "} - " + (delegate.isClosed() ? "closed" : "opened");
            }

            else {
                return method.invoke(delegate, args);
            }
        }
    }

}
