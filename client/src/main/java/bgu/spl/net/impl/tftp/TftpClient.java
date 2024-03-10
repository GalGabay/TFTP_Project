package bgu.spl.net.impl.tftp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;

public class TftpClient {
    //TODO: implement the main logic of the client, when using a thread per client the main logic goes here
    public static void main(String[] args) throws UnknownHostException, IOException {
        //  if (args.length == 0) {
        //     args = new String[]{"localhost", "hello"};
        // }

        // if (args.length < 2) {
        //     System.out.println("you must supply two arguments: host, message");
        //     System.exit(1);
        // }

        //BufferedReader and BufferedWriter automatically using UTF-8 encoding


        try (Socket sock = new Socket(args[0], 7777);
            BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()))) {

            Thread keyboardThread = new Thread(() -> {
                BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));    
                System.out.println("sending message to server");
                String userInputStr;
                try {
                    while ((userInputStr = userInput.readLine()) != null) {
                        String[] userArr = userInputStr.split(" ");
                        if(userArr[0] == "DISC" | userArr[0] == "DIRQ") {
                            byte[] output = new byte[2];
                            if(userArr[0] == "DISC") {
                                output[0] = 0;
                                output[1] = 10;
                            } else {
                                output[0] = 0;
                                output[1] = 6;
                            }
                            String theOutput = new String(output, "UTF-8");
                            out.write(theOutput);
                            out.newLine();
                            out.flush();
        
                        } else if(userArr[0] == "LOGRQ" | userArr[0] == "DELRQ" | userArr[0] == "RRQ" | userArr[0] == "WRQ"){
                            int userInputLength = userArr[1].getBytes().length;
                            byte[] output  = new byte[userInputLength+3];
                            output[0] = 0;
                            if(userArr[0] == "LOGRQ")
                                output[1] = 7;
                            else if(userArr[0] == "DELRQ")
                                output[1] = 8;
                            else if(userArr[0] == "RRQ") {
                                output[1] = 1;
                                Path folder = Paths.get("./");
                                Path filePath = folder.resolve(userArr[1]);
                                if (Files.exists(filePath)) {
                                    try {
                                        Files.createFile(filePath);
                                    } catch (IOException e) {}   
                            }}
                                
                                // create file with the name of the file
                             
                                
                            else if(userArr[0] == "WRQ")
                                output[1] = 2;
                            System.arraycopy(userArr[1].getBytes(), 0, output, 2, userInputLength);
                            output[output.length-1] = 0;
                            String theOutput = new String(output, "UTF-8");
                            out.write(theOutput);
                            out.newLine();
                            out.flush();
                        } else { // output not valid
        
                        }
                    
                    }
                } catch(IOException e) {
                    e.printStackTrace();
                }
                
            });

              // Start listening thread
        Thread listenThread = new Thread(() -> {
            String line;
            try {
                while ((line = in.readLine()) != null) { // not good
                    byte[] message = line.getBytes();
                    if(message.length > 2 && message[1] == 4) { // ACK
                        if(message.length == 4) {
                            System.out.println("ACK " + message[2] + message[3]);
                        } else if(message.length == 3) {
                            System.out.println("ACK " + message[2]);
                        }
                    } else if(message.length > 2 && message[1] == 9) { // BCAST
                        byte[] fileName = new byte[message.length-4];
                        System.arraycopy(message, 3, fileName, 0, fileName.length);
                        String fileNameStr = new String(fileName, "UTF-8");
                        System.out.println("BCAST " + (message[2] == 1 ? "add" : "del") + " " + fileNameStr);
                    } else if(message.length > 2 && message[1] == 5) { // ERROR
                        System.out.println("ERROR " + message[2] + message[3]);
                    } else if(message.length > 2 && message[1] == 5) { // DATA
                        // TODO: implement
                        
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        
        listenThread.start();
        keyboardThread.start();
        } 
    }
}
