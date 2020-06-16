package Networking;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ClientAccept implements Runnable{
	
	ServerSocket serverSocket;
	public static final int PORT = 23000;
	Thread clientAcceptor;
	static volatile boolean serverON = false;
	
	public List<Client> clients;
	
	Random random = new Random();
	
	public ClientAccept() {
		
		try {
			serverSocket = new ServerSocket(PORT);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		clients = new ArrayList<>(8);
		clientAcceptor = new Thread(this, "Client-Acceptor");
	}

	@Override
	public void run() {
		while(serverON) {
			try {
				// creates new client with a unique ID
				Client client = createNewClient();
				
				System.out.println("Blocking after new client creation");
				
				// Blocking method
				Socket socket = serverSocket.accept();
				
				client.setupConnection(socket);
				
				clients.add(client);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private Client createNewClient() {
		Client client = new Client();
		
		boolean validID = true;
		
		do {
			client.setIdentity((long) (Math.random() * 1000));
			for (int i = 0; i < clients.size(); i++) {
				if (clients.get(i).getIdentity() == client.getIdentity()) {
					validID = false;
				}
			}
		} while (!validID); // While ID is not valid
		
		return client;
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
	
	public void disconnectClient(Client client) throws IOException{
		
	}
	
	public void disconnectAllClients() throws IOException {
		for (Client client: clients) {
			client.disconnect();
		}
		clients.clear();
	}
}
