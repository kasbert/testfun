package org.testfun.jee.runner.inject;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.ejb.ApplicationException;
import javax.persistence.EntityTransaction;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.testfun.jee.runner.SingletonEntityManager;
import org.testfun.jee.runner.SingletonTransactionManager;

public class TransactionUtils {
    private static final Logger logger = LogManager.getLogger(TransactionUtils.class);

    public static Object wrapEjbWithTransaction(Object impl) {
        Assert.assertNotNull("EJB Implementation is null", impl);
        Class<?>[] interfaces = impl.getClass().getInterfaces();

        return Proxy.newProxyInstance(TransactionUtils.class.getClassLoader(), interfaces,
                new TransactionalMethodWrapper(impl));
    }

    public static boolean beginTransaction() {
        EntityTransaction tx = getTransaction();
        if (tx == null) {
            try {
                Transaction emtx = getJTATransaction();
                if (emtx != null) {
                    return false;
                }
                SingletonTransactionManager.getInstance().begin();
                SingletonEntityManager.getInstance().joinTransaction();
            } catch (Exception e) {
                logger.warn("begin transaction failed", e);
            }
            return true;
        }
        if (!tx.isActive()) {
            tx.begin();
            return true;
        }

        return false;
    }

    public static void rollbackTransaction() {
        EntityTransaction tx = getTransaction();
        if (tx == null) {
            try {
                Transaction emtx = getJTATransaction();
                if (emtx != null) {
                    emtx.setRollbackOnly();
                }
            } catch (Exception e) {
                logger.warn("rollback transaction failed", e);
            }
            return;
        }
        if (tx.isActive() && !tx.getRollbackOnly()) {
            // only flag the transaction for rollback - actual rollback will happen when the
            // method starting the transaction is done
            tx.setRollbackOnly();
        }
    }

    public static void endTransaction(boolean newTransaction) {
        EntityTransaction tx = getTransaction();
        if (tx == null) {
            try {
                Transaction emtx = getJTATransaction();
                if (emtx != null && newTransaction) {
                    if (emtx.getStatus() == javax.transaction.Status.STATUS_MARKED_ROLLBACK) {
                        emtx.rollback();
                    } else {
                        emtx.commit();
                    }
                }
            } catch (Exception e) {
                logger.warn("end transaction failed", e);
            }
            return;
        }
        if (tx.isActive() && newTransaction) {
            if (tx.getRollbackOnly()) {
                // Rollback the transaction and close the connection if roll back was requested
                // deeper in the stack and this is the method starting the transaction
                tx.rollback();
            }

            else {
                // Commit the transaction only if this is the method starting the transaction
                // and rollback wasn't requested
                tx.commit();
            }
        }
    }

    private static EntityTransaction getTransaction() {
        try {
            return SingletonEntityManager.getInstance().getTransaction();
        } catch (IllegalStateException e) {
            return null; // JTA transaction
        }
    }

    private static Transaction getJTATransaction() throws SystemException {
        return SingletonTransactionManager.getInstance().getTransaction();
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

            } catch (InvocationTargetException throwable) {
                InvocationTargetException invocationTargetException = (InvocationTargetException) throwable;
                ApplicationException applicationException = invocationTargetException.getTargetException().getClass()
                        .getAnnotation(ApplicationException.class);
                if (applicationException == null || applicationException.rollback()) {
                    rollbackTransaction();
                }
                throw throwable.getCause();

            } catch (Throwable throwable) {
                rollbackTransaction();
                throw throwable;

            } finally {
                endTransaction(newTransaction);
            }
        }
    }

}
