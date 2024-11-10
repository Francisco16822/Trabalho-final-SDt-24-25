package com.sdt;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface LeaderInterface extends Remote {
    void updateDocument(String documentId, String content) throws RemoteException;
}
