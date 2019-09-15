import java.io.*; 
import java.net.*; 
import java.util.Hashtable;
import java.lang.String;


class TCPServer{ 
	public static Hashtable<String, Socket> hashtable = new Hashtable<String, Socket>(); 
	public static Hashtable<String, String> hashtable2 = new Hashtable<String, String>(); 
	public static Boolean well_formed(String str){
		String regex = "^[a-zA-Z0-9]+$";
		return (str.matches(regex));
	}

	public static String register(BufferedReader inFromClient, String strd){
		String str="";

		try{
			str = inFromClient.readLine(); 
		} catch(Exception e){
			System.out.println("Connection Error");
		}

		String subStr = str.substring(0, 15);

		if(subStr.equals("REGISTER TOSEND") && strd.equals("send")){
			String username = str.substring(16);
			return username;
		}
		else if(subStr.equals("REGISTER TORECV") && strd.equals("recv")){
			String username = str.substring(16);
			return username;
		}
		else return "";
	}


	public static void main(String argv[]) throws Exception { 
    	ServerSocket welcomeSocket = new ServerSocket(5000);

      	while(true) { 
      		Socket connectionSocketSend = welcomeSocket.accept(); 
			BufferedReader inFromClientSend = new BufferedReader(new InputStreamReader(connectionSocketSend.getInputStream())); 
			DataOutputStream outToClientSend = new DataOutputStream(connectionSocketSend.getOutputStream()); 

			String usernamesend = register(inFromClientSend,"send");

			if(!inFromClientSend.readLine().equals("")){
				System.out.println("Illegal Request");
				break;
			}
			String outputsend = "";

			if(well_formed(usernamesend) && !(TCPServer.hashtable.containsKey(usernamesend))){
				outputsend = "REGISTERED TOSEND " + usernamesend + '\n';
				String keys=inFromClientSend.readLine();
				if(inFromClientSend.readLine().equals(""))
					hashtable2.put(usernamesend,keys);
				else{
					System.out.println("Could not fetch public key");
				}
				//System.out.println(usernamesend + " Is well-formed");
			}else{
				outputsend = "ERROR 100 Malformed username" + '\n';
				//System.out.println(usernamesend + " Is not well-formed");
			}

			
			SocketThreadSend socketThreadsend = new SocketThreadSend(connectionSocketSend, inFromClientSend, outToClientSend, outputsend, usernamesend);
	
			Thread thread_send = new Thread(socketThreadsend);
			thread_send.start();
			
			if(outputsend.equals("ERROR 100 Malformed username\n"))
				continue;

			//Sender registration done and receiver registration starts
			Socket connectionSocketRecv = welcomeSocket.accept(); 
			BufferedReader inFromClientRecv = new BufferedReader(new InputStreamReader(connectionSocketRecv.getInputStream())); 
			DataOutputStream outToClientRecv = new DataOutputStream(connectionSocketRecv.getOutputStream()); 
			String usernamerecv = register(inFromClientRecv, "recv");
			if(!inFromClientRecv.readLine().equals("")) break;
			String outputrecv = "";

			if(well_formed(usernamerecv) && !(TCPServer.hashtable.containsKey(usernamerecv))){
				outputrecv = "REGISTERED TORECV " + usernamerecv + '\n';
			}else{
				outputrecv = "ERROR 100 Malformed username\n";
			}

			SocketThreadRecv socketThreadrecv = new SocketThreadRecv(connectionSocketRecv, inFromClientRecv, outToClientRecv, outputrecv, usernamerecv);

			Thread thread_recv = new Thread(socketThreadrecv);
			thread_recv.start();
			hashtable.put(usernamerecv, connectionSocketRecv);
	
		}
    } 
} 


class SocketThreadRecv implements Runnable {
     String clientSentence;
     Socket connectionSocket;
     String output;
     BufferedReader inFromClient;
     DataOutputStream outToClient;
   	 private boolean exit;
   	 String usernamerecv;
     SocketThreadRecv (Socket connectionSocket, BufferedReader inFromClient, DataOutputStream outToClient, String output, String usernamerecv){
		this.connectionSocket = connectionSocket;
        this.inFromClient = inFromClient;
        this.outToClient = outToClient;
        this.output = output;
        this.usernamerecv=usernamerecv;
        exit=false;
     } 

