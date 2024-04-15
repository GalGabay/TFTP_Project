# TFTP Project

## ABOUT:
Welcome to our own implementation of the well known TFTP(Trivial File Transfer Protocol).
The server and client are implemented in java using the TPC(thread-per-client) design patters.
The clients can login to the server, and it supports reading, writing, deleting and getting files.

## HOW TO RUN:
1. Windows: Be sure you have maven installed on your computer. then just press "run" in your IDE.
2. Linux: mvn compile.
   for the server: mvn exec:java -Dexec.mainClass="bgu.spl.net.impl.tftp.TftpServer" -Dexec.args="<port>"
   for the client: mvn exec:java -Dexec.mainClass="bgu.spl.net.impl.stomp.tftp.TftpClient" -Dexec.args="<ip> <port>" ( change the ip and the port)
3. login using command "LOGRQ *username*. Then you have these commands you can work with:
     a. RRQ *filename* - reading a file from the server.
     b. WRQ *filename* - writing a file into the server.
     c. DELRQ *filename* - deleting a file from the server.
     d. DIRQ - listing all current files in the server.
     e. DISC - disconnecting the current client from the server
