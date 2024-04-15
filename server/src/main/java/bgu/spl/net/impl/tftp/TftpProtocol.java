package bgu.spl.net.impl.tftp;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
    static ConcurrentHashMap<Integer,String> usernames= new ConcurrentHashMap<>();
}

public class TftpProtocol implements BidiMessagingProtocol<byte[]>  {
    private boolean shouldTerminate = false;
    private int connectionId;
    private Connections<byte[]> connections; // string or byte[]?
    private static final String FILES_DIRECTORY = "./Flies";

    private byte[] data;
    private int blockNumberToInt = 0;
    private File wrqFile;
    


    @Override
    public void start(int connectionId, Connections<byte[]> connections) {
        // TODO implement this
        this.connectionId = connectionId;
        this.connections = connections;
        holder.logginId.put(connectionId, false); // not sure
        data = null;
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
        if(data!=null) {

            short blockNumberCast = (short)(((short)((blockNumber[0] & 0xFF)) << 8) | (short)(blockNumber[1] & 0xFF));
            blockNumberCast++;
            blockNumber[0] = (byte)((blockNumberCast >> 8) & 0xFF);
            blockNumber[1] = (byte)(blockNumberCast & 0xFF);
            int i = blockNumberToInt*512;
            blockNumberToInt++;
            byte[] packetToSend = new byte[Math.min(data.length-i+6,518)]; // 512 size of data + 7 more places
            packetToSend[0] = 0;
            packetToSend[1] = 3;
            packetToSend[2] = (byte) ((packetToSend.length-6 >> 8) & 0xFF);
            packetToSend[3] = (byte) (packetToSend.length-6 & 0xFF);     
            packetToSend[4] = blockNumber[0];
            packetToSend[5] = blockNumber[1];
            System.arraycopy(data, i, packetToSend, 6, packetToSend.length-6);
            connections.send(connectionId,packetToSend);
            if(packetToSend.length != 518 || i == data.length-512) {
                blockNumberToInt = 0;
                this.data = null;
            } 
            
        }
    }

    @Override
    public void process(byte[] message) {
        // TODO implement this
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
        else if (opByte == 1){ // RRQ WORKS

            String fileName = new String(message,2,message.length-3); // maybe length is 3
            Path path = Paths.get(FILES_DIRECTORY + "/" + fileName); 

            if (Files.exists(path)){
                File file = path.toFile();
                connections.send(connectionId,ack);
                // call func to handle data
                try (FileInputStream fileToRead = new FileInputStream(file)) {
                    // Read file data in chunks
                    data = new byte[(int) file.length()];
                    fileToRead.read(data);
                    byte[] blockNumber = {0,0};
                    handleData(data,blockNumber);
                    } catch (IOException e) {}
            } else { // need to get it back
                byte[] error = {0,5,0,1,0};
                connections.send(connectionId,error);
            }
            
        } 
        else if (opByte == 2){ //WRQ WORKS
            String fileName = new String(message,2,message.length-3); // maybe length is 3
            Path folder = Paths.get(FILES_DIRECTORY);
            Path filePath = folder.resolve(fileName);          
            if (Files.exists(filePath)) {
                byte[] error = {0,5,0,5,0};
                connections.send(connectionId,error);
            } else {
                try {
                    Files.createFile(filePath);
                    this.wrqFile = filePath.toFile();
                    connections.send(connectionId,ack);
                } catch (IOException e) {} 
                
            }

        } else if(opByte == 3) { // DATA WORKS
            short packetSize = (short)((message[2] << 8) | (message[3] & 0xFF)); 
            byte[] ackData = {0,4,message[4],message[5]};  
            try (FileOutputStream fileToCreate = new FileOutputStream(wrqFile, true)) {
                fileToCreate.write(Arrays.copyOfRange(message, 6, message.length));
        } catch (IOException e) {
            e.printStackTrace();}
            
            connections.send(connectionId,ackData);
            if(packetSize < 512) {
                bCastPacket((byte)1, wrqFile.getName().getBytes());
                this.wrqFile = null;
            }
        }
        else if (opByte == 6){ // DIRQ WORKS
            File dir = new File(FILES_DIRECTORY);
            File[] filesList = dir.listFiles();
            String fileNames = "";
            int filesCounter = 0;
            for (File file : filesList) {
                if (file.isFile()) {
                    filesCounter++;
                    fileNames += file.getName();
                }
            }
            byte[] fileNamesToBytes = fileNames.getBytes();
            
            if(filesCounter != 0) {
                data = new byte[fileNamesToBytes.length + filesCounter-1];
                int filesLength = 0;
                for (File file : filesList) {
                    if (file.isFile()) {
                        filesCounter--;
                        System.arraycopy(file.getName().getBytes(), 0, data, filesLength, file.getName().getBytes().length);
                        filesLength += file.getName().getBytes().length + 1;
                        if(filesCounter != 0) {
                            data[filesLength-1] = 0;
                        }       
                        
                    }
                }
                
                byte[] blockNumber = {0,0};
                handleData(data, blockNumber);
            }
            
            

            
        }else if (opByte == 7){ // LOGRQ WORKS
            String userName = new String(message, 2, message.length-2);
            if (holder.usernames.contains(userName) || (holder.logginId.containsKey(connectionId) && holder.logginId.get(connectionId) == true)){ // need to change?
                byte[] error = {0,5,0,7,0};
                connections.send(connectionId,error);
            }else {
                holder.logginId.put(connectionId,true);
                holder.usernames.put(connectionId,userName);
                connections.send(connectionId, ack);
            }
        }else if (opByte == 8){ // DELRQ WORKS
            String fileName = new String(message,2,message.length-3); 
            Path path = Paths.get(FILES_DIRECTORY + "/" + fileName); 
            if (Files.exists(path)){
                File file = path.toFile();
                file.delete();
                connections.send(connectionId,ack);
                bCastPacket((byte)0, Arrays.copyOfRange(message, 2, message.length-1));
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
    }

    @Override
    public boolean shouldTerminate() {
        // TODO implement this
        return shouldTerminate;
    } 
}
