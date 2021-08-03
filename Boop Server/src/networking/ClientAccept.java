package networking;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Random;

public class ClientAccept implements Runnable{
	
	ServerSocket serverSocket;
	public static final int PORT = 2300;
	private Thread clientAcceptor;
	static volatile boolean serverON = false;
	
	ClientHandler clientHandler;
	
	
	Random random = new Random();
	
	public ClientAccept(ClientHandler clientHandler) {
		this.clientHandler = clientHandler;
		try {
			serverSocket = new ServerSocket(PORT);
			
			System.out.println("ServerIP: " + serverSocket.getLocalSocketAddress());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		clientAcceptor = new Thread(this, "Client-Acceptor");
		clientAcceptor.setDaemon(true);
	}

	@Override
	public void run() {
		while(serverON) {
			try {
				// creates new client with a unique ID
				var client = createNewClient();
				
				System.out.println("Waiting for new Client");
				
				// Blocking method
				var socket = serverSocket.accept();
				
				setUpClient(client, socket);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void setUpClient(Client client, Socket socket)throws IOException {
		client.setupConnection(socket);
		//Anything to send or recieve here:
		
		
		//
		clientHandler.addClient(client);
		client.finishSetUp();
	}
	
	private Client createNewClient() {
		var client = new Client();
		List<Client> clients = clientHandler.getClients();
		
		var validID = true;
		
		do {
			client.setIdentity((long) (Math.random() * 1000 + 1000));
			for (var i = 0; i < clients.size(); i++) {
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
		clientHandler.disconnectAllClients();
		try {
			serverSocket.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
