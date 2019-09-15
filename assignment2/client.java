import java.io.*; 
import java.net.*; 
class TCPClient { 

    public static void main(String argv[]) throws Exception 
    { 
        String sentence; 
        String modifiedSentence;
        String username;
        String hostname="";
        BufferedReader inFromUserSend;
		DataOutputStream outToServerSend;
		BufferedReader inFromServerSend;
		Socket clientSendSocket;
		int i=0;
    while(true){
    	try{

		System.out.print("Enter username: ");

		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in)); 
		
		username=inFromUser.readLine();
		if(i==0){
			System.out.print("Enter host's IP address: ");
			inFromUser = new BufferedReader(new InputStreamReader(System.in));
			hostname=inFromUser.readLine();
		}

		try{
			clientSendSocket = new Socket(hostname, 5000);
	 	}catch(Exception eee){System.out.println("No host with this IP"); return;}
		inFromUserSend = new BufferedReader(new InputStreamReader(System.in));

		outToServerSend = new DataOutputStream(clientSendSocket.getOutputStream());

		inFromServerSend = new BufferedReader(new InputStreamReader(clientSendSocket.getInputStream()));
	
		sentence ="REGISTER TOSEND "+username+"\n";
		
		outToServerSend.writeBytes(sentence + '\n');
		String B = inFromServerSend.readLine();
		if((B).equals("REGISTERED TOSEND "+username)){
			//System.out.println("Yes, I am registered to send!");
			String temp = inFromServerSend.readLine();
			break;
		}
		else{
			System.out.println("Try another username");
		}
		i++;
		}catch(Exception e1){
			System.out.println("Could not register at server");
			return;
		}
	}


	Socket clientRecvSocket = new Socket(hostname, 5000);
	BufferedReader inFromUserRecv = new BufferedReader(new InputStreamReader(System.in));
	DataOutputStream outToServerRecv = new DataOutputStream(clientRecvSocket.getOutputStream());	
	BufferedReader inFromServerRecv = new BufferedReader(new InputStreamReader(clientRecvSocket.getInputStream()));	

 	while(true){
 		try{
            sentence ="REGISTER TORECV "+username+"\n";
            outToServerRecv.writeBytes(sentence + '\n');
            String C = inFromServerRecv.readLine();
            if((C).equals("REGISTERED TORECV "+username)){
                //System.out.println("Yes, I am registered to receive!");
                String temp2 = inFromServerRecv.readLine();
                break;
            }
		}catch(Exception e1){
			System.out.println("Could not register at server");
			return;
		}
	}

	SocketSend socketSend=new SocketSend(clientSendSocket, inFromUserSend, outToServerSend, inFromServerSend);
	Thread threadSend=new Thread(socketSend);

	threadSend.start();
	SocketRecv socketRecv=new SocketRecv(clientRecvSocket, inFromUserRecv, outToServerRecv, inFromServerRecv);
	Thread threadRecv=new Thread(socketRecv);
	threadRecv.start();
         
    } 
} 

class SocketSend implements Runnable{
	Socket clientSendSocket;
	BufferedReader inFromUserSend;
	DataOutputStream outToServerSend;
 	BufferedReader inFromServerSend;
	SocketSend (Socket clientSendSocket, BufferedReader inFromUserSend, DataOutputStream outToServerSend, BufferedReader inFromServerSend){
		this.clientSendSocket = clientSendSocket;
    	this.inFromUserSend = inFromUserSend;
    	this.outToServerSend = outToServerSend;
		this.inFromServerSend = inFromServerSend;
    } 
	public void run(){
		while(true){
			try{
				String sentence = inFromUserSend.readLine();
				//read sentence in @[recipient username] [message] SEND format
				if(sentence.charAt(0)!='@'){
					System.out.println("Wrong Format !!");
					continue;				
				}
				String recipient="";
				int content;
				String message;
				int i;
				for(i=1;i<sentence.length();i++){
					if(sentence.charAt(i)==' '){ break;}
					recipient+=sentence.charAt(i);			
				}
				if(i==sentence.length()){
					System.out.println("Wrong Format !!");
					continue;
				}
				message=sentence.substring(i+1);
				content=sentence.length()-i-1;
				try{
					outToServerSend.writeBytes("SEND "+recipient+"\n"+ "Content-length: "+content+"\n"+'\n'+message+'\n');
				}catch (Exception ee){
					System.out.println("You are not connected to any server!");
					clientSendSocket.close();
					break;
				}
				
				String responseSend=inFromServerSend.readLine();
				if(responseSend.equals("SENT "+recipient)){
					if((inFromServerSend.readLine()).equals("") )
						System.out.println("Succesfully Delivered");
				}
				else{
					if((inFromServerSend.readLine()).equals("") )
						System.out.println("FROM SERVER: " + responseSend);
						System.out.println("Send your message again !");
				}
			}catch(Exception e){
				try {
					System.out.println("Connection Error");
					clientSendSocket.close();
				} catch(Exception ee) { }
				break;
			}
		}
	}
}

class SocketRecv implements Runnable{
	Socket clientRecvSocket;
	BufferedReader inFromUserRecv;
	DataOutputStream outToServerRecv;
 	BufferedReader inFromServerRecv;
	SocketRecv(Socket clientRecvSocket, BufferedReader inFromUserRecv, DataOutputStream outToServerRecv, BufferedReader inFromServerRecv){
		this.clientRecvSocket = clientRecvSocket;
        this.inFromUserRecv = inFromUserRecv;
        this.outToServerRecv = outToServerRecv;
		this.inFromServerRecv = inFromServerRecv;
     } 
	public void run(){
		while(true){
			try{
				String recvFormat="";
				try{
					recvFormat=inFromServerRecv.readLine();
					if(recvFormat.equals(""))
						recvFormat=inFromServerRecv.readLine();
				}catch(Exception ee) { 
					System.out.println("Server port is closed!");
					clientRecvSocket.close();
					break;
				}


				String responseRecv="ERROR 103 Header incomplete\n";
				String sender;
				if((recvFormat.substring(0,8)).equals("FORWARD ")){
					sender=recvFormat.substring(8);
				}
				else{
					outToServerRecv.writeBytes(responseRecv+'\n');
					continue;
				}
				recvFormat=inFromServerRecv.readLine();
				int content;
				if((recvFormat.substring(0,16)).equals("Content-length: ")){
					content=Integer.parseInt(recvFormat.substring(16));
				}
				else{
					outToServerRecv.writeBytes(responseRecv+'\n');
					continue;
				}
				String message;
				if(inFromServerRecv.readLine().equals("")){
					message=(inFromServerRecv.readLine()).substring(0,content);
				}
				else{
					outToServerRecv.writeBytes(responseRecv+'\n');
					continue;
				}
				//extract sender and message
				responseRecv="RECEIVED "+sender+'\n';
				outToServerRecv.writeBytes(responseRecv+'\n');
				System.out.println(sender+":"+message);
			}catch(Exception e){
				try {
					System.out.println("Connection Error");
					clientRecvSocket.close();
				} catch(Exception ee) { System.out.println("Connection Error");}
				break;
			}
		}
	}
}


