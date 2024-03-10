package bgu.spl.net.srv;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class BlockingConnectionHandler<T> implements Runnable, ConnectionHandler<T> {

    private final BidiMessagingProtocol<T> protocol;
    private final MessageEncoderDecoder<T> encdec;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private volatile boolean connected = true;

    public BlockingConnectionHandler(Socket sock, MessageEncoderDecoder<T> reader, BidiMessagingProtocol<T> protocol) {
        this.sock = sock;
        this.encdec = reader;
        this.protocol = protocol;
    }
    
    public BidiMessagingProtocol<T> getProtocol() {
        return protocol;
    }

    @Override
    public void run() {
        try (Socket sock = this.sock) { //just for automatic closing
            int read;

            in = new BufferedInputStream(sock.getInputStream());
            out = new BufferedOutputStream(sock.getOutputStream());
            int counter = 0;
            while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
                System.out.println("read is: " + read);
                T nextMessage = encdec.decodeNextByte((byte) read);
                System.out.println("entered " + counter);
                counter++;
                if (nextMessage != null) {
                    byte[] message = (byte[])nextMessage;
                    // for(int i = 0; i < message.length; i++) {
                    //     System.out.println("message[" + i + "] is: " + message[i]);
                    // }
                    String fileName = new String(message,0,message.length);
                    System.out.println("entered protocol and message is: " + fileName);
                    protocol.process(nextMessage);
                    // if (response != null) {
                    //     out.write(encdec.encode(response));
                    //     out.flush();
                    // }
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public void close() throws IOException {
        connected = false;
        sock.close();
    }

    @Override
    public void send(T msg) {
        //IMPLEMENT IF NEEDED
        // try (Socket sock = this.sock) {
        //     out = new BufferedOutputStream(sock.getOutputStream());
        if(msg!=null){
            try {
                out.write(encdec.encode(msg));
                out.flush(); 
            } catch(IOException e) {
                e.printStackTrace();
            }
            
        }
         
        // } catch (IOException ex) {
        //     ex.printStackTrace();
        // }
    }
}
