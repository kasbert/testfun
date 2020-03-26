package org.testfun.jee.runner.inject;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * MockTransactionManager
 */
public class MockTransactionManager implements TransactionManager, Synchronization {

    private static final Logger logger = LogManager.getLogger(MockTransactionManager.class);

    private static ThreadLocal<LinkedList<MockTransaction>> txStack = new ThreadLocal<LinkedList<MockTransaction>>();
    protected int transactionTimeout;

    public MockTransactionManager() {
    }

    @Override
    public void begin() {
        MockTransaction tx = getFirst();
        logger.debug("begin " + tx);
        if (tx != null && !tx.isActive()) {
            logger.debug("discard old tx");
            removeFirst();
        }
        tx = new MockTransaction();
        tx.registerSynchronization(this);
        addFirst(tx);
    }

    @Override
    public void commit() throws IllegalStateException, SystemException {
        MockTransaction tx = getFirst();
        logger.debug("commit " + tx);
        if (tx == null) {
            throw new IllegalStateException("No transaction");
        }
        tx.commit();
    }

    @Override
    public void rollback() throws IllegalStateException, SystemException {
        MockTransaction tx = getFirst();
        logger.debug("rollback " + tx);
        if (tx == null) {
            throw new IllegalStateException("No transaction");
        }
        tx.rollback();
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException {
        MockTransaction tx = getFirst();
        logger.debug("setRollbackOnly " + tx);
        if (tx == null) {
            throw new IllegalStateException("No transaction");
        }
        tx.setRollbackOnly();
    }

    @Override
    public Transaction suspend() {
        MockTransaction tx = removeFirst();
        logger.debug("suspend " + tx);
        return tx;
    }

    @Override
    public void resume(Transaction arg0) throws IllegalStateException {
        MockTransaction tx = getFirst();
        logger.debug("resume " + tx);
        if (tx != null) {
            throw new IllegalStateException("Already in transaction");
        }
        addFirst((MockTransaction) arg0);
    }

    @Override
    public int getStatus() {
        MockTransaction tx = getFirst();
        String str = "" + tx;
        if (!"0".equals(str)) {
            logger.trace("getStatus " + tx);
        }
        if (tx == null) {
            return Status.STATUS_NO_TRANSACTION;
        }
        return tx.getStatus();
    }

    @Override
    public Transaction getTransaction() {
        MockTransaction tx = getFirst();
        String str = "" + tx;
        if (!"0".equals(str)) {
            logger.trace("getTransaction " + tx);
        }
        return tx;
    }

    @Override
    public void setTransactionTimeout(int arg0) {
        logger.debug("setTransactionTimeout " + arg0);
        transactionTimeout = arg0;
    }

    @Override
    public void beforeCompletion() {
        // Called from MockTransaction
    }

    @Override
    public void afterCompletion(int status) {
        // Called from MockTransaction
        removeFirst();
    }
    
    protected MockTransaction getFirst() {
        if (txStack.get() == null) {
            return null;
        }
        if (txStack.get().isEmpty()) {
            return null;
        }
        return txStack.get().getFirst();
    }

    protected MockTransaction removeFirst() {
        if (txStack.get() == null) {
            return null;
        }
        if (txStack.get().isEmpty()) {
            return null;
        }
        return txStack.get().removeFirst();
    }

    protected void addFirst(MockTransaction tx) {
        if (txStack.get() == null) {
            txStack.set(new LinkedList<MockTransaction>());
        }
        txStack.get().addFirst(tx);
    }

    public static class MockXid implements Xid {
        private final byte[] bytes;
        static long counter;

        public MockXid() {
            bytes = longToBytes(counter++);
        }

        public static byte[] longToBytes(long x) {
            ByteBuffer buffer = ByteBuffer.allocate(8);
            buffer.putLong(0, x);
            return buffer.array();
        }

        public static long bytesToLong(byte[] bytes) {
            ByteBuffer buffer = ByteBuffer.allocate(8);
            buffer.put(bytes, 0, bytes.length);
            buffer.flip();// need flip
            return buffer.getLong();
        }

        @Override
        public byte[] getBranchQualifier() {
            // TODO Auto-generated method stub
            return bytes;
        }

        @Override
        public int getFormatId() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public byte[] getGlobalTransactionId() {
            // TODO Auto-generated method stub
            return bytes;
        }

        @Override
        public String toString() {
            return String.valueOf(bytesToLong(bytes));
        }
    }

    public static class MockTransaction implements Transaction {

        private int status = Status.STATUS_ACTIVE;
        private final List<Synchronization> synchronizations = new LinkedList<Synchronization>();
        private final List<XAResource> resources = new LinkedList<XAResource>();
        private final Xid xid = new MockXid();

        public MockTransaction() {
            logger.debug(xid + " MockTransaction.init");
        }

        @Override
        public void commit() throws SystemException {
            logger.debug(xid + " MockTransaction.commit");
            if (status == Status.STATUS_MARKED_ROLLBACK) {
                throw new IllegalStateException("Rollback only");
            }
            endResources();
            status = Status.STATUS_PREPARING;
            prepareResources();
            status = Status.STATUS_PREPARED;

            status = Status.STATUS_COMMITTING;
            beforeCompletion();
            commitResources();
            status = Status.STATUS_COMMITTED;
            afterCompletion();
        }

        @Override
        public boolean delistResource(XAResource arg0, int arg1) {
            logger.debug(xid + " MockTransaction.delistResource");
            resources.remove(arg0);
            return true;
        }

        @Override
        public boolean enlistResource(XAResource arg0) {
            logger.debug(xid + " MockTransaction.enlistResource");
            resources.add(arg0);
            try {
                arg0.start(xid, XAResource.TMNOFLAGS);
            } catch (XAException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return true;
        }

        @Override
        public int getStatus() {
            logger.debug(xid + " MockTransaction.getStatus " + statusToString(status));
            return status;
        }

        @Override
        public void registerSynchronization(Synchronization arg0) {
            logger.debug(xid + " MockTransaction.registerSynchronization");
            synchronizations.add(arg0);
        }

        @Override
        public void rollback() throws SystemException {
            logger.debug(xid + " MockTransaction.rollback");
            endResources();
            status = Status.STATUS_ROLLING_BACK;
            beforeCompletion();
            rollbackResources();
            status = Status.STATUS_ROLLEDBACK;
            afterCompletion();
        }

        @Override
        public void setRollbackOnly() {
            logger.debug(xid + " MockTransaction.setRollbackOnly");
            status = Status.STATUS_MARKED_ROLLBACK;
        }

        private String statusToString(int status) {
            switch (status) {
            case Status.STATUS_ACTIVE:
                return "STATUS_ACTIVE";
            case Status.STATUS_MARKED_ROLLBACK:
                return "STATUS_MARKED_ROLLBACK";
            case Status.STATUS_PREPARED:
                return "STATUS_PREPARED";
            case Status.STATUS_COMMITTED:
                return "STATUS_COMMITTED";
            case Status.STATUS_ROLLEDBACK:
                return "STATUS_ROLLEDBACK";
            case Status.STATUS_UNKNOWN:
                return "STATUS_UNKNOWN";
            case Status.STATUS_NO_TRANSACTION:
                return "STATUS_NO_TRANSACTION";
            case Status.STATUS_PREPARING:
                return "STATUS_PREPARING";
            case Status.STATUS_COMMITTING:
                return "STATUS_COMMITTING";
            case Status.STATUS_ROLLING_BACK:
                return "STATUS_ROLLING_BACK";
            }
            return "UNKNOWN(" + status + ")";
        }

        /*
         * 
         */

        protected boolean isActive() {
            return !(status == Status.STATUS_COMMITTED || status == Status.STATUS_ROLLEDBACK
                    || status == Status.STATUS_UNKNOWN);
        }

        /*
         * 
         */
        protected void beforeCompletion() {
            for (Synchronization sync : synchronizations) {
                sync.beforeCompletion();
            }
        }

        protected void afterCompletion() {
            for (Synchronization sync : synchronizations) {
                sync.afterCompletion(status);
            }
        }

        /*
         * 
         */
        protected void startResources() throws SystemException {
            for (XAResource resource : resources) {
                try {
                    resource.start(xid, XAResource.TMNOFLAGS);
                } catch (XAException e) {
                    logger.error("error", e);
                    throw new SystemException("resource start failed: " + e);
                }
            }
        }

        protected void commitResources() throws SystemException {
            for (XAResource resource : resources) {
                try {
                    resource.commit(xid, true);
                } catch (XAException e) {
                    logger.error("error", e);
                    throw new SystemException("resource commit failed: " + e);
                }
            }
        }

        protected void rollbackResources() throws SystemException {
            for (XAResource resource : resources) {
                try {
                    resource.rollback(xid);
                } catch (XAException e) {
                    logger.error("error", e);
                    throw new SystemException("resource rollback failed: " + e);
                }
            }
        }

        protected void prepareResources() throws SystemException {
            for (XAResource resource : resources) {
                try {
                    resource.prepare(xid);
                } catch (XAException e) {
                    logger.error("error", e);
                    throw new SystemException("resource prepare failed: " + e);
                }
            }
        }

        protected void endResources() throws SystemException {
            for (XAResource resource : resources) {
                try {
                    resource.end(xid, XAResource.TMSUCCESS);
                } catch (XAException e) {
                    logger.error("error", e);
                    throw new SystemException("resource end failed: " + e);
                }
            }
        }

        @Override
        public String toString() {
            return xid.toString();
        }
    }
}
