package com.carpenter.cross_jvm_reflection.shared.rmi;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerStub extends Remote {
    /**
     * Gets the uid of the specified field, it's value is implementation specific.
     *
     * @return If the field has a null value, 0, otherwise a long with the lower 32 bits being a uid for the resolved object.
     */
    long uid(String name) throws RemoteException;

    /**
     * Gets the uid of the specified field, it's value is implementation specific.
     *
     * @return If the field has a null value, 0, otherwise a long with the upper 32 bits containing the value of owner_uid and the lower 32 bits being an implementation specific uid for the resolved object.
     */
    long uid(String name, long owner_uid) throws RemoteException;

    /**
     * Gets the length of the array with the given uid.
     */
    int length(long array_uid) throws RemoteException;

    /**
     * Gets the uid of the object at a given index in the array.
     */
    long index(int index, long uid) throws RemoteException;

    /**
     * Gets the elements of the given array without null values (cleaned)
     */
    long[] cleaned_array(long uid) throws RemoteException;

    /**
     * Gets the elements of the given array without null values (cleaned)
     */
    long[] cleaned_array(String name) throws RemoteException;

    /**
     * Gets the elements of the given array without null values (cleaned)
     */
    long[] cleaned_array(String name, long owner_uid) throws RemoteException;

    /**
     * Gets the elements of the given array with null values (not cleaned, "dirty")
     */
    long[] array(long uid) throws RemoteException;

    /**
     * Gets the elements of the given array with null values (not cleaned, "dirty")
     */
    long[] array(String name) throws RemoteException;

    /**
     * Gets the elements of the given array with null values (not cleaned, "dirty")
     */
    long[] array(String name, long owner_uid) throws RemoteException;

    /**
     * Gets an array of the uid's of the keys of the given map.
     */
    long[] map_keys(long map_uid) throws RemoteException;

    /**
     * Gets the uid of the object with the key in the map (key is resolved from key_uid).
     */
    long map_value(long key_uid, long map_uid) throws RemoteException;

    /**
     * Gets the uid of the object with the given key in the map.
     * It accepts an Object instead of a Serializable because value(long, long) uses it with non-serializable objects.
     */
    long map_value(Object key, long map_uid) throws RemoteException;

    /**
     * Checks whether the given uid can be resolved to an object. Resolution my fail if the jvm has garbage collected the object.
     */
    boolean validate(long uid) throws RemoteException;

    /**
     * Checks whether or not the object provided by the given uid is an instance of the class with name 'type_name'
     */
    boolean instance(String type_name, long uid) throws RemoteException;

    /**
     * Gets the name of the class of the object with the given uid.
     *
     * @param uid An object's uid
     * @return The name of the object's class, ex: new Object().getClass().getName()
     */
    String type(long uid) throws RemoteException;

    /**
     * Gets the object with the given uid as an instance of Serializable
     */
    Serializable serialize(long uid) throws RemoteException;

    /**
     * Gets the object with the given name as an instance of Serializable
     */
    Serializable serialize(String name) throws RemoteException;

    /**
     * Gets the object with the given name and owner as an instance of Serializable
     */
    Serializable serialize(String name, long owner_uid) throws RemoteException;

    /**
     * Gets the object with the given uid as a byte
     */
    byte int8(long uid) throws RemoteException;

    /**
     * Gets the object with the given name as a byte
     */
    byte int8(String name) throws RemoteException;

    /**
     * Gets the object with the given name and owner as a byte
     */
    byte int8(String name, long owner_uid) throws RemoteException;

    short int16(long uid) throws RemoteException;

    short int16(String name) throws RemoteException;

    short int16(String name, long owner_uid) throws RemoteException;

    int int32(long uid) throws RemoteException;

    int int32(String name) throws RemoteException;

    int int32(String name, long owner_uid) throws RemoteException;

    long int64(long uid) throws RemoteException;

    long int64(String name) throws RemoteException;

    long int64(String name, long owner_uid) throws RemoteException;

    float fp32(long uid) throws RemoteException;

    float fp32(String name) throws RemoteException;

    float fp32(String name, long owner_uid) throws RemoteException;

    double fp64(long uid) throws RemoteException;

    double fp64(String name) throws RemoteException;

    double fp64(String name, long owner_uid) throws RemoteException;

    boolean bool(long uid) throws RemoteException;

    boolean bool(String name) throws RemoteException;

    boolean bool(String name, long owner_uid) throws RemoteException;

    /**
     * Gets the object with the given uid as a char (an unsigned short)
     */
    char uint16(long uid) throws RemoteException;

    /**
     * Gets the object with the given name as a char (an unsigned short)
     */
    char uint16(String name) throws RemoteException;

    /**
     * Gets the object with the given name and owner as a char (an unsigned short)
     */
    char uint16(String name, long owner_uid) throws RemoteException;
}
