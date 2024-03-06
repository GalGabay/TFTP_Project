package bgu.spl.net.impl.tftp;

import java.util.concurrent.ConcurrentHashMap;
import bgu.spl.net.srv.BlockingConnectionHandler;
import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;



public class TftpConnections<T> implements Connections<T> {
    ConcurrentHashMap<Integer,BlockingConnectionHandler<T>> logginId = new ConcurrentHashMap<>();

    // changed to void
    public boolean connect(int connectionId, ConnectionHandler<T> handler){
        return logginId.put(connectionId,(BlockingConnectionHandler<T>)handler) != null;
    }

    // changed to void
    public void send(int connectionId, T msg){
        logginId.get(connectionId).send(msg);
    }

    // changed to void
    public boolean disconnect(int connectionId){
        return logginId.remove(connectionId) != null;
    }
}
