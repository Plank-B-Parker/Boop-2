package networking;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler{
	
	List<Client> clientsToAdd = new ArrayList<>(4);
	List<Client> clientsToRemove = new ArrayList<>(4);
	List<Client> clients = new ArrayList<>();
	
	public void updateClients(float dt) {
		for(Client client: clients) {
			client.updatePos(dt);
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
