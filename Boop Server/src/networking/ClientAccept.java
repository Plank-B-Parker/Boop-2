package networking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.BlockingQueue;

/**
 * Accepts incoming connections and creates a client object to associate with it.
 *
 */
public class ClientAccept implements Runnable {

	private final Server server;
	ServerSocketChannel serverChannel;
	private BlockingQueue<Client> clientQueue;

	private ClientHandler clientHandler;

	private static final SocketAddress WILDCARD_ADDRESS = new InetSocketAddress(Server.PORT);

	ClientAccept(Server server, ClientHandler clientHandler, BlockingQueue<Client> socketQueue) {
		this.server = server;
		this.clientHandler = clientHandler;
		this.clientQueue = socketQueue;

		try {
			// Binds channel to local IP-address at server-port
			serverChannel = ServerSocketChannel.open();
			serverChannel.bind(WILDCARD_ADDRESS);
			serverChannel.configureBlocking(false);

			System.out.println("ServerIP: " + serverChannel.getLocalAddress());
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void run() {
		var client = clientHandler.createNewClient();

		while (server.serverON) {
			try {
				var socketChannel = serverChannel.accept();
				if (socketChannel == null)
					continue;

				client.setClientChannel(socketChannel);
				clientQueue.put(client);

				client = clientHandler.createNewClient();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
				Thread.currentThread().interrupt();
			}
		}
	}

	public void closeServerChannel() throws IOException {
		serverChannel.close();
		clientQueue.clear();
	}

	public BlockingQueue<Client> getClientBuffer() {
		return clientQueue;
	}

}
