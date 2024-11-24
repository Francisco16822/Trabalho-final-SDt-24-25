package com.sdt;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface LeaderInterface extends Remote {


    void updateDocument(String documentId, String content) throws RemoteException;
    void receiveAck(String documentId, String nodeId) throws RemoteException;


    List<String> getPendingUpdates() throws RemoteException;
    List<String> getDocumentList() throws RemoteException;
    Map<String, String> getDocumentVersions() throws RemoteException;


    void addNode(String nodeId) throws RemoteException;
    //void handleNodeExit(String nodeId) throws RemoteException;
}
