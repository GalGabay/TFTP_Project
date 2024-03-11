package bgu.spl.net.impl.tftp;

import java.io.File;
import java.io.UnsupportedEncodingException;

import bgu.spl.net.api.BidiMessagingProtocol;

public class TftpProtocol implements BidiMessagingProtocol<byte[]>{

    private File rrqFile;
    private boolean isInRRQ = false;
    private byte[] data;

    public byte[] process(byte[] message) {
        

        // listen thread:
        if(message.length > 2 && message[1] == 4) { // ACK
            if(message.length == 4) {
                System.out.println("ACK " + message[2] + message[3]);
            } else if(message.length == 3) {
                System.out.println("ACK " + message[2]);
            }
        } else if(message.length > 2 && message[1] == 9) { // BCAST
            byte[] fileName = new byte[message.length-4];
            System.arraycopy(message, 3, fileName, 0, fileName.length);
            String fileNameStr = "";
            try {
                fileNameStr = new String(fileName, "UTF-8");
            } catch (UnsupportedEncodingException e) {}
            System.out.println("BCAST " + (message[2] == 1 ? "add" : "del") + " " + fileNameStr);
        } else if(message.length > 2 && message[1] == 5) { // ERROR
            System.out.println("ERROR " + message[2] + message[3]);
        } else if(message.length > 2 && message[1] == 3) { // DATA
             
            
        }
        return null;
    }

    public boolean shouldTerminate() {
        return false;
    }
}
