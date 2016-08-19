package com.carpenter.cross_jvm_reflection.client;

import com.carpenter.cross_jvm_reflection.shared.rmi.ServerStub;
import com.sun.tools.attach.*;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;

public class ClientExample {
    private static final String ABSOLUTE_PATH_TO_JAVA_AGENT_JAR = "For you to fill in";
    private static final String PID_TO_ATTACH_TO = "For you to fill in";

    public static void main(String[] args) throws IOException, AgentLoadException, AgentInitializationException, AttachNotSupportedException, NotBoundException {
        for (VirtualMachineDescriptor vmd : VirtualMachine.list()) {
            if (vmd.id().equals(PID_TO_ATTACH_TO)) {
                VirtualMachine vm = VirtualMachine.attach(vmd);
                vm.loadAgent(ABSOLUTE_PATH_TO_JAVA_AGENT_JAR, "name=Server" + vm.id());
                vm.detach();
            }
        }
        ServerStub stub = (ServerStub) LocateRegistry.getRegistry().lookup("Server" + PID_TO_ATTACH_TO);
        System.out.println("Value of Class.ENUM on the remote jvm: " + stub.int32("java/lang/Class.ENUM"));
    }
}
