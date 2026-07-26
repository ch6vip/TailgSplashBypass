package com.tailg.lsposed.adblock;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ReflectionAccessTest {
    @Test
    public void setField_updatesInheritedPrimitiveField() throws ReflectiveOperationException {
        Child child = new Child();

        ReflectionAccess.setField(child, "value", 42);

        assertEquals(42, ReflectionAccess.getField(child, "value"));
    }

    private static class Parent {
        private int value;
    }

    private static final class Child extends Parent {
    }
}
