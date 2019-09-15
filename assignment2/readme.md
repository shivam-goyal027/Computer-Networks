First compile the code by the following command:
for unencrypted mode: ./run.sh 1
for encrypted mode: ./run.sh 2
for encrypted with signatures mode: ./run.sh 3 

Now run the server with the command: java TCPServer

After this multiple clients can be opened up to connect to the server as the server can accomodate multiple client.
They can be run by: java TCPClient

After running TCPClient, it will ask for the username and the ip address. Any well-formed username that is not already registered will work, if ill-formed is given then server will ask to type a valid one. The ip address==127.0.0.1 is valid to connect to the localhost.

After registration messages received by the server, the client can send messages to each other in the way specified in the assignment statement. Exceptions are handled if some invalid message is sent.
