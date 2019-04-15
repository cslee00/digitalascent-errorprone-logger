package com.digitalascent.errorprone.flogger;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

class TestSpecs {
    private final Supplier<TestFixtures.TestSourceBuilder> testSupplier;
    private final Supplier<TestFixtures.TestSourceBuilder> expectedSupplier;

    private final List<TestSpec> testSpecs = new ArrayList<>();

    TestSpecs(Supplier<TestFixtures.TestSourceBuilder> testSupplier, Supplier<TestFixtures.TestSourceBuilder> expectedSupplier) {
        this.testSupplier = testSupplier;
        this.expectedSupplier = expectedSupplier;
    }

    void add(LogLevel logLevel, String name, TestFixtures.MethodBodyCallback test, TestFixtures.MethodBodyCallback expected) {
        String testCode = testSupplier.get()
                .code(test)
                .build();
        String expectedCode = expectedSupplier.get()
                .code(expected)
                .build();

        testSpecs.add(new TestSpec(logLevel, name, testCode, expectedCode));
    }

    List<TestSpec> testSpecs() {
        return ImmutableList.copyOf(testSpecs);
    }
}
