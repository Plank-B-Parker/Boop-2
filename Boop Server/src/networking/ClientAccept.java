package networking;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ClientAccept implements Runnable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5435601507210685540L;
	ServerSocket serverSocket;
	public static final int PORT = 2300;
	Thread clientAcceptor;
	static volatile boolean serverON = false;
	
	List<Client> clientsToAdd = new ArrayList<>(4);
	List<Client> clientsToRemove = new ArrayList<>(4);
	List<Client> clients = new ArrayList<>();
	
	
	Random random = new Random();
	
	public ClientAccept() {
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
				
				clientsToAdd.add(client);
				
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
		disconnectAllClients();
		try {
			serverSocket.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void moveWaitingClients() {
		for (Client client : clientsToAdd) {
			clients.add(client);
		}
		
		for (Client client : clientsToRemove) {
			clients.remove(client);
		}
		
		clientsToAdd.clear();
		clientsToRemove.clear();
	}
	
	public void checkClientsConnection() {
		for (int i = 0; i < clients.size(); i++) {
			if (! clients.get(i).isConnected()) clientsToRemove.add(clients.get(i));
		}
	}
	
	public void disconnectClient(Client client){
		client.disconnect();
	}
	
	public void disconnectAllClients(){
		for (Client client: clients) {
			client.disconnect();
		}
		clients.clear();
	}

	public Client getClientByAddressAndPort(InetAddress address, int port) {

		for (Client client: clients) {
			if (client.getIpv4Address().equals(address) && client.getClientPort() == port) return client;
		}

		return null;
	}
	
	public Client getClientByID(long ID) {
		for(Client client: clients) {
			if(client.getIdentity() == ID) 
				return client;
		}
		return null;
	}
	
	public List<Client> getClients(){
		return clients;
	}
}
