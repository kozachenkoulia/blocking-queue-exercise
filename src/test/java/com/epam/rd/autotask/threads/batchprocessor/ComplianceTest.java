package com.epam.rd.autotask.threads.batchprocessor;

import org.junit.jupiter.api.Test;
import spoon.Launcher;
import spoon.SpoonAPI;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtSynchronized;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtModifiable;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.Filter;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ComplianceTest {

    static CtModel ctModel = init();

    static CtModel init() {
        SpoonAPI spoon = new Launcher();
        spoon.addInputResource("src/main/java/");
        return spoon.buildModel();
    }

    @Test
    void testSynchronizationCompliance() {
        List<Object> syncBlocks = ctModel
                .filterChildren((Filter<CtSynchronized>) el -> true)
                .list();
        assertTrue(syncBlocks.isEmpty(), "You must not use synchronization, but was:\n" + syncBlocks);

        List<Object> syncModifiers = ctModel.filterChildren((Filter<CtMethod<?>>) CtModifiable::isSynchronized).list();
        assertTrue(syncModifiers.isEmpty(), "You must not use synchronization, but was:\n" + syncModifiers);

        List<CtTypeReference<?>> refs = ctModel.filterChildren((Filter<CtTypeReference<?>>) el -> true).list();
        List<CtTypeReference<?>> locks = refs.stream().flatMap(r -> r.getSuperInterfaces().stream())
                .filter(r -> r.getQualifiedName().startsWith("java.util.concurrent") &&
                        !r.getSimpleName().equals(BlockingQueue.class.getSimpleName()))
                .distinct()
                .toList();
        assertTrue(locks.isEmpty(), "You must not use synchronization, but was:\n" + locks);
    }
}
