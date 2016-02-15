package com.carpenter.cross_jvm_reflection.server.rmi.impl;

public class DefaultServerImplUtil {
    /**
     * Gets the owner component of the uid.
     */
    public long getOwnerUID(long uid) {
        return uid >>> 32;
    }

    /**
     * Gets the child component of the uid.
     * This is necessary for comparison within circular lists and other data structures.
     */
    public long getChildUID(long uid) {
        return uid & 0xFFFFFFFFL;
    }
}
