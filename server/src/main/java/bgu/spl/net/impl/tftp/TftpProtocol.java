package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.Connections;

class holder {
    static ConcurrentHashMap<Integer,Boolean> logginId= new ConcurrentHashMap<>();
}

public class TftpProtocol implements BidiMessagingProtocol<byte[]>  {
    private boolean shouldTerminate = false;
    private int connectionId;
    private Connections<String> connections; 

    @Override
    public void start(int connectionId, Connections<byte[]> connections) {
        // TODO implement this
        this.connectionId = connectionId;
        this.connections = connections;
        holder.logginId.put(connectionId, true);
        // throw new UnsupportedOperationException("Unimplemented method 'start'");
    }

    @Override
    public void process(byte[] message) {
        // TODO implement this
        
        throw new UnsupportedOperationException("Unimplemented method 'process'");
    }

    @Override
    public boolean shouldTerminate() {
        // TODO implement this
        return shouldTerminate;
        throw new UnsupportedOperationException("Unimplemented method 'shouldTerminate'");
    } 


    
}
