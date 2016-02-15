package com.carpenter.cross_jvm_reflection.server.rmi.impl;

import com.carpenter.cross_jvm_reflection.shared.rmi.ServerStub;

import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The default server implementation.
 * The upper 32 bits of a uid corresponds to the owner and the lower 32 bits correspond to the child.
 * Uid's are weakly linked to their objects to allow the remote jvm to garbage collect normally.
 * Resolved classes and method handles are stored in concurrent hash maps are are NOT held with weak references.
 * Classes are resolved via the provided class loader and initialize is set to false in the Class resolution.
 */
public class DefaultServerImpl extends UnicastRemoteObject implements ServerStub {
    private final ClassLoader class_loader;
    private Map<String, Class<?>> class_cache = new ConcurrentHashMap<>();
    private Map<Class<?>, String> class_name_cache = new ConcurrentHashMap<>();
    private Map<String, MethodHandle> cached_handles = new ConcurrentHashMap<>();
    private Map<Long, WeakReference<Object>> object_store = new ConcurrentHashMap<>();

    public DefaultServerImpl(ClassLoader class_loader) throws RemoteException {
        this.class_loader = class_loader;
    }

    @Override
    public long uid(String field) throws RemoteException {
        Object object = getUncheckedObject(field);
        long uid = System.identityHashCode(object);
        if (uid != 0) {
            object_store.put(uid, new WeakReference<>(object));
        }
        return uid;
    }

    @Override
    public long uid(String field, long owner_uid) throws RemoteException {
        Object object = getUncheckedObject(field, owner_uid);
        long uid = System.identityHashCode(object);
        if (uid != 0) {
            uid += (owner_uid << 32);
            object_store.put(uid, new WeakReference<>(object));
        }
        return uid;
    }

    @Override
    public int length(long array_uid) throws RemoteException {
        Object object = getUncheckedObject(array_uid);
        if (object != null) {
            return Array.getLength(object);
        }
        return 0;
    }

    @Override
    public long index(int index, long uid) throws RemoteException {
        Object object = getUncheckedObject(uid);
        if (object != null) {
            Object child = Array.get(object, index);
            long child_uid = System.identityHashCode(child);
            if (child_uid != 0) {
                child_uid += (uid << 32);
                object_store.put(child_uid, new WeakReference<>(child));
                return child_uid;
            }
        }
        return 0;
    }

    @Override
    public long[] cleaned_array(long uid) throws RemoteException {
        Object object = getUncheckedObject(uid);
        if (object != null) {
            int length = Array.getLength(object);
            List<Long> list = new ArrayList<>(length);
            for (int index = 0; index < length; ++index) {
                Object child = Array.get(object, index);
                long child_uid = System.identityHashCode(child);
                if (child_uid != 0) {
                    child_uid += (uid << 32);
                    object_store.put(child_uid, new WeakReference<>(child));
                    list.add(child_uid);
                }
            }
            length = list.size();
            if (length > 0) {
                long[] uids = new long[length];
                for (int index = 0; index < length; ++index) {
                    uids[index] = list.get(index);
                }
                return uids;
            }
        }
        return null;
    }

    @Override
    public long[] cleaned_array(String field) throws RemoteException {
        long uid = uid(field);
        if (uid != 0) {
            return cleaned_array(uid);
        }
        return null;
    }

    @Override
    public long[] cleaned_array(String field, long owner_uid) throws RemoteException {
        long uid = uid(field, owner_uid);
        if (uid != 0) {
            return cleaned_array(uid);
        }
        return null;
    }

    @Override
    public long[] array(long uid) throws RemoteException {
        Object object = getUncheckedObject(uid);
        if (object != null) {
            int length = Array.getLength(object);
            if (length > 0) {
                long[] array = new long[length];
                for (int index = 0; index < length; ++index) {
                    Object child = Array.get(object, index);
                    long child_uid = System.identityHashCode(child);
                    if (child_uid != 0) {
                        child_uid += (uid << 32);
                        object_store.put(child_uid, new WeakReference<>(child));
                    }
                    array[index] = child_uid;
                }
                return array;
            }
        }
        return null;
    }

    @Override
    public long[] array(String field) throws RemoteException {
        long uid = uid(field);
        if (uid != 0) {
            return array(uid);
        }
        return null;
    }

    @Override
    public long[] array(String field, long owner_uid) throws RemoteException {
        long uid = uid(field, owner_uid);
        if (uid != 0) {
            return array(uid);
        }
        return null;
    }

    @Override
    public long[] map_keys(long map_uid) throws RemoteException {
        Object object = getUncheckedObject(map_uid);
        if (object instanceof Map) {
            Set keys = ((Map) object).keySet();
            long[] uids = new long[keys.size()];
            Iterator iterator = keys.iterator();
            for (int index = 0; iterator.hasNext(); ++index) {
                Object key = iterator.next();
                long key_uid = System.identityHashCode(key);
                if (key_uid != 0) {
                    key_uid += (map_uid << 32);
                    object_store.put(key_uid, new WeakReference<>(key));
                }
                uids[index] = key_uid;
            }
            return uids;
        }
        return null;
    }

