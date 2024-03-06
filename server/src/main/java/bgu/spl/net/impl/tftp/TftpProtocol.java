package bgu.spl.net.impl.tftp;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.Connections;

class holder {
    static ConcurrentHashMap<Integer,Boolean> logginId= new ConcurrentHashMap<>();
}

public class TftpProtocol implements BidiMessagingProtocol<byte[]>  {
    private boolean shouldTerminate = false;
    private int connectionId;
    private Connections<byte[]> connections; // string or byte[]?
    private static final String FILES_DIRECTORY = "./././././././Flies";


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
        byte opByte = message[1];
        byte[] ack = {0,4,0};
        if(holder.logginId.get(connectionId) == false && opByte != 7){
            byte[] error = {0,5,0,6,0};
            connections.send(connectionId,error);
            return;
        }
        
        if (opByte == 1){ // RRQ **
            String fileName = new String(message,2,message.length-2); // maybe length is 3
            Path path = Paths.get(FILES_DIRECTORY + "/" + fileName); // "./././././././Flies/gal.txt"
            if (Files.exists(path)){
                File file = path.toFile();
                byte[] blockNumber = {0,1};
                //byte[] block = {0,3,0,1};
                //connections.send(connectionId,block);
                try (FileInputStream fis = new FileInputStream(file)) {
                    // Read file data in chunks
                        connections.send(connectionId,ack);
                        byte[] data = new byte[512];
                        int bytesRead;
                        while ((bytesRead = fis.read(data)) != -1) {
                            // Send the data
                            byte[] packetToSend = new byte[bytesRead + 6];

                            // NEED TO CHANGE:
                            packetToSend[0] = 0;
                            packetToSend[1] = 3;
                            packetToSend[2] = 0; //
                            packetToSend[3] = 0; //
                            packetToSend[4] = blockNumber[0];
                            packetToSend[5] = blockNumber[1];
                            System.arraycopy(data, 0, packetToSend, 4, bytesRead);
                            connections.send(connectionId,packetToSend);
                            if(blockNumber[1] == 127) { 
                                blockNumber[0]++;
                                blockNumber[1] = 0;
                            } else {
                                blockNumber[1]++;
                            }
                            data = new byte[512];
                        }
                        

                    }
                catch (IOException e) {
                }
            } else {
                byte[] error = {0,5,0,1,0};
                connections.send(connectionId,error);
            }
            
        } 
        else if (opByte == 2){ //WRQ **
            
        }else if (opByte == 6){ // DIRQ **
            File dir = new File(FILES_DIRECTORY);
            File[] filesList = dir.listFiles();
            byte[] blockNumber = {0,1};
            for (File file : filesList) {
                if (file.isFile()) {
                    byte[] fileName = file.getName().getBytes();
                    byte[] data = new byte[fileName.length + 2];
                    data[0] = 0;
                    data[1] = 3;
                    System.arraycopy(fileName, 0, data, 2, fileName.length);
                    connections.send(connectionId, data);
                }
            }
            //byte[] end = {0,3,0,0};
            //connections.send(connectionId,end);
            
        }else if (opByte == 7){ // LOGRQ
            if (holder.logginId.contains(connectionId)){
                byte[] error = {0,5,0,7,0};
                connections.send(connectionId,error);
            }else {
                holder.logginId.put(connectionId,true);
                connections.send(connectionId, ack);
            }
        }else if (opByte == 8){ // DELRQ
            String fileName = new String(message,2,message.length-2); // maybe length is 3
            Path path = Paths.get(FILES_DIRECTORY + "/" + fileName); // "./././././././Flies/gal.txt"
            if (Files.exists(path)){
                File file = path.toFile();
                file.delete();
                connections.send(connectionId,ack);
            } else {
                byte[] error = {0,5,0,1,0};
                connections.send(connectionId,error);
            }
        }else if (opByte == 10){ // DISC
            holder.logginId.put(connectionId,false);
            //shouldTerminate = true; // ???
            connections.send(connectionId,ack);
        }
   //     throw new UnsupportedOperationException("Unimplemented method 'process'");
    }

    @Override
    public boolean shouldTerminate() {
        // TODO implement this
        return shouldTerminate;
       // throw new UnsupportedOperationException("Unimplemented method 'shouldTerminate'");
    } 


    
}
