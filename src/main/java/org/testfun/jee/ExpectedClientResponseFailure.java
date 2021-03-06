package org.testfun.jee;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * A JUnit rule that is used for declaring on expected REST/HTTP-client response returned from the server - should be
 * used in conjunction with the {@link JaxRsServer} rule.
 * <p>
 * To use this rule, add the following member variable to your test class:
 * <pre>{@code @Rule public ExpectedClientResponseFailure thrownClientResponseFailure = ExpectedClientResponseFailure.none();}</pre>
 * <p>
 * Setting failure expectation can be done as follows:
 * <pre>{@code thrownClientResponseFailure.expectFailureResponse(Response.Status.BAD_REQUEST, "Password was not provided");}</pre>
 * Note that it is recommended to use {@link JaxRsServer#expectFailureResponse(javax.ws.rs.core.Response.Status, java.lang.String)} instead:
 */
public class ExpectedClientResponseFailure implements MethodRule {

    private String expectedMessageSubstring;
    private Response.Status expectedResponseStatus;

    public static ExpectedClientResponseFailure none() {
        return new ExpectedClientResponseFailure();
    }

    private ExpectedClientResponseFailure() {
    }

    @Override
    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        return new ExpectedClientResponseFailureStatement(base);
    }

    /**
     * Set expectation for REST failure with a particular status code and a substring that should appear in the failure message.
     */
    public void expectFailureResponse(Response.Status expectedResponseStatus, String expectedMessageSubstring) {
        assertNotNull(expectedResponseStatus);
        this.expectedResponseStatus = expectedResponseStatus;

        assertNotNull(expectedMessageSubstring);
        this.expectedMessageSubstring = expectedMessageSubstring;
    }

    @SuppressWarnings("unchecked")
    @SafeVarargs
    public static <T extends Throwable> T getEncapsulatedException(Throwable currentException,
                                                                   Class<? extends Throwable>... exceptionClasses) {
        List<Throwable> visitedExceptions = new ArrayList<>();
        while (currentException != null) {
            if (visitedExceptions.contains(currentException)) {
                // to avoid infinite loops
                break;
            } else {
                visitedExceptions.add(currentException);
            }
            for (Class<? extends Throwable> exceptionClass : exceptionClasses) {
                if (exceptionClass.isInstance(currentException))
                    return (T) currentException;
            }
            currentException = currentException.getCause();
        }
        return null;
    }

    private class ExpectedClientResponseFailureStatement extends Statement {

        private final Statement next;

        private ExpectedClientResponseFailureStatement(Statement next) {
            this.next = next;
        }

        @Override
        public void evaluate() throws Throwable {
            try {
                next.evaluate();

            } catch (Throwable e) {
                if (expectedMessageSubstring == null) {
                    throw e; // unexpected exception
                }

                Throwable causedByClientResponseFailure = getEncapsulatedException(e, ClientErrorException.class);
                if (causedByClientResponseFailure == null) {
                    throw e; // the caught exception isn't caused by the expected one
                }

                // if there's a failure was expected and the caught, make sure the expected message matches the caught one
                ClientErrorException failure = (ClientErrorException) causedByClientResponseFailure;
                Response response = failure.getResponse();
                String actualResponseMessage = response.readEntity(String.class);

                Response.Status responseStatus = Response.Status.fromStatusCode(response.getStatus());
                boolean actualFailureMatchesExpectedOne = actualResponseMessage.contains(expectedMessageSubstring) && responseStatus == expectedResponseStatus;

                if (!actualFailureMatchesExpectedOne) {
                    StringBuilder sb = getExpectedFailureMessage("Unexpected failure:").
                            append("\n\tFound response with status ").append(response.getStatus()).
                            append(" (").append(responseStatus).append(") and body: ").append(actualResponseMessage);
                    fail(sb.toString());
                }

                // must return now so to skip the code outside of the try-catch block which is responsible for making sure that
                // the test will fail if an exception was expected but wasn't thrown
                return;

            }

            // Fail if an exception was expected but wasn't thrown
            if (expectedMessageSubstring != null) {
                fail(getExpectedFailureMessage("Expected test to fail:").toString());
            }
        }

        private StringBuilder getExpectedFailureMessage(String prefix) {
            return new StringBuilder(prefix).
                    append("\n\tExpected response with status ").append(expectedResponseStatus.getStatusCode()).
                    append(" (").append(expectedResponseStatus).append(") ").
                    append("and body containing: ").append(expectedMessageSubstring);
        }
    }
}
