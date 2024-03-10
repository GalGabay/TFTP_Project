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
    private static final String FILES_DIRECTORY = "./server/Flies";

    private byte[] data;
    private int blockNumberToInt = 0;
    


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

    public void handleData(byte[] data, byte[] blockNumber) {
        if(blockNumber[1] == 127) { 
            blockNumber[0]++;
            blockNumber[1] = 0;
        } else {
            blockNumber[1]++;
        }
        int i = blockNumberToInt*512;
        blockNumberToInt++;
        byte[] packetToSend = new byte[Math.min(data.length-i,512)];
        packetToSend[0] = 0;
        packetToSend[1] = 3;
        packetToSend[2] = (byte) ((data.length >> 8) & 0xFF);
        packetToSend[3] = (byte) (data.length & 0xFF);     
        packetToSend[4] = blockNumber[0];
        packetToSend[5] = blockNumber[1];
        System.arraycopy(data, i, packetToSend, 6, packetToSend.length-6);
        connections.send(connectionId,packetToSend);
        System.out.println("send data to client");
        if(packetToSend.length != 512 || i == data.length-512) {
            blockNumberToInt = 0;
            data = null;
        } 
        
        
        //     // Read file data in chunks
        //         byte[] data = new byte[512];
        //         int bytesRead;
        //         if ((bytesRead = fileToRead.read(data)) != -1) {
        //             // Send the data
        //             byte[] packetToSend = new byte[bytesRead + 6];
        //             packetToSend[0] = 0;
        //             packetToSend[1] = 3;
        //             packetToSend[2] = (byte) ((bytesRead >> 8) & 0xFF);
        //             packetToSend[3] = (byte) (bytesRead & 0xFF);     
        //             packetToSend[4] = blockNumber[0];
        //             packetToSend[5] = blockNumber[1];
        //             System.arraycopy(data, 0, packetToSend, 6, bytesRead);
        //             connections.send(connectionId,packetToSend);
                    
        //             data = new byte[512];
        //         }
                

        //     }
        // catch (IOException e) {
        // }

    }

    @Override
    public void process(byte[] message) {
        // TODO implement this
        for(int i = 0; i < message.length; i++) {
            System.out.print("message[" + i + "] is: " + message[i] + " ");
        }
        byte opByte = message[1];
        byte[] ack = {0,4,0,0};
        if(holder.logginId.get(connectionId) == false && opByte != 7){
            byte[] error = {0,5,0,6,0};
            connections.send(connectionId,error);
            return;
        }
        if(opByte == 4 & message.length == 4) { // ACK Data(with block number)
            byte[] blockNumber = {message[2],message[3]};
            // call func to handle data
            handleData(data, blockNumber);
        }
        else if (opByte == 1){ // RRQ
            System.out.println("Entered To RRQ!");
            String fileName = new String(message,2,message.length-3); // maybe length is 3
            Path path = Paths.get(FILES_DIRECTORY + "/" + fileName); 
            System.out.println("Current Working Directory: " + Paths.get("").toAbsolutePath());
            System.out.println("path is: " + path);
            if (Files.exists(path)){
                File file = path.toFile();
                connections.send(connectionId,ack);
                System.out.println("send ack to client after RRQ");
                // call func to handle data
                try (FileInputStream fileToRead = new FileInputStream(file)) {
                    // Read file data in chunks
                    data = new byte[(int) file.length()];
                    fileToRead.read(data);
                    byte[] blockNumber = {0,0};
                    handleData(data,blockNumber);
                    } catch (IOException e) {}
                
                //byte[] block = {0,3,0,1};
                //connections.send(connectionId,block);
                // try (FileInputStream fileToRead = new FileInputStream(file)) {
                //     // Read file data in chunks

                //         byte[] data = new byte[512];
                //         int bytesRead;
                //         while ((bytesRead = fileToRead.read(data)) != -1) {
                //             // Send the data
                //             byte[] packetToSend = new byte[bytesRead + 6];
                //             packetToSend[0] = 0;
                //             packetToSend[1] = 3;
                //             packetToSend[2] = (byte) ((bytesRead >> 8) & 0xFF);
                //             packetToSend[3] = (byte) (bytesRead & 0xFF);     
                //             packetToSend[4] = blockNumber[0];
                //             packetToSend[5] = blockNumber[1];
                //             System.arraycopy(data, 0, packetToSend, 6, bytesRead);
                //             connections.send(connectionId,packetToSend);
                //             if(blockNumber[1] == 127) { 
                //                 blockNumber[0]++;
                //                 blockNumber[1] = 0;
                //             } else {
                //                 blockNumber[1]++;
                //             }
                //             data = new byte[512];
                //         }
                        

                //     }
                // catch (IOException e) {
                // }
            } else { // need to get it back
                byte[] error = {0,5,0,1,0};
                connections.send(connectionId,error);
            }
            
        } 
        else if (opByte == 2){ //WRQ 
            String fileName = new String(message,2,message.length-3); // maybe length is 3
            Path folder = Paths.get(FILES_DIRECTORY);
            Path filePath = folder.resolve(fileName);
            if (!Files.exists(filePath)) {
                byte[] error = {0,5,0,5,0};
                connections.send(connectionId,error);
            } else {
                try {
                    Files.createFile(filePath);
                    connections.send(connectionId,ack);
                    bCastPacket((byte)1, Arrays.copyOfRange(message, 2, message.length-2));
                } catch (IOException e) {} 
                
            }

        } else if (opByte == 6){ // DIRQ 
            File dir = new File(FILES_DIRECTORY);
            File[] filesList = dir.listFiles();
            String fileNames = "";
            for (File file : filesList) {
                if (file.isFile()) {
                    fileNames += file.getName();
                    fileNames += '0';
                }
            }
            data = fileNames.getBytes();
            byte[] blockNumber = {0,0};
            handleData(data, blockNumber);
            

            
        }else if (opByte == 7){ // LOGRQ
            if (holder.usernames.contains(connectionId)){ // need to change?
                byte[] error = {0,5,0,7,0};
                connections.send(connectionId,error);
            }else {
                holder.logginId.put(connectionId,true);
                holder.usernames.put(connectionId,Arrays.copyOfRange(message, 2, message.length-2));
                connections.send(connectionId, ack);
                System.out.println("send ack to client after LOGRQ");
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
