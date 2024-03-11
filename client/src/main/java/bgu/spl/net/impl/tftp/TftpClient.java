package bgu.spl.net.impl.tftp;

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

public class TftpClient {


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


        try (Socket sock = new Socket(args[0], 7777);
            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()))) {

            Thread keyboardThread = new Thread(() -> {
                BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));    
                System.out.println("sending message to server");
                String userInputStr;
                try {
                    while ((userInputStr = userInput.readLine()) != null) {
                        String output = processKeyboard(userInputStr);
                        out.write(output);
                        out.newLine();
                        out.flush();
                    }
                } catch(IOException e) {
                    e.printStackTrace();
                }
                
            });

              // Start listening thread
            Thread listenThread = new Thread(() -> {
                int read;
                try {
                    while ((read = in.read()) >= 0) {
                        byte[] nextMessage = encdec.decodeNextByte((byte) read);
                    //while ((line = in.readLine()) != null) { // not good
                        if (nextMessage != null) {
                            byte[] output = processListen(nextMessage);
                            if(output != null) {
                                String theOutput = new String(output,0,output.length);
                                out.write(theOutput);
                                out.flush();
                            }
                        }
                        //byte[] message = line.getBytes();
                    }
                } 
                catch (IOException e) {
                    e.printStackTrace();
                }
        });
        
        listenThread.start();
        keyboardThread.start();
        } 
    }

    private static File wrqFile;
    private static int blockNumberToInt = 0;
    private static boolean isInRRQ = false;
    private static File rrqFile;
    private static boolean isInDIRQ = false;

    public static String processKeyboard(String userInputStr) {
        String[] userArr = userInputStr.split(" ");
        if(userArr[0] == "DISC" | userArr[0] == "DIRQ") {
            byte[] output = new byte[2];
            if(userArr[0] == "DISC") {
                output[0] = 0;
                output[1] = 10;
            } else {
                output[0] = 0;
                output[1] = 6;
                isInDIRQ = true;
            }
            String theOutput = "";
            try {
                theOutput = new String(output, "UTF-8");
                
            } catch (UnsupportedEncodingException e) {}           
            return theOutput;
        } else if(userArr[0] == "LOGRQ" | userArr[0] == "DELRQ" | userArr[0] == "RRQ" | userArr[0] == "WRQ"){
            int userInputLength = userArr[1].getBytes().length;
            byte[] output  = new byte[userInputLength+3];
            output[0] = 0;
            if(userArr[0] == "LOGRQ")
                output[1] = 7;
            else if(userArr[0] == "DELRQ")
                output[1] = 8;
            else if(userArr[0] == "RRQ") {

                Path folder = Paths.get("./");
                Path filePath = folder.resolve(userArr[1]);
                if (Files.exists(filePath)) {
                    try {
                        Files.createFile(filePath);
                        output[1] = 1;
                        isInRRQ = true;
                        rrqFile = filePath.toFile();
                    } catch (IOException e) {}   
                } else {
                    System.out.println("File does not exist");
                    return null;
                }
            }
                
                // create file with the name of the file
            else if(userArr[0] == "WRQ") {
               
                    Path folder = Paths.get("./");
                    Path filePath = folder.resolve(userArr[1]);
                    if (Files.exists(filePath)) {
                        wrqFile = filePath.toFile();   
                        output[1] = 2;
                    } else {
                        System.out.println("File does not exist");
                        return null;
                    }
            }
                System.arraycopy(userArr[1].getBytes(), 0, output, 2, userInputLength);
                output[output.length-1] = 0;
                String theOutput = "";
                try {
                    theOutput = new String(output, "UTF-8");
                } catch (UnsupportedEncodingException e) {}
            return theOutput;
        } else { // output not valid     
            System.out.println("Invalid input");
            return null;
        }
    
    }

    public static byte[] processListen(byte[] message) {
        if(message.length == 4 && message[1] == 4) { // ACK
            System.out.println("ACK " + message[2] + message[3]);
            if(wrqFile != null) { // ACK for WRQ
                int i = blockNumberToInt*512;
                blockNumberToInt++;
                byte[] blockNumber = {message[2], message[3]};
                if(blockNumber[1] == 127) { 
                    blockNumber[0]++;
                    blockNumber[1] = 0;
                } else {
                    blockNumber[1]++;
                }
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
                        wrqFile = null;
                    }
                    return packetToSend;
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
                }
        } else if(message.length > 2 && message[1] == 9) { // BCAST
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
            System.out.println("ERROR " + message[2] + message[3]);
        } else if(message.length > 2 && message[1] == 3) { // DATA(DIRQ or RRQ)
            int packetSize = (message[2] << 8) | (message[3] & 0xFF);   
            if(isInRRQ) {  // RRQ
                try (FileOutputStream fileToCreate = new FileOutputStream(wrqFile, true)) {
                    fileToCreate.write(Arrays.copyOfRange(message, 6, message.length));
                } catch (IOException e) {
                e.printStackTrace();}

                byte[] ackData = {0,4,message[4],message[5]};

                if(packetSize < 512) {
                    rrqFile = null;
                    isInRRQ = false;
                }
                return ackData;
            } else if(isInDIRQ) { // DIRQ
                String fileName = "";
                for (int i = 6; i < message.length; i++) {
                    byte b = message[i];
                    if (b == 0) {
                        System.out.println(fileName);
                        fileName = "";
                    } else {
                        fileName += (char) b;
                    }
                }
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
