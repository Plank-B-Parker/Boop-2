package Networking;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientAccept implements Runnable{
	
	ServerSocket serverSocket;
	private int serverPort;
	Thread clientAcceptor;
	static volatile boolean serverON = false;
	
	public List<Client> clients;
	
	public ClientAccept(int port) {
		this.serverPort = port;
		
		try {
			serverSocket = new ServerSocket(port);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		clients = new ArrayList<>(8);
		clientAcceptor = new Thread("Client-Acceptor");
	}

	@Override
	public void run() {
		while(serverON) {
			try {
				Client client = new Client();
				// Blocking method
				Socket socket = serverSocket.accept();
				
				client.setupConnection(socket);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void startServer() {
		serverON = true;
		clientAcceptor.start();
	}
	
	
	public void terminateServer() {
		serverON = false;
		try {
			disconnectAllClients();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void disconnectAllClients() throws IOException {
		for (Client client: clients) {
			client.disconnect();
		}
	}
}
