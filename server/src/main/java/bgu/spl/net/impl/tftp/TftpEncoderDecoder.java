package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.MessageEncoderDecoder;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    //TODO: Implement here the TFTP encoder and decoder
    private byte[] bytes = new byte[1 << 10]; //start with 1k
    private int len = 0;
    private int counter =0;

    @Override
    public byte[] decodeNextByte(byte nextByte) {
        // TODO: implement this
        counter++;
        if (counter == 2 && nextByte ==6){
            pushByte(nextByte);
            return bytes;
        }
        if (nextByte == 0) {
            pushByte(nextByte);
            return bytes;
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
        return null;
    }
}