package org.testfun.jee;

import javax.ejb.ApplicationException;

@ApplicationException(rollback = true)
public class RollbackAppException extends RuntimeException {
    private static final long serialVersionUID = 1L;
}
