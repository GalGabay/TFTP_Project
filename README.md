HOW TO RUN:
1. Windows: Be sure you have maven installed on your computer. then just press "run" in your IDE.
2. Linux: mvn compile.
   for the server: mvn exec:java -Dexec.mainClass="bgu.spl.net.impl.tftp.TftpServer" -Dexec.args="<port>"
   for the client: mvn exec:java -Dexec.mainClass="bgu.spl.net.impl.stomp.tftp.TftpClient" -Dexec.args="<ip> <port>" ( change the ip and the port)
