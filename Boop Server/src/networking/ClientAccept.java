package networking;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ClientAccept extends ArrayList<Client> implements Runnable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5435601507210685540L;
	ServerSocket serverSocket;
	public static final int PORT = 2300;
	Thread clientAcceptor;
	static volatile boolean serverON = false;
	
	ArrayList<Client> clientsToAdd = new ArrayList<>();
	
	
	Random random = new Random();
	
	public ClientAccept() {
		super(8);
		try {
			serverSocket = new ServerSocket(PORT);
			
			System.out.println("ServerIP: " + serverSocket.getLocalSocketAddress());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		clientAcceptor = new Thread(this, "Client-Acceptor");
	}

	@Override
	public void run() {
		while(serverON) {
			try {
				// creates new client with a unique ID
				Client client = createNewClient();
				
				System.out.println("Waiting for new Client");
				
				// Blocking method
				Socket socket = serverSocket.accept();
				
				client.setupConnection(socket);
				
				add(client);
				
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
			for (int i = 0; i < size(); i++) {
				if (get(i).getIdentity() == client.getIdentity()) {
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
		disconnectAllClients();
		try {
			serverSocket.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void checkClientsConnection() {
		Client[] localClients = toArray(new Client[size()]);
		
		for (Client client: localClients) {
			if (! client.isConnected()) remove(client);
		}
	}
	
	public void disconnectClient(Client client){
		client.disconnect();
	}
	
	public void disconnectAllClients(){
		for (Client client: this) {
			client.disconnect();
		}
		clear();
	}

	public Client getClientByAddressAndPort(InetAddress address, int port) {

		for (Client client: this) {
			if (client.getIpv4Address().equals(address) && client.getClientPort() == port) return client;
		}

		return null;
	}
	
	public Client getClientByID(long ID) {
		for(Client client: this) {
			if(client.getIdentity() == ID) 
				return client;
		}
		return null;
	}
}
