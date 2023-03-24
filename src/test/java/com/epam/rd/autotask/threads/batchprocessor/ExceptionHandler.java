package com.epam.rd.autotask.threads.batchprocessor;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

import static org.junit.jupiter.api.Assertions.fail;

public class ExceptionHandler implements TestExecutionExceptionHandler {

    public static final String FAIL_MSG = "You must properly place messages to the destination queue in transformer";

    @Override
    public void handleTestExecutionException(ExtensionContext ctx, Throwable throwable) throws Throwable {
        ctx.publishReportEntry(FAIL_MSG,
                throwable.getMessage());
        fail(FAIL_MSG, throwable);
    }
}