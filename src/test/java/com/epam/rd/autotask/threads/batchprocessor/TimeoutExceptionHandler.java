package com.epam.rd.autotask.threads.batchprocessor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import java.lang.reflect.Method;
import java.time.Duration;

public class TimeoutExceptionHandler implements InvocationInterceptor {
    @Override
    public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) {
        Assertions.assertTimeoutPreemptively(Duration.ofSeconds(5), invocation::proceed,
                "Probably getResult() doesn't work properly");
    }
}