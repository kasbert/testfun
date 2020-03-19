package org.testfun.jee.runner.inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.testfun.jee.runner.PersistenceXml;
import org.testfun.jee.runner.SingletonEntityManager;

import javax.ejb.ApplicationException;
import javax.naming.InitialContext;
import javax.persistence.EntityTransaction;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class TransactionUtils {
    private static final Logger logger = LogManager.getLogger(TransactionUtils.class);

    public static Object wrapEjbWithTransaction(Object impl) {
        Assert.assertNotNull("EJB Implementation is null", impl);
        Class<?>[] interfaces = impl.getClass().getInterfaces();

        return Proxy.newProxyInstance(TransactionUtils.class.getClassLoader(), interfaces, new TransactionalMethodWrapper(impl));
    }

    public static boolean beginTransaction() {
        if (PersistenceXml.getInstnace().isJtaDataSource()) {
            try {
                TransactionManager tm = getJTATransactionManager();
                Transaction tx = tm.getTransaction();
                if (tx != null) {
                    return false;
                }
                tm.begin();
            } catch (Exception e) {
                logger.warn("begin transaction failed", e);
            }
            return true;
        }
        EntityTransaction tx = getTransaction();
        if (!tx.isActive()) {
            tx.begin();
            return true;
        }

        return false;
    }

    public static void rollbackTransaction() {
        if (PersistenceXml.getInstnace().isJtaDataSource()) {
            try {
                TransactionManager tm = getJTATransactionManager();
                Transaction tx = tm.getTransaction();
                if (tx != null) {
                    tx.setRollbackOnly();
                }
            } catch (Exception e) {
                logger.warn("rollback transaction failed", e);
            }
            return;
        }
        EntityTransaction tx = getTransaction();
        if (tx.isActive() && !tx.getRollbackOnly()) {
            // only flag the transaction for rollback - actual rollback will happen when the method starting the transaction is done
            tx.setRollbackOnly();
        }
    }

    public static void endTransaction(boolean newTransaction) {
        if (PersistenceXml.getInstnace().isJtaDataSource()) {
            try {
                TransactionManager tm = getJTATransactionManager();
                Transaction tx = tm.getTransaction();
                if (tx != null && newTransaction) {
                    if (tx.getStatus() == javax.transaction.Status.STATUS_MARKED_ROLLBACK) {
                        tx.rollback();
                    } else {
                        tx.commit();
                    }
                }
            } catch (Exception e) {
                logger.warn("end transaction failed", e);
            }
            return;
        }
        EntityTransaction tx = getTransaction();
        if (tx.isActive() && newTransaction) {
            if (tx.getRollbackOnly()) {
                // Rollback the transaction and close the connection if roll back was requested deeper in the stack and this is the method starting the transaction
                tx.rollback();
            }

            else {
                // Commit the transaction only if this is the method starting the transaction and rollback wasn't requested
                tx.commit();
            }
        }
    }

    private static EntityTransaction getTransaction() {
        return SingletonEntityManager.getInstance().getTransaction();
    }

    private static TransactionManager getJTATransactionManager() {
        InitialContext ic;
        try {
            ic = new InitialContext();
            TransactionManager tm = (TransactionManager) ic.lookup(MockTransactionManager.JNDI_NAME);

            if (tm == null) {
                tm = new MockTransactionManager();
                ic.bind(MockTransactionManager.JNDI_NAME, tm);
            }

            return tm;
        } catch (Exception e) {
            throw new Error("getJTATransactionManager", e);
        }
    }

    private static class TransactionalMethodWrapper implements InvocationHandler {

        private Object delegate;

        private TransactionalMethodWrapper(Object delegate) {
            this.delegate = delegate;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            boolean newTransaction = beginTransaction();

            try {
                return method.invoke(delegate, args);

            } catch (Throwable throwable) {

                if (throwable instanceof InvocationTargetException) {
                    InvocationTargetException invocationTargetException = (InvocationTargetException) throwable;

                    ApplicationException applicationException = invocationTargetException.getTargetException().getClass().getAnnotation(ApplicationException.class);

                    if (applicationException == null || applicationException.rollback()) {
                        rollbackTransaction();
                    }

                } else {
                    rollbackTransaction();
                }

                throw throwable.getCause();

            } finally {
                endTransaction(newTransaction);
            }
        }
    }

}