     public void run() {
       while(true) { 
	   try {
		   	if(output.length()>=17){
		   		if(output.equals("ERROR 100 Malformed username\n")){
						outToClient.writeBytes(output+'\n');
						break;
				}
		   		if(output.substring(0,17).equals("REGISTERED TORECV")){
		   			outToClient.writeBytes(output+'\n');
	    			break;
		   		}
	    	}
	    	
	   } catch(Exception e) {
			try {
				System.out.println("Connection Error");
				connectionSocket.close();
			} catch(Exception ee) { }
			break;
	   }
        } 
    }
}


class SocketThreadSend implements Runnable {
	String sentence; 
	Socket connectionSocket;
	String username;
	BufferedReader inFromClient;
	DataOutputStream outToClient;
    String output;
    String usernamesend;
    int flag;
    String temp;
	SocketThreadSend (Socket connectionSocket, BufferedReader inFromClient, DataOutputStream outToClient, String output,String usernamesend){
		this.connectionSocket = connectionSocket;
		this.inFromClient = inFromClient;
		this.outToClient = outToClient;
		this.output=output;
		this.usernamesend=usernamesend;
		this.flag = 0;
	} 

    public void run() {
		while(true) { 
			try {
				if(flag==0){
				if(output.length()>=17){
					if(output.equals("ERROR 100 Malformed username\n")){
						outToClient.writeBytes(output+'\n');
						break;
					}
					if(output.substring(0,17).equals("REGISTERED TOSEND")){
		   				outToClient.writeBytes(output+'\n');
		   				flag = 1;
	    				continue;
		   			}
		   		}
		   	}
				sentence = inFromClient.readLine();

				if(sentence.equals(""))
					sentence = inFromClient.readLine();
				if(sentence.substring(0,5).equals("FETCH")){
					if(!(TCPServer.hashtable2.containsKey(sentence.substring(6)))){
									outToClient.writeBytes("No User with username "+sentence.substring(6)+'\n'+'\n');
									continue;
								}
					else{
						outToClient.writeBytes(TCPServer.hashtable2.get(sentence.substring(6))+'\n'+'\n');
					}
				}
				sentence=inFromClient.readLine();
				if(sentence.equals(""))
					sentence = inFromClient.readLine();
				int count = 0;
				if((sentence.substring(0, 4)).equals("SEND")){
					// System.out.println("A:"+sentence);
					username = sentence.substring(5);
					sentence = inFromClient.readLine(); 
					// System.out.println("B:"+sentence);
					if(sentence.substring(0, 16).equals("Content-length: ")){
						count = Integer.parseInt(sentence.substring(16));
						sentence = inFromClient.readLine(); 
						// System.out.print("C:"+sentence);
						if(sentence.equals("")){
							sentence = inFromClient.readLine(); 
							System.out.println("message:"+sentence);
								if(!(TCPServer.hashtable.containsKey(username))){
									outToClient.writeBytes("No User with username "+username+'\n'+'\n');
									continue;
								}

								Socket socket = TCPServer.hashtable.get(username);
								
								DataOutputStream outToClientrecv = new DataOutputStream(socket.getOutputStream());
								
								BufferedReader inFromClientrecv = new BufferedReader(new InputStreamReader(socket.getInputStream()));
								
								outToClientrecv.writeBytes("FORWARD "+usernamesend+'\n');
	    						outToClientrecv.writeBytes("Content-length: "+ count+'\n');
	    						outToClientrecv.writeBytes("\n");
	    						outToClientrecv.writeBytes(sentence+'\n');
	    						sentence=inFromClientrecv.readLine();
	    						// System.out.println("G:"+sentence);
	    						String temp = inFromClientrecv.readLine();
	    						while(true){
	    							// System.out.println("Check "+sentence);
		    						if(sentence.equals("RECEIVED "+usernamesend)){
		    							break;
		    						}
		    						//break;
	    						}
	    						outToClient.writeBytes("SENT "+username+'\n'+'\n');
						}
					}
				}
			} 
			catch(Exception e) {
				try {
					System.out.println("Connection Error");
					connectionSocket.close();
				} 
				catch(Exception ee) { System.out.println("Connection Error");}
				break;
			}
		} 
    }
}
