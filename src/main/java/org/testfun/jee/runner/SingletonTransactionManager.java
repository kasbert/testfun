package org.testfun.jee.runner;

import javax.transaction.TransactionManager;

import org.testfun.jee.runner.inject.MockInitialContextFactory;
import org.testfun.jee.runner.inject.MockInitialContextFactory.MockContext;
import org.testfun.jee.runner.inject.MockTransactionManager;

public class SingletonTransactionManager {

    public static final String JNDI_NAME = "java:/TransactionManager";

    public static TransactionManager getInstance() {
        return getTransactionManager();
    }

    private SingletonTransactionManager() {
    }

    private synchronized static TransactionManager getTransactionManager() {
        MockContext context = MockInitialContextFactory.getMockContext();
        if (context.contains(JNDI_NAME)) {
            return (TransactionManager) context.get(JNDI_NAME);
        }
        TransactionManager transactionManager = createTransactionManager();
        context.rebind(JNDI_NAME, transactionManager);
        return transactionManager;
    }

    private static TransactionManager createTransactionManager() {
        return new MockTransactionManager();
    }
}
