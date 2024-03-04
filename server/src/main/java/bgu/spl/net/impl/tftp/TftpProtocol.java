package bgu.spl.net.impl.tftp;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.Connections;

class holder {
    static ConcurrentHashMap<Integer,Boolean> logginId= new ConcurrentHashMap<>();
}

public class TftpProtocol implements BidiMessagingProtocol<byte[]>  {
    private boolean shouldTerminate = false;
    private int connectionId;
    private Connections<String> connections; 
    private static final String FILES_DIRECTORY = "./././././././Files";


    @Override
    public void start(int connectionId, Connections<byte[]> connections) {
        // TODO implement this
        this.connectionId = connectionId;
        this.connections = connections;
        holder.logginId.put(connectionId, false); // not sure
        // throw new UnsupportedOperationException("Unimplemented method 'start'");
    }

    @Override
    public void process(byte[] message) {

        // TODO implement this
        byte byte = message[1];
        byte[] ack = {0,4,0};
        if (byte = 1){
            
        } 
        else if (byte = 2){
            
        }else if (byte = 6){
            
        }else if (byte = 7){


            if (holder.logginId.contains(connectionId)){
                connections.send({0,5,0,7,0});
            }else {
                holder.logginId.put(connectionId,true);
                connections.send(ack);
            }
        }else if (byte = 8){
            
        }else if (byte = 10){
            
        }
        throw new UnsupportedOperationException("Unimplemented method 'process'");
    }

    @Override
    public boolean shouldTerminate() {
        // TODO implement this
        return shouldTerminate;
        throw new UnsupportedOperationException("Unimplemented method 'shouldTerminate'");
    } 


    
}