    @Override
    public long map_value(long key_uid, long map_uid) throws RemoteException {
        Object key = getUncheckedObject(key_uid);
        if (key != null) {
            return map_value(key, map_uid);
        }
        return 0;
    }

    @Override
    public long map_value(Object key, long map_uid) throws RemoteException {
        Object object = getUncheckedObject(map_uid);
        if (object instanceof Map) {
            Object child = ((Map) object).get(key);
            long child_uid = System.identityHashCode(child);
            if (child_uid != 0) {
                child_uid += (map_uid << 32);
                object_store.put(child_uid, new WeakReference<>(child));
                return child_uid;
            }
        }
        return 0;
    }

    @Override
    public boolean validate(long uid) throws RemoteException {
        return getUncheckedObject(uid) != null;
    }

    @Override
    public boolean instance(String type_name, long uid) throws RemoteException {
        Object object = getUncheckedObject(uid);
        if (object != null) {
            Class<?> type = getClassObject(type_name);
            if (type != null && type.isAssignableFrom(object.getClass())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String type(long uid) throws RemoteException {
        Object object = getUncheckedObject(uid);
        if (object != null) {
            Class<?> type = object.getClass();
            String name = class_name_cache.get(type);
            if (name != null) {
                return name;
            }
            class_name_cache.put(type, name = type.getName());
            return name;
        }
        return null;
    }

    @Override
    public Serializable serialize(long uid) throws RemoteException {
        Object evaluated = getUncheckedObject(uid);
        return evaluated instanceof Serializable ? (Serializable) evaluated : null;
    }

    @Override
    public Serializable serialize(String name) throws RemoteException {
        Object evaluated = getUncheckedObject(name);
        return evaluated instanceof Serializable ? (Serializable) evaluated : null;
    }

    @Override
    public Serializable serialize(String name, long owner_uid) throws RemoteException {
        Object evaluated = getUncheckedObject(name, owner_uid);
        return evaluated instanceof Serializable ? (Serializable) evaluated : null;
    }

    @Override
    public byte int8(long uid) throws RemoteException {
        Object evaluated = getUncheckedObject(uid);
        return evaluated instanceof Byte ? (byte) evaluated : -1;
    }

    @Override
    public byte int8(String field) throws RemoteException {
        Object evaluated = getUncheckedObject(field);
        return evaluated instanceof Byte ? (byte) evaluated : -1;
    }

    @Override
    public byte int8(String field, long owner_uid) throws RemoteException {
        Object evaluated = getUncheckedObject(field, owner_uid);
        return evaluated instanceof Byte ? (byte) evaluated : -1;
    }

    @Override
    public short int16(long uid) throws RemoteException {
        Object evaluated = getUncheckedObject(uid);
        return evaluated instanceof Short ? (short) evaluated : -1;
    }

    @Override
    public short int16(String field) throws RemoteException {
        Object evaluated = getUncheckedObject(field);
        return evaluated instanceof Short ? (short) evaluated : -1;
    }

    @Override
    public short int16(String field, long owner_uid) throws RemoteException {
        Object evaluated = getUncheckedObject(field, owner_uid);
        return evaluated instanceof Short ? (short) evaluated : -1;
    }

    @Override
    public int int32(long uid) throws RemoteException {
        Object evaluated = getUncheckedObject(uid);
        return evaluated instanceof Integer ? (int) evaluated : -1;
    }

    @Override
    public int int32(String field) throws RemoteException {
        Object evaluated = getUncheckedObject(field);
        return evaluated instanceof Integer ? (int) evaluated : -1;
    }

    @Override
    public int int32(String field, long owner_uid) throws RemoteException {
        Object evaluated = getUncheckedObject(field, owner_uid);
        return evaluated instanceof Integer ? (int) evaluated : -1;
    }

    @Override
    public long int64(long uid) throws RemoteException {
        Object evaluated = getUncheckedObject(uid);
        return evaluated instanceof Long ? (long) evaluated : -1;
    }

    @Override
    public long int64(String field) throws RemoteException {
        Object evaluated = getUncheckedObject(field);
        return evaluated instanceof Long ? (long) evaluated : -1;
    }

    @Override
    public long int64(String field, long owner_uid) throws RemoteException {
        Object evaluated = getUncheckedObject(field, owner_uid);
        return evaluated instanceof Long ? (long) evaluated : -1;
    }

    @Override
    public float fp32(long uid) throws RemoteException {
        Object evaluated = getUncheckedObject(uid);
        return evaluated instanceof Float ? (float) evaluated : -1;
    }

    @Override
    public float fp32(String field) throws RemoteException {
        Object evaluated = getUncheckedObject(field);
        return evaluated instanceof Float ? (float) evaluated : -1;
    }

    @Override
    public float fp32(String field, long owner_uid) throws RemoteException {
        Object evaluated = getUncheckedObject(field, owner_uid);
        return evaluated instanceof Float ? (float) evaluated : -1;
    }

    @Override
    public double fp64(long uid) throws RemoteException {
        Object evaluated = getUncheckedObject(uid);
        return evaluated instanceof Double ? (double) evaluated : -1;
    }

    @Override
    public double fp64(String field) throws RemoteException {
        Object evaluated = getUncheckedObject(field);
        return evaluated instanceof Double ? (double) evaluated : -1;
    }

    @Override
    public double fp64(String field, long owner_uid) throws RemoteException {
        Object evaluated = getUncheckedObject(field, owner_uid);
        return evaluated instanceof Double ? (double) evaluated : -1;
    }

    @Override
    public boolean bool(long uid) throws RemoteException {
        Object evaluated = getUncheckedObject(uid);
        return evaluated instanceof Boolean && (boolean) evaluated;
    }

    @Override
    public boolean bool(String field) throws RemoteException {
        Object evaluated = getUncheckedObject(field);
        return evaluated instanceof Boolean && (boolean) evaluated;
    }

    @Override
    public boolean bool(String field, long owner_uid) throws RemoteException {
        Object evaluated = getUncheckedObject(field, owner_uid);
        return evaluated instanceof Boolean && (boolean) evaluated;
    }

    @Override
    public char uint16(long uid) throws RemoteException {
        Object evaluated = getUncheckedObject(uid);
        return (char) (evaluated instanceof Character ? evaluated : -1);
    }

    @Override
    public char uint16(String field) throws RemoteException {
        Object evaluated = getUncheckedObject(field);
        return (char) (evaluated instanceof Character ? evaluated : -1);
    }

    @Override
    public char uint16(String field, long owner_uid) throws RemoteException {
        Object evaluated = getUncheckedObject(field, owner_uid);
        return (char) (evaluated instanceof Character ? evaluated : -1);
    }

    /**
     * Gets an Object that may not be Serializable and as a result couldn't be transported over the rmi socket.
     */
    private Object getUncheckedObject(long uid) {
        WeakReference ref = object_store.get(uid);
        if (ref != null) {
            Object object = ref.get();
            if (object != null) {
                return object;
            } else {
                object_store.remove(uid);
            }
        }
        return null;
    }

    /**
     * Gets an Object that may not be Serializable and as a result couldn't be transported over the rmi socket.
     */
    private Object getUncheckedObject(String field) throws RemoteException {
        MethodHandle handle = getMethodHandle(field);
        if (handle != null) {
            try {
                return handle.invoke();
            } catch (Throwable t) {
                throw new RemoteException("Failed to retrieve the value of " + field + ".", t);
            }
        }
        return null;
    }

    /**
     * Gets an Object that may not be Serializable and as a result couldn't be transported over the rmi socket.
     */
    private Object getUncheckedObject(String field, long owner_uid) throws RemoteException {
        Object object = getUncheckedObject(owner_uid);
        if (object != null) {
            MethodHandle handle = getMethodHandle(field);
            if (handle != null) {
                try {
                    return handle.invoke(object);
                } catch (Throwable t) {
                    throw new RemoteException("Failed to retrieve the value of " + field + " as the child of " + owner_uid + ".", t);
                }
            }
        }
        return null;
    }

    /**
     * Gets a MethodHandle for a field and caches it.
     *
     * @param name A String in the format "classname.fieldname"
     * @return A MethodHandle if the name was properly formatted.
     * @throws RemoteException if the name was unable to be resolved to a class and field.
     */
    private MethodHandle getMethodHandle(String name) throws RemoteException {
        MethodHandle handle = cached_handles.get(name);
        if (handle == null) {
            int name_separator = name.lastIndexOf(".");
            if (name_separator != -1) {
                try {
                    Class<?> c = getClassObject(name.substring(0, name_separator));
                    if (c != null) {
                        Field f = c.getDeclaredField(name.substring(name_separator + 1));
                        if ((f.getModifiers() & Modifier.PUBLIC) != Modifier.PUBLIC) {
                            f.setAccessible(true);
                        }
                        handle = MethodHandles.lookup().unreflectGetter(f);
                        cached_handles.put(name, handle);
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new RemoteException("Failed to get a MethodHandle for " + name, e);
                }
            }
        }
        return handle;
    }

    /**
     * Gets a Class for the given type name and caches it. The resolution passes false for initialize.
     */
    private Class<?> getClassObject(String type_name) throws RemoteException {
        Class<?> type = class_cache.get(type_name);
        if (type == null) {
            try {
                type = Class.forName(type_name, false, class_loader);
                class_cache.put(type_name, type);
            } catch (ClassNotFoundException cnfe) {
                throw new RemoteException("Unable to find a Class<?> for " + type_name, cnfe);
            }
        }
        return type;
    }
}
