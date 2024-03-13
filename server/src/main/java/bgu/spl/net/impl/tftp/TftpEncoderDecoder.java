package bgu.spl.net.impl.tftp;

import java.util.Arrays;

import bgu.spl.net.api.MessageEncoderDecoder;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    //TODO: Implement here the TFTP encoder and decoder
    private byte[] bytes = new byte[1 << 10]; //start with 1k
    private int len = 0;
    private int sizeArr = 0;

    @Override
    public byte[] decodeNextByte(byte nextByte) {
        // TODO: implement this
        
        pushByte(nextByte);
        sizeArr++; // size of the array right now
        if(sizeArr > 1) {
            if ((bytes[1] == 6) || (bytes[1] == 10)){ // DIRQ or DISC
                //pushByte(nextByte);
                byte[] output = new byte[sizeArr];
                for(int i = 0; i < sizeArr; i++) {
                    output[i] = bytes[i];
                }
                sizeArr = 0;
                len = 0;
                bytes = new byte[1 << 10];
                return output;
            } else if (bytes[1] == 4){ // ACK
                if(sizeArr == 4) {
                    byte[] output = new byte[sizeArr];
                    for(int i = 0; i < sizeArr; i++) {
                        output[i] = bytes[i];
                    }
                    len = 0;
                    sizeArr = 0;
                    bytes = new byte[1 << 10];
                    return output;
                }
            } else if(bytes[1] == 3){ // DATA
                
                if(sizeArr >= 4) {
                    int packetSize = (bytes[2] << 8) | (bytes[3] & 0xFF);      
                    if(sizeArr >= packetSize+6) {
                        byte[] output = new byte[sizeArr];
                        for(int i = 0; i < sizeArr; i++) {
                            output[i] = bytes[i];
                        }
                        len = 0;
                        sizeArr = 0;
                        bytes = new byte[1 << 10];
                        return output;
                    }
                }
            } 
            else if(nextByte == 0) {
                byte[] output = new byte[sizeArr];
                for(int i = 0; i < sizeArr; i++) {
                    output[i] = bytes[i];
                }
                len = 0;
                sizeArr = 0;
                bytes = new byte[1 << 10];
                return output;
            }
        }    
        return null;
    }

     private void pushByte(byte nextByte) {
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2);
        }
 
        bytes[len++] = nextByte;
    }


    @Override
    public byte[] encode(byte[] message) {
        //TODO: implement this
        return message;
    }
}