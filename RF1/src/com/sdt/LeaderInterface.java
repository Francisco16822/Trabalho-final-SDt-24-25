package com.sdt;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public interface LeaderInterface extends Remote {
    void addNode(String nodeId) throws RemoteException;
    void updateDocument(String documentId, String content) throws RemoteException;
    Map<String, String> getDocumentVersions() throws RemoteException;  // Método para obter versões de documentos
    List<String> getPendingUpdates() throws RemoteException;            // Método para obter atualizações pendentes

    void receiveAck(String documentId, String nodeId) throws RemoteException;  // Para o recebimento de ACKs
    void setNewLeader(String nodeId) throws RemoteException;
    void updateAckTime(String nodeId) throws RemoteException;
    CopyOnWriteArrayList<String> getActiveNodes() throws RemoteException;
    public long updateHeartbeatTime(String nodeId) throws RemoteException;
}