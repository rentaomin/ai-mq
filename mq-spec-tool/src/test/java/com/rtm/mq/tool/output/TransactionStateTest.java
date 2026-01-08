package com.rtm.mq.tool.output;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TransactionState}.
 */
class TransactionStateTest {

    @Test
    void testEnumValues() {
        TransactionState[] values = TransactionState.values();
        assertEquals(3, values.length);
    }

    @Test
    void testPendingState() {
        assertEquals(TransactionState.PENDING, TransactionState.valueOf("PENDING"));
    }

    @Test
    void testCommittedState() {
        assertEquals(TransactionState.COMMITTED, TransactionState.valueOf("COMMITTED"));
    }

    @Test
    void testRolledBackState() {
        assertEquals(TransactionState.ROLLED_BACK, TransactionState.valueOf("ROLLED_BACK"));
    }
}
