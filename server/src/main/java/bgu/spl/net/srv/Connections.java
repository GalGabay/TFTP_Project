package bgu.spl.net.srv;

import java.io.IOException;

public interface Connections<T> {

    boolean connect(int connectionId, ConnectionHandler<T> handler);

    void send(int connectionId, T msg);

    boolean disconnect(int connectionId);
}
