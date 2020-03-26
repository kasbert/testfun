package org.testfun.jee.runner;

import org.hibernate.cfg.AvailableSettings;
import org.testfun.jee.runner.inject.MockInitialContextFactory;
import org.testfun.jee.runner.inject.MockInitialContextFactory.MockContext;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

public class SingletonEntityManager {

    public static final String JNDI_NAME = "java:/EntityManager";

    public static EntityManager getInstance() {
        return getEntityManager();
    }

    private SingletonEntityManager() {
    }

    private synchronized static EntityManager getEntityManager() {
        MockContext context = MockInitialContextFactory.getMockContext();
        if (context.contains(JNDI_NAME)) {
            return (EntityManager) context.get(JNDI_NAME);
        }
        EntityManager entityManager = createEntityManager();
        context.rebind(JNDI_NAME, entityManager);
        return entityManager;
    }
    
    private static EntityManager createEntityManager() {
        Map<String, DataSource> config = new HashMap<>();
        config.put(AvailableSettings.DATASOURCE, SingletonDataSource.getDataSource());

        EntityManagerFactory emf = Persistence.createEntityManagerFactory(PersistenceXml.getInstnace().getPersistenceUnitName(), config);
        
        EntityManager entityManager = emf.createEntityManager();
        return entityManager;
    }
    
}
