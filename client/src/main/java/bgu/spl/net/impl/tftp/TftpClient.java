package bgu.spl.net.impl.tftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.impl.tftp.TftpEncoderDecoder;

public class TftpClient {

    private static boolean shouldTerminate = false;
    private static Socket socket;
    //TODO: implement the main logic of the client, when using a thread per client the main logic goes here
    public static <T> void main(String[] args) throws UnknownHostException, IOException {
        //  if (args.length == 0) {
        //     args = new String[]{"localhost", "hello"};
        // }

        // if (args.length < 2) {
        //     System.out.println("you must supply two arguments: host, message");
        //     System.exit(1);
        // }

        //BufferedReader and BufferedWriter automatically using UTF-8 encoding
        final MessageEncoderDecoder<byte[]> encdec = (MessageEncoderDecoder<byte[]>) new TftpEncoderDecoder();    
        //final BidiMessagingProtocol<byte[]> protocol = new TftpProtocol();
        socket = new Socket(args[0], Integer.parseInt(args[1]));

        try (Socket sock = socket;
        BufferedInputStream in = new BufferedInputStream(sock.getInputStream());
        BufferedOutputStream out = new BufferedOutputStream(sock.getOutputStream())) {
                // final KeyboardHandler<byte[]> keyboardHandler = new KeyboardHandler<>(protocol,sock, encdec);
                // final BlockingConnectionHandler<byte[]> connectionHandler = new BlockingConnectionHandler<>(sock, encdec, protocol);
                // Thread KeyboardThread = new Thread(keyboardHandler);
                // Thread ListeningThread = new Thread(connectionHandler);
                // KeyboardThread.start();
                // ListeningThread.start();
                // System.out.println("Client is connected");
                // try {
                //     KeyboardThread.join();
                //     ListeningThread.join();
                // } catch (InterruptedException e) {
                //     // TODO Auto-generated catch block
                //     e.printStackTrace();
                // }
                
            Thread keyboardThread = new Thread(() -> {
                BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));    
                System.out.println("sending message to server");
                String userInputStr;
                try {
                    while (!shouldTerminate && (userInputStr = userInput.readLine()) != null) {
                        String output = processKeyboard(userInputStr);       
                        if(output!=null) {
                            // for(int i = 0; i < output.length(); i++) {
                            //     System.out.println(output.charAt(i)+ " ");
                            // }
                            // for(int i = 0; i < output.getBytes().length; i++) {
                            //     System.out.print(output.getBytes()[i] + " ");
                            // }
                            
                            out.write(encdec.encode(output.getBytes()));
                            //out.newLine();
                            out.flush();
                        }
                    }
                    System.out.println("keyboard thread finished");
                } catch(IOException e) {
                    e.printStackTrace();
                }
                
            });

              // Start listening thread
            Thread listenThread = new Thread(() -> {
                int read;
                try {
                    while (!shouldTerminate && (read = in.read()) >= 0) {
                        byte[] nextMessage = encdec.decodeNextByte((byte) read);
                    //while ((line = in.readLine()) != null) { // not good
                        if (nextMessage != null) {
                            byte[] output = processListen(nextMessage);
                            if(output != null) {
                                //String theOutput = new String(output,0,output.length);
                                out.write(encdec.encode(output));
                                out.flush();
                            }
                        }
                        //byte[] message = line.getBytes();
                    }
                    System.out.println("listen thread finished");
                } 
                catch (IOException e) {
                    e.printStackTrace();
                }
        });
        
        listenThread.start();
        keyboardThread.start();
        
