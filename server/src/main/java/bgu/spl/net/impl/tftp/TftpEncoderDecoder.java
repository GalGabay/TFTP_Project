package bgu.spl.net.impl.tftp;

import java.util.Arrays;

import bgu.spl.net.api.MessageEncoderDecoder;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    //TODO: Implement here the TFTP encoder and decoder
    private byte[] bytes = new byte[1 << 10]; //start with 1k
    private int len = 0;
    private int sizeArr =0;

    @Override
    public byte[] decodeNextByte(byte nextByte) {
        // TODO: implement this
        
        pushByte(nextByte);
        sizeArr++; // size of the array right now
        if(sizeArr > 1) {
            if ((bytes[1] == 6) || (bytes[1] == 10)){ // DIRQ or DISC
                //pushByte(nextByte);
                return bytes;
            } else if (bytes[1] == 4){ // ACK
                //
                //
                return bytes;
            } else if(bytes[1] == 3){ // DATA
                int packetSize = 0;
                if(sizeArr == 4) {
                    packetSize = (bytes[2] << 8) | (bytes[3] & 0xFF);        
                }
                if(sizeArr > 4) {
                    if(sizeArr > packetSize+2) {
                        return bytes;
                    }
                }
            } 
            else if(nextByte == 0) {
                return bytes;
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