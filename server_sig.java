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

			// System.out.println(usernamesend);
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
				System.out.println(usernamesend + " Is well-formed");
			}else{
				outputsend = "ERROR 100 Malformed username" + '\n';
				System.out.println(usernamesend + " Is not well-formed");
			}

			
			SocketThreadSend socketThreadsend = new SocketThreadSend(connectionSocketSend, inFromClientSend, outToClientSend, outputsend, usernamesend);
	
			Thread thread_send = new Thread(socketThreadsend);
			thread_send.start();
			// Checked

			//Sender registration done and receiver registration starts
			Socket connectionSocketRecv = welcomeSocket.accept(); 
			// System.out.println("ok1");
			BufferedReader inFromClientRecv = new BufferedReader(new InputStreamReader(connectionSocketRecv.getInputStream())); 
			// System.out.println("ok2");
			DataOutputStream outToClientRecv = new DataOutputStream(connectionSocketRecv.getOutputStream()); 
			// System.out.println("ok3");
			String usernamerecv = register(inFromClientRecv, "recv");
			// System.out.println("usernamerecv " + usernamerecv);
			if(!inFromClientRecv.readLine().equals("")) break;
			String outputrecv = "";

			//System.out.println("ok1");
			if(well_formed(usernamerecv) && !(TCPServer.hashtable.containsKey(usernamerecv))){
				outputrecv = "REGISTERED TORECV " + usernamerecv + '\n';
			}else{
				outputrecv = "ERROR 100 Malformed username\n";
				//System.out.println(outputrecv);
			}
			//System.out.println("ok2");

			SocketThreadRecv socketThreadrecv = new SocketThreadRecv(connectionSocketRecv, inFromClientRecv, outToClientRecv, outputrecv, usernamerecv);

			Thread thread_recv = new Thread(socketThreadrecv);
			thread_recv.start();
			hashtable.put(usernamerecv, connectionSocketRecv);
			//thread_recv.stop();
	
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
	   	//System.out.println("HERE IS "+output);
		   	if(output.length()>=17){
		   		//System.out.println("HERE");
		   		if(output.equals("ERROR 100 Malformed username\n")){
						outToClient.writeBytes(output+'\n');
						break;
				}
		   		if(output.substring(0,17).equals("REGISTERED TORECV")){
		   			//System.out.println("AND HERE");
		   			outToClient.writeBytes(output+'\n');
	    			break;
		   		}
	    	}
	    	//System.out.println("OR HERE");
	    	
	   } catch(Exception e) {
			try {
				System.out.println("Connection Error3");
				connectionSocket.close();
			} catch(Exception ee) { }
			break;
	   }
        } 
    }
}


class SocketThreadSend implements Runnable {
	String sentence; 
	String signature;
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
				if(output.length()>=17 && flag==0){
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
				sentence = inFromClient.readLine();
				System.out.println("This "+sentence);

				if(sentence.equals(""))
					sentence = inFromClient.readLine();
				System.out.println("This1 "+sentence);
				if(sentence.substring(0,5).equals("FETCH")){
					System.out.println("Got here");
					if(!(TCPServer.hashtable2.containsKey(sentence.substring(6)))){
									System.out.println("Errorrrrrr");
									outToClient.writeBytes("No User with username "+sentence.substring(6)+'\n'+'\n');
									continue;
								}
					else{
						System.out.println("Good");
						outToClient.writeBytes(TCPServer.hashtable2.get(sentence.substring(6))+'\n'+'\n');
					}
				}
				System.out.println("Got here too");
				signature=inFromClient.readLine();
				System.out.println("Got here toooooo "+signature);
				if(signature.equals(""))
					signature = inFromClient.readLine();
				System.out.println("Got here too1 "+ signature);
				sentence=inFromClient.readLine();
				if(sentence.equals(""))
					sentence = inFromClient.readLine();
				System.out.println("This2 "+sentence);
				int count = 0;
				if((sentence.substring(0, 4)).equals("SEND") && TCPServer.well_formed(sentence.substring(5))){
					// System.out.println("Maybe here ");
					System.out.println("A:"+sentence);
					username = sentence.substring(5);
					sentence = inFromClient.readLine(); 
					System.out.println("B:"+sentence);
					if(sentence.substring(0, 16).equals("Content-length: ")){
						count = Integer.parseInt(sentence.substring(16));
						sentence = inFromClient.readLine(); 
						System.out.print("C:"+sentence);
						if(sentence.equals("")){
							sentence = inFromClient.readLine(); 
							System.out.println("message:"+sentence);
							//if(sentence.length()==count){
								if(!(TCPServer.hashtable.containsKey(username))){
									outToClient.writeBytes("No User with username "+username+'\n'+'\n');
									continue;
								}

								Socket socket = TCPServer.hashtable.get(username);
								
								DataOutputStream outToClientrecv = new DataOutputStream(socket.getOutputStream());
								
								BufferedReader inFromClientrecv = new BufferedReader(new InputStreamReader(socket.getInputStream()));
								
								//System.out.print("FORWARD "+usernamesend+'\n');
								outToClientrecv.writeBytes(signature+'\n');
								outToClientrecv.writeBytes("FORWARD "+usernamesend+'\n');
	    						outToClientrecv.writeBytes("Content-length: "+ count+'\n');
	    						outToClientrecv.writeBytes("\n");
	    						System.out.println("E");
	    						outToClientrecv.writeBytes(sentence+'\n');
	    						System.out.println("F");
	    						sentence=inFromClientrecv.readLine();
	    						if(sentence.equals(""))
									sentence = inFromClientrecv.readLine(); 

								if(sentence.substring(0,5).equals("FETCH")){
									System.out.println("Got here");
									if(!(TCPServer.hashtable2.containsKey(sentence.substring(6)))){
										System.out.println("Errorrrrrr");
										outToClientrecv.writeBytes("No User with username "+sentence.substring(6)+'\n'+'\n');
										continue;
									}
									else{
										System.out.println("Good");
										outToClientrecv.writeBytes(TCPServer.hashtable2.get(sentence.substring(6))+'\n'+'\n');
									}
								}
	    						sentence=inFromClientrecv.readLine();
	    						System.out.println("G:"+sentence);
	    						String temp = inFromClientrecv.readLine();
	    						System.out.println("temp: "+sentence);

	    						while(true){
	    							System.out.println("I");
	    							System.out.println("Check "+sentence);
		    						if(sentence.equals("RECEIVED "+usernamesend)){
		    							System.out.println("H");
		    							break;
		    						}
		    						//break;
	    						}
	    						outToClient.writeBytes("SENT "+username+'\n'+'\n');
							//}
						}
					}
				}
			} 
			catch(Exception e) {
				try {
					System.out.println("Connection Error1");
					connectionSocket.close();
				} 
				catch(Exception ee) { System.out.println("Connection Error2");}
				break;
			}
		} 
    }
}
