package com.rtm.mq.tool.output;

/**
 * Represents the state of an atomic output transaction.
 *
 * <p>A transaction has exactly two terminal states as per T-307:</p>
 * <ul>
 *   <li>COMMITTED - all outputs written successfully</li>
 *   <li>ROLLED_BACK - all temporary outputs removed, no changes to target</li>
 * </ul>
 */
public enum TransactionState {
    /**
     * Transaction is open and accepting outputs.
     */
    PENDING,

    /**
     * Transaction has been successfully committed.
     * All outputs are in the target directory.
     * This is a terminal state.
     */
    COMMITTED,

    /**
     * Transaction has been rolled back.
     * No outputs were written to the target directory.
     * This is a terminal state.
     */
    ROLLED_BACK
}
