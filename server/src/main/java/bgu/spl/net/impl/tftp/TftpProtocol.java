package bgu.spl.net.impl.tftp;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.Connections;

class holder {
    static ConcurrentHashMap<Integer,Boolean> logginId= new ConcurrentHashMap<>();
    static ConcurrentHashMap<Integer,byte[]> usernames= new ConcurrentHashMap<>();
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

    public void bCastPacket(byte deleteOrAdded, byte[] fileName) {
        byte[] data = new byte[fileName.length + 4];
        data[0] = 0;
        data[1] = 9;
        data[2] = deleteOrAdded;
        System.arraycopy(fileName, 0, data, 3, fileName.length);
        data[data.length-1] = 0;
        for(int connectionId : holder.logginId.keySet()) {
            if(holder.logginId.get(connectionId) == true) {
                connections.send(connectionId, data);
            }
        }

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
        
        if (opByte == 1){ // RRQ 
            String fileName = new String(message,2,message.length-2); // maybe length is 3
            Path path = Paths.get(FILES_DIRECTORY + "/" + fileName); // "./././././././Flies/gal.txt"
            if (Files.exists(path)){
                File file = path.toFile();
                byte[] blockNumber = {0,1};
                //byte[] block = {0,3,0,1};
                //connections.send(connectionId,block);
                try (FileInputStream fileToRead = new FileInputStream(file)) {
                    // Read file data in chunks
                        connections.send(connectionId,ack);
                        byte[] data = new byte[512];
                        int bytesRead;
                        while ((bytesRead = fileToRead.read(data)) != -1) {
                            // Send the data
                            byte[] packetToSend = new byte[bytesRead + 6];
                            packetToSend[0] = 0;
                            packetToSend[1] = 3;
                            packetToSend[2] = (byte) ((bytesRead >> 8) & 0xFF);
                            packetToSend[3] = (byte) (bytesRead & 0xFF);     
                            packetToSend[4] = blockNumber[0];
                            packetToSend[5] = blockNumber[1];
                            System.arraycopy(data, 0, packetToSend, 6, bytesRead);
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
        else if (opByte == 2){ //WRQ 
            String fileName = new String(message,2,message.length-2); // maybe length is 3
            Path folder = Paths.get(FILES_DIRECTORY);
            Path filePath = folder.resolve(fileName);
            if (Files.exists(filePath)) {
                byte[] error = {0,5,0,5,0};
                connections.send(connectionId,error);
            } else {
                try {
                    Files.createFile(filePath);
                    connections.send(connectionId,ack);
                    bCastPacket((byte)1, Arrays.copyOfRange(message, 2, message.length-2));
                } catch (IOException e) {} 
                
            }

        } else if (opByte == 6){ // DIRQ **
            // File dir = new File(FILES_DIRECTORY);
            // File[] filesList = dir.listFiles();
            // byte[] blockNumber = {0,1};
            // try (FileInputStream fileToRead = new FileInputStream(file)) {

            //     byte[] data = new byte[512];
            //     int bytesRead;
            //     while ((bytesRead = fileToRead.read(data)) != -1) {
            //         // Send the data
            //         byte[] packetToSend = new byte[bytesRead + 6];
            //         packetToSend[0] = 0;
            //         packetToSend[1] = 3;
            //         packetToSend[2] = (byte) ((bytesRead >> 8) & 0xFF);
            //         packetToSend[3] = (byte) (bytesRead & 0xFF);     
            //         packetToSend[4] = blockNumber[0];
            //         packetToSend[5] = blockNumber[1];
            //         System.arraycopy(data, 0, packetToSend, 6, bytesRead);
            //         connections.send(connectionId,packetToSend);
            //         if(blockNumber[1] == 127) { 
            //             blockNumber[0]++;
            //             blockNumber[1] = 0;
            //         } else {
            //             blockNumber[1]++;
            //         }
            //         data = new byte[512];
            //     }
            // } catch (IOException e) {}
            // for (File file : filesList) {
            //     if (file.isFile()) {
            //         byte[] fileName = file.getName().getBytes();
            //         byte[] data = new byte[fileName.length + 2];
            //         data[0] = 0;
            //         data[1] = 3;
            //         System.arraycopy(fileName, 0, data, 2, fileName.length);
            //         connections.send(connectionId, data);
            //     }
            // }
            //byte[] end = {0,3,0,0};
            //connections.send(connectionId,end);
            
        }else if (opByte == 7){ // LOGRQ
            if (holder.usernames.contains(connectionId)){
                byte[] error = {0,5,0,7,0};
                connections.send(connectionId,error);
            }else {
                holder.logginId.put(connectionId,true);
                holder.usernames.put(connectionId,Arrays.copyOfRange(message, 2, message.length-2));
                connections.send(connectionId, ack);
            }
        }else if (opByte == 8){ // DELRQ
            String fileName = new String(message,2,message.length-2); // maybe length is 3
            Path path = Paths.get(FILES_DIRECTORY + "/" + fileName); // "./././././././Flies/gal.txt"
            if (Files.exists(path)){
                File file = path.toFile();
                file.delete();
                connections.send(connectionId,ack);
                bCastPacket((byte)0, Arrays.copyOfRange(message, 2, message.length-2));
            } else {
                byte[] error = {0,5,0,1,0};
                connections.send(connectionId,error);
            }
        } else if (opByte == 10){ // DISC
            holder.logginId.put(connectionId,false);
            connections.send(connectionId,ack);
            holder.usernames.remove(connectionId);
            shouldTerminate = true;
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
