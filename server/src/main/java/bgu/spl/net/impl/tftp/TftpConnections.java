public class TftpConnections <T> implements Connections <T> {
    static ConcurrentHashMap<Integer,ConnectionHandler<T>> logginId = new ConcurrentHashMap<>();

    boolean connect(int connectionId, ConnectionHandler<T> handler){
        return logginId.put(connectionId,handler) != null;
    }

    boolean send(int connectionId, T msg){
        return logginId.get(connectionId).send(msg) != null;
    }

    boolean disconnect(int connectionId){
        return logginId.remove(connectionId) != null;
    }
}
