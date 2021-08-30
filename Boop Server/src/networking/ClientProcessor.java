package networking;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientProcessor implements Runnable {

	private final Server server;
	private final ClientHandler clientHandler;
	private BlockingQueue<Client> inboundClientQueue;

	private BlockingQueue<Packet> outboundPacketQueue;

	private Map<Long, Client> clientIdMap;

	// Which clients has data to be written to
	private Set<Client> emptyToNonEmptyClients;
	private Set<Client> nonEmptyToEmptyClients;

	// Buffers can hold a minimum of 4 max sized packets at once
	private ByteBuffer readBuffer = ByteBuffer.allocate(PacketData.MAX_PAYLOAD_SIZE * 4);
	private ByteBuffer writeBuffer = ByteBuffer.allocate(PacketData.MAX_PAYLOAD_SIZE * 4);

	private Selector readSelector;
	private Selector writeSelector;

	private static final int READ_CAPACITY = 30;
	private static final int WRITE_CAPACITY = 30;

	ClientProcessor(Server server, ClientHandler clientHandler, BlockingQueue<Client> clientQueue) {
		this.server = server;
		this.clientHandler = clientHandler;
		this.inboundClientQueue = clientQueue;

		clientIdMap = new HashMap<>(64); // Will resize when no. of clients >= 48
		outboundPacketQueue = new LinkedBlockingQueue<>();

		emptyToNonEmptyClients = new HashSet<>();
		nonEmptyToEmptyClients = new HashSet<>();

		try {
			readSelector = Selector.open();
			writeSelector = Selector.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		while (server.serverON) {
			try {
				registerNewClients();
				readFromClients();
				writeToClients();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Polls all client in the inbound queue to register it to the selector.
	 * <p>
	 * Configures the channel to be non-blocking as well as completes the set-up for
	 * the client.
	 * 
	 * @throws IOException
	 */
	private void registerNewClients() throws IOException {
		var newClient = inboundClientQueue.poll();

		while (newClient != null) {
			var socketChannel = newClient.getClientChannel();

			// Socket belonging to the client is set to non-blocking
			socketChannel.configureBlocking(false);

			// initialised readers and writes for the client
			newClient.tcpPacketReader = new PacketReader(READ_CAPACITY);
			newClient.tcpPacketWriter = new PacketWriter(WRITE_CAPACITY);

			clientIdMap.put(newClient.getIdentity(), newClient);

			// Registers a channel to the selector and attaches the client object
			SelectionKey key = socketChannel.register(readSelector, SelectionKey.OP_READ);
			key.attach(newClient);

			clientHandler.setUpClient(newClient, socketChannel, outboundPacketQueue);
			// Send id and client join
			for (Client existingClient : clientHandler.clients) {
				if (newClient == existingClient)
					continue;

				server.alertClient(existingClient, newClient, true);
				server.alertClient(newClient, existingClient, true);
			}
			clientHandler.setEveryOneKnowsEachOther(true);

			newClient = inboundClientQueue.poll();
		}
	}

	private void readFromClients() throws IOException {
		int readable = readSelector.selectNow();

		if (readable > 0) {
			Set<SelectionKey> selectedKeys = readSelector.selectedKeys();
			var keysIterator = selectedKeys.iterator();

			while (keysIterator.hasNext()) {
				var key = keysIterator.next();

				readPacketFromKey(key);

				keysIterator.remove();
			}

			// Removes handled keys from the set
			selectedKeys.clear();
		}
	}

	private void writeToClients() throws IOException {
		// Take all of the packets from the outbound packet queue
		takeOutboundPackets();

		// Cancel clients that don't have data to write with from selector
		// (This is done because the client can be 'ready' but have no data)
		cancelEmptyClients();

		// Register clients that have data to the selector
		registerNonEmptyClients();

		// Iterate through and write to them from a buffer in client class
		int writeReady = this.writeSelector.selectNow();

		if (writeReady > 0) {
			Set<SelectionKey> keys = writeSelector.selectedKeys();
			var keyIterator = keys.iterator();

			while (keyIterator.hasNext()) {
				var key = keyIterator.next();

				Client client = (Client) key.attachment();

				client.tcpPacketWriter.write(client, writeBuffer);

				// if no more packets to write then add to empty clients set.
				if (client.tcpPacketWriter.isWriterEmpty()) {
					nonEmptyToEmptyClients.add(client);
				}

				keyIterator.remove();
			}

			// Removes handled keys from the set
			keys.clear();
		}
	}

	private void readPacketFromKey(SelectionKey key) throws IOException {
		Client client = (Client) key.attachment();
		client.read(readBuffer);

		// Client has disconnected
		if (client.isEndOfStream()) {
			System.out.println("Socket closed by Client");
			clientIdMap.remove(client.getIdentity());
			key.attach(null);
			key.cancel();
			key.channel().close();
			client.disconnect();

			for (Client existingClient : clientHandler.clients) {
				server.alertClient(existingClient, client, false);
			}
			clientHandler.setEveryOneKnowsEachOther(true);
		}

	}

	/**
	 * Transfers packets to be sent from the queue into their respective client's
	 * buffers
	 */
	private void takeOutboundPackets() {
		var outPacket = outboundPacketQueue.poll();

		while (outPacket != null) {
			// Get client that the packet is for
			var client = clientIdMap.get(outPacket.clientID);

			if (client != null) {
				if (client.tcpPacketWriter.isWriterEmpty()) {
					// Client was empty but now it's not
					client.tcpPacketWriter.enqueueWritePacket(outPacket);
					nonEmptyToEmptyClients.remove(client);
					emptyToNonEmptyClients.add(client);
				} else { // Client is not empty
					client.tcpPacketWriter.enqueueWritePacket(outPacket);
				}

			}

			outPacket = outboundPacketQueue.poll();
		}

	}

	private void cancelEmptyClients() {
		for (var client : nonEmptyToEmptyClients) {
			SelectionKey key = client.getClientChannel().keyFor(writeSelector);
			key.cancel();
		}

		nonEmptyToEmptyClients.clear();
	}

	private void registerNonEmptyClients() throws ClosedChannelException {
		for (var client : emptyToNonEmptyClients) {
			client.getClientChannel().register(writeSelector, SelectionKey.OP_WRITE, client);
		}

		emptyToNonEmptyClients.clear();
	}

	public boolean addOutboundPacket(Packet packet) {
		return outboundPacketQueue.offer(packet);
	}

	public void stopProcessing() throws IOException {
		readSelector.close();
		writeSelector.close();
	}
}
