package com.carpenter.cross_jvm_reflection.server.java_agent;

import com.carpenter.cross_jvm_reflection.server.rmi.impl.DefaultServerImpl;

import java.lang.instrument.Instrumentation;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class JavaAgent {
    public static void agentmain(String args, Instrumentation instrumentation) throws RemoteException {
        String name = null;
        for (String arg : args.split(",")) {
            if (arg.startsWith("name=")) {
                name = arg.replace("name=", "");
            }
        }
        if (name == null) {
            throw new IllegalArgumentException("The name to use for the rmi server was not provided as an argument to the java agent.");
        }
        Registry registry = LocateRegistry.getRegistry();
        if (registry != null) {
            DefaultServerImpl rmi = new DefaultServerImpl(ClassLoader.getSystemClassLoader());
            try {
                //Attempt to bind to the registry stub that was returned.
                registry.rebind(name, rmi);
            } catch (ConnectException ce) {
                //Couldn't bind to it (perhaps it doesn't exist), creating a new one.
                registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
                registry.rebind(name, rmi);
            }
        }
    }
}