        try {
            listenThread.join();
            keyboardThread.join();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
    } }
}

    public boolean shouldTerminate() {
        return shouldTerminate;
    }
    public static void terminate() {
        shouldTerminate = true;
        try {
            socket.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    private static File wrqFile;
    private static short blockNumberToInt = 0;
    private static boolean isInRRQ = false;
    private static File rrqFile;
    private static boolean isInDIRQ = false;
    private static String lastCommand = "";

    


    public static String processKeyboard(String userInputStr) {
        String[] userArr = userInputStr.split(" ");
        // System.out.println("userArr[0] is:" + userArr[0]);
        if(userArr[0].equals("DISC") | userArr[0].equals("DIRQ")) {
            byte[] output = new byte[2];
            if(userArr[0].equals("DISC")) {
                output[0] = 0;
                output[1] = 10;
                lastCommand = "DISC";
            } else {
                output[0] = 0;
                output[1] = 6;
                isInDIRQ = true;
                lastCommand = "DIRQ";
            }
            String theOutput = "";
            try {
                theOutput = new String(output, "UTF-8");
                
            } catch (UnsupportedEncodingException e) {}           
            return theOutput;
        } else if(userArr[0].equals("LOGRQ") | userArr[0].equals("DELRQ") | userArr[0].equals("RRQ") | userArr[0].equals("WRQ")){
            int userInputLength = userArr[1].getBytes().length;          
            byte[] output  = new byte[userInputLength+3];
            output[0] = 0;
            if(userArr[0].equals("LOGRQ")) {
               output[1] = 7; 
               lastCommand = "LOGRQ";
            }
                
            else if(userArr[0].equals("DELRQ")) {
                output[1] = 8;
                lastCommand = "DELRQ";
            }
            else if(userArr[0].equals("RRQ")) {
                Path folder = Paths.get("./");
                Path filePath = folder.resolve(userArr[1]);
                try {
                    if(!Files.exists(filePath)) {
                        Files.createFile(filePath);
                        output[1] = 1;
                        isInRRQ = true;
                        rrqFile = filePath.toFile();
                        lastCommand = "RRQ";
                    } else {
                        System.out.println("File already exists");
                        return null;
                    }
                    
                } catch (IOException e) {}   
            }  
                // create file with the name of the file
            else if(userArr[0].equals("WRQ")) {
                    Path folder = Paths.get("./");
                    Path filePath = folder.resolve(userArr[1]);
                    if (Files.exists(filePath)) {
                        wrqFile = filePath.toFile();   
                        output[1] = 2;
                        lastCommand = "WRQ";
                    } else {
                        System.out.println("File does not exist");
                        return null;
                    }
            }
                System.arraycopy(userArr[1].getBytes(), 0, output, 2, userInputLength);
                output[output.length-1] = 0;
                // for(int i = 0; i < output.length; i++) {
                //     System.out.print(output[i] + " ");
                // }
                String theOutput = "";
                try {
                    theOutput = new String(output, "UTF-8");
                } catch (UnsupportedEncodingException e) {}
            return theOutput;
        } else { // output not valid     
            // System.out.println("userArr[0] is:" + userArr[0]);
            System.out.println("Invalid input");
            return null;
        }
    
    }

    public static byte[] processListen(byte[] message) {
        // for(int i = 0; i < message.length; i++) {
        //     System.out.println("message[" + i + "] is: " + message[i]);
        // }
        if(message.length == 4 && message[1] == 4) { // ACK
            if(message[3] == 0 && !lastCommand.equals("WRQ") && !lastCommand.equals("DISC")) {
                System.out.println("ACK " + message[2]);
            } else if(lastCommand.equals("DISC")) {
                System.out.println("ACK " + message[2]);
                terminate();
            }
            else { // ACK DATA
                byte[] blockNumber = {message[2], message[3]};
                // System.out.print("blockNumber[0] is: " + blockNumber[0] + " blockNumber[1] is: " + blockNumber[1]);
                //System.out.print("blockNumber[0]: " + blockNumber[0] + " blockNumber[1]: " + blockNumber[1] + " " );
                short blockNumberCast = (short)(((short)((blockNumber[0] & 0xFF)) << 8) | (short)(blockNumber[1] & 0xFF));
                // System.out.print("blockNumberCast: " + blockNumberCast + " ");
                System.out.println("ACK " + blockNumberCast);

                if(wrqFile != null) { // ACK for WRQ
                    int i =  blockNumberToInt*512;
                    blockNumberToInt++;
                    
                   // short blockNumberCast = (short)(((short)((blockNumber[0] & 0xFF)) << 8) | (short)(blockNumber[1] & 0xFF));
                    blockNumberCast++;
                    //System.out.print("increment: " + blockNumberCast);
                    blockNumber[0] = (byte) ((blockNumberCast >> 8) & 0xFF); 
                    blockNumber[1] = (byte) (blockNumberCast & 0xFF); 
                    //System.out.println("blocknumber: " + blockNumber[0] + " " + blockNumber[1]);
                    //System.out.println(" casting: " + blockNumber[0] + " " + blockNumber[1]);
                    // if(blockNumber[1] == 127) { 
                    //     blockNumber[0]++;
                    //     blockNumber[1] = 0;
                    // } 
                    byte[] data;
                    try (FileInputStream fileToRead = new FileInputStream(wrqFile)) {
                        data = new byte[(int) wrqFile.length()];
                        fileToRead.read(data);
                        byte[] packetToSend = new byte[Math.min(data.length-i+6,518)]; // 512 size of data + 7 more places
                        packetToSend[0] = 0;
                        packetToSend[1] = 3;
                        packetToSend[2] = (byte) ((packetToSend.length-6 >> 8) & 0xFF);
                        packetToSend[3] = (byte) (packetToSend.length-6 & 0xFF);     
                        packetToSend[4] = blockNumber[0];
                        packetToSend[5] = blockNumber[1];  
                        System.arraycopy(data, i, packetToSend, 6, packetToSend.length-6);
                        if(packetToSend.length != 518 || i == (int)(wrqFile.length())-512) {
                            blockNumberToInt = 0;
                            System.out.println("WRQ " + wrqFile.getName() + " complete");
                            wrqFile = null;
                        }
                        //System.out.println("blockNumber[0] is: " + packetToSend[4] + " blockNumber[1] is: " + packetToSend[5]);
                        return packetToSend;
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    
                    }
            }

        } else if(message.length > 2 && message[1] == 9) { // BCAST
            // for(int i = 0; i < message.length; i++) {
            //     System.out.print(message[i] + " ");
            // }
            byte[] fileName = new byte[message.length-4];
            System.arraycopy(message, 3, fileName, 0, fileName.length);
            String fileNameStr = "";
            try {
                fileNameStr = new String(fileName, "UTF-8");
            } catch (UnsupportedEncodingException e) {}
            System.out.println("BCAST " + (message[2] == 1 ? "add" : "del") + " " + fileNameStr);
            byte[] ackData = {0,4,0,0};
            return ackData;
        } else if(message.length > 2 && message[1] == 5) { // ERROR
            // // ERROR there is a bug here
            if(lastCommand.equals("DISC")) {
                terminate();
            }
            System.out.println("Error " + message[2] + message[3]);
            return null;
        } else if(message.length > 2 && message[1] == 3) { // DATA(DIRQ or RRQ)
            int packetSize = (message[2] << 8) | (message[3] & 0xFF);   
            if(isInRRQ) {  // RRQ
                try (FileOutputStream fileToCreate = new FileOutputStream(rrqFile, true)) {
                    fileToCreate.write(Arrays.copyOfRange(message, 6, message.length));
                } catch (IOException e) {
                e.printStackTrace();}

                byte[] ackData = {0,4,message[4],message[5]};

                if(packetSize < 512) {
                    System.out.println("RRQ " + rrqFile.getName()  + " complete");
                    rrqFile = null;
                    isInRRQ = false;
                    
                }
                return ackData;
            } else if(isInDIRQ) { // DIRQ
                String fileName = "";
                for (int i = 6; i < packetSize+6; i++) {
                    byte b = message[i];
                    if (b == 0) {
                        System.out.println(fileName);
                        fileName = "";
                    } else {  
                        fileName += (char) b;
                    }
                }
                System.out.println(fileName);
                byte[] ackData = {0,4,message[4],message[5]};
                if(packetSize < 512) {
                    isInDIRQ = false;
                }
                return ackData;

            }
            
        }
        return null;
    }

    
}
