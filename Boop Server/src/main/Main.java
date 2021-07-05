package main;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.swing.JFrame;

import balls.Ball;
import balls.Storage;
import debug.Key;
import debug.Keyboard;
import debug.Mouse;
import math.Bitmaths;
import math.Vec2f;
import networking.Client;
import networking.ClientAccept;
import networking.ClientHandler;
import networking.Packet;
import networking.UDP;

public class Main {
	
	public static final int windowWidth = 720, windowHeight = 720;
	private Canvas canvas = new Canvas();
	public Storage storage = new Storage();
	public ClientHandler clientHandler = new ClientHandler();
	public Keyboard keyboard;
	public Mouse mouse;
	
	private Ball debug_ball = new Ball(1);
	
	BufferStrategy bs;
	
	public static final boolean deterministicPhysics = true;
	
	public static InetAddress serverIP = null;
	
	public Main() {
		
		keyboard = new Keyboard(this);
		mouse = new Mouse();
		
		//Add keyboard listener.
		canvas.addKeyListener(keyboard);
		canvas.addMouseMotionListener(mouse);
		
		storage.add(debug_ball);
		//temporary
		debug_ball.setID(-5);
		
		Random random;
		
		// If the physics is deterministic, use a a set seed. Otherwise, use a random seed.
		if (deterministicPhysics) random = new Random(3);
		else random = new Random();
		
		for(int i = 0; i < 10; i++) {
			Ball ball = new Ball(1);
			ball.setPos(2f*(random.nextFloat() - 0.5f), 2f*(random.nextFloat() - 0.5f));
			//ball.setPos(0, -0.98f*i);
			//ball.phys.vel.x = 2f*(random.nextFloat() - 0.5f);
			//ball.phys.vel.y = 2f*(random.nextFloat() - 0.5f);
			storage.add(ball);
		}
		
		
		
		for(int i = 0; i < 1; i++) {
			Ball ball = new Ball(2);
			ball.setPos(2f*(random.nextFloat() - 0.5f), 2f*(random.nextFloat() - 0.5f));
			//ball.setPos(0, -0.98f*i);
			ball.phys.vel.x = 2f*(random.nextFloat() - 0.5f);
			ball.phys.vel.y = 2f*(random.nextFloat() - 0.5f);
			storage.add(ball);
		}
		
//		Ball a = new Ball(2);
//		a.setPos(-0.75f, 0);
//		a.setVel(0.1f, 0);
//		a.phys.acc.x = 0f;
//		storage.add(a);
//		
//		Ball b = new Ball(2);
//		b.setPos(0.5f, 0);
//		b.setVel(0,0);
//		storage.add(b);
		
	}
	
	public void createDisplay() {
		JFrame frame = new JFrame("Server");
		
		canvas.setSize(1920, 1080);
		canvas.setFocusable(true);
		
		frame.getContentPane().setPreferredSize(new Dimension(windowWidth, windowHeight));
		frame.pack();
		frame.setLayout(null);
		frame.getContentPane().setBackground(Color.BLACK);
		frame.getContentPane().add(canvas);
		frame.setResizable(true);
		frame.setLocationRelativeTo(null);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent we) {
				clientAcceptor.terminateServer();
				udp.disconnect();
				System.exit(0);
			}
		});
		frame.validate();
		frame.setVisible(true);
	}
	
	ClientAccept clientAcceptor;
	UDP udp;
	
	public void setupConnections() {
		clientAcceptor = new ClientAccept(clientHandler);
		udp = new UDP(clientAcceptor);
		clientAcceptor.startServer();
		udp.startUDP();
	}
	
	private boolean running = false;
	public static boolean physpaused = false;
	private int TPS = 0;
	private int FPS = 0;
	private int packetsSentPerSec = 0;
	private int packetsRecievedPerSec = 0;
	private int packetsRecievedLastTotal = 0;
	private int packetsSentLastTotal = 0;
	
	public void mainLoop() {
		running = true;
				
		final double MS_PER_UPDATE = 1000.0 / 60.0;
		final double MS_PER_FRAME_LIMIT = 1000.0 / 60.0;

		long previous = System.currentTimeMillis();
		long timeAfterLastTick = 0;
		long timeAfterLastTransmit = 0;
		long timer = System.currentTimeMillis();
		long frameTimer = System.currentTimeMillis();
		
		int ticks = 0;
		int frames = 0;
		
		while(running) {
			long current = System.currentTimeMillis();
			long elapsed = current - previous;
			previous = current;
			timeAfterLastTick += elapsed;
			timeAfterLastTransmit += elapsed;
			
			processInputs();
			
			// 60 physics update per second
			while (timeAfterLastTick >= MS_PER_UPDATE) {
				
				if(! physpaused) {
					// If the physics is not deterministic, use the real delta time for physics calculations
					if (!deterministicPhysics) fixedUpdate(timeAfterLastTick/1000f);
					
					// If the physics is deterministic, set a fixed delta time for physics calculations
					else fixedUpdate(1f/60f);
				}
				
				if (physpaused && keyboard.isActive(Key.L)) {
					fixedUpdate(1f/60f);
				}
				
				timeAfterLastTick -= MS_PER_UPDATE;
				ticks++;
			}
			
			// maximum 60 transmits per second
			while (timeAfterLastTransmit >= MS_PER_UPDATE) {
				// Send data and other stuff here
				
				// check if clients are connected before attempting to send data.
				final List<Client> clients = clientHandler.getClients();
				if (clients.isEmpty()) break;
				
				// Send position, velocity, influence and ID about every client to every client.
				sendAllClientInfo();
				
				// send ballz
				sendTestBalls2();
				
				
				timeAfterLastTransmit -= MS_PER_UPDATE * 2;
			}
			
			if (System.currentTimeMillis() - frameTimer >= MS_PER_FRAME_LIMIT) {
				render(timeAfterLastTick / 1000f);
			frames++;
			
				frameTimer += MS_PER_FRAME_LIMIT;
			}
			
			if (System.currentTimeMillis() - timer >= 1000) {
				clockSynchronise();
				packetsRecievedPerSec = udp.RecievedPacketsUDP.get() - packetsRecievedLastTotal;
				packetsSentPerSec = udp.SentPacketsUDP.get() - packetsSentLastTotal;
				
				packetsRecievedLastTotal = udp.RecievedPacketsUDP.get();
				packetsSentLastTotal = udp.SentPacketsUDP.get();
				TPS = ticks;
				FPS = frames;
				ticks = 0;
				frames = 0;
				timer += 1000;
				//System.out.println(clientAcceptor.clients);
			}
		}
		
	}
	
	public void processInputs() {
		boolean prevPause = physpaused;
		physpaused = keyboard.isActive(Key.K);
		if (prevPause != physpaused) keyboard.resetCount(Key.L);
	}
	
	public void fixedUpdate(float dt) {
		// Update client lists here to avoid concurrent modification
		clientHandler.checkClientsConnection();
		clientHandler.moveWaitingClients();
		
		List<Client> clients = clientHandler.getClients();
		
		clientHandler.updateClients(storage.getBalls(), dt);
		storage.updateBalls(clients, dt);
	}
	
	public void render(float dt) {
		bs = canvas.getBufferStrategy();
		if (bs == null) {
			canvas.createBufferStrategy(2);
			return;
		}
		
		do {
			Graphics2D g2d = (Graphics2D) bs.getDrawGraphics();
			
			g2d.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());;
			
			if (keyboard.isActive(Key.SPACE)) renderGrid(g2d);
			
			if (keyboard.isActive(Key.G)) debugBall();
			else debug_ball.setPos(-10, -10);
			
			storage.renderBalls(g2d, dt);
			
			drawPerformance(g2d);
			
			g2d.dispose();
			bs.show();
		}while(bs.contentsLost());
	}
	
	//Render a grid.
	public void renderGrid(Graphics2D g) {
		g.setColor(Color.WHITE);
		float n = 8f;
		for(int i = 1; i < n; i++) {
			g.drawLine(0, (int)(i/n*windowHeight), windowHeight, (int)(i/n*windowHeight));
			g.drawLine((int)(i/n*windowHeight), 0, (int)(i/n*windowHeight), windowHeight);
			g.drawString(""+ 2*(n/2-i)/n,  windowHeight/2, (int)(i/n*windowHeight));
			g.drawString(""+ 2*(i - n/2)/n,  (int)(i/n*windowHeight), windowHeight/2);
		}
		List<Client> clients = clientHandler.getClients();
		for (Client client : clients) {
			g.setColor(Color.RED);
			int x = (int) ((client.centrePos.x + 1) * 0.5 * windowHeight);
			int y = (int) ((client.centrePos.y + 1) * 0.5 * windowHeight);
			int rad = (int) ((client.radOfVision) * 0.5 * windowHeight);
			
			g.drawOval(x - rad, y - rad, 2*rad, 2*rad);
			
			g.setColor(Color.GREEN);
			// TODO draw rectangle to show client's viewport.
			x = (int) ((client.centrePos.x + 1) * 0.5 * windowHeight);
			y = (int) ((client.centrePos.y + 1) * 0.5 * windowHeight);
			int height = (int) (client.radOfInf / Math.sqrt(2) * (float) windowHeight / windowWidth * windowHeight);
			int width = (int) (client.radOfInf / Math.sqrt(2) * windowHeight);
			x -= width / 2;
			y -= height / 2;
			
			g.drawRect(x, y, width, height);
		}
	}

	private void drawPerformance(Graphics2D g2d) {
		g2d.setColor(Color.RED);
		g2d.setFont(new Font("Calibri", Font.PLAIN, 20)); 
		g2d.drawString("FPS: " + FPS, 50, 50);
		g2d.drawString("Ticks: " + TPS, 50, 75);
		
		g2d.drawString("Tx: " + packetsSentPerSec, 140, 50);
		g2d.drawString("Rx: " + packetsRecievedPerSec, 140, 75);
	}
	
	private void debugBall() {
		Vec2f pos = mouse.getMousePos();
		float x = pos.x / windowHeight * 2 - 1;
		float y = pos.y / windowHeight * 2 - 1;
		debug_ball.setPos(x, y);
	}
	
	private void sendTestBalls() {
		// packet id, ballID, ball type, x, y, velx, vely, ownerID 
		Collection<Ball> allBalls = storage.getBalls();
		List<Client> clients = clientHandler.getClients();
		
		// sends data to all clients at the same time
		int numberOfItems = Packet.NEW_BALLS.getNumberOfItems();
		int ballsPerPacket = Packet.NEW_BALLS.getNumObj();
		
		// Store balls within a certain area around the client screen inside the Client class TODO
		
		int packetsNo = (int) Math.ceil((float) allBalls.size() / (float) ballsPerPacket);
		byte[][][] data = new byte[clients.size()][packetsNo][Packet.MAX_PAYLOAD_SIZE - 1];
		
		int[] clientMaxPackets = new int[clients.size()];
		
		for (int i = 0; i < clients.size(); i++) {
			
			Client client = clients.get(i);
			//Check if client is ready;
			if(!client.isReadyForPacket(100, Packet.NEW_BALLS)) continue;
			
			List<Ball> inRange = new ArrayList<>();
			
			for (Ball ball: allBalls) {
				// check if ball is in client area (simple circ)
				//TODO: use disp method to find differnce.
				float dx = ball.phys.pos.x - client.centrePos.x;
				float dy = ball.phys.pos.y - client.centrePos.y;

				//Finds minimum difference in position.
				if(dx > 1) {
					dx = dx-2;
				}
				if(dx < -1) {
					dx = dx+2;
				}
				
				if(dy > 1) {
					dy = dy-2;
				}
				if(dy < -1) {
					dy = dy+2;
				}
				
				if (dx*dx + dy*dy <= (client.radOfVision + ball.getRad())*(client.radOfVision + ball.getRad())) {
					inRange.add(ball);	
				}	
			}
			
		//	System.out.println("num balls sent: " + inRange.size());
			
			int numberOfPackets = (int) Math.ceil((double) inRange.size() / ballsPerPacket);
			int floatsPerPacket = ballsPerPacket * numberOfItems;
			int packetsFilled = 0;
			int lastBall = 0;
			
			clientMaxPackets[i] = numberOfPackets;
			
			float[][] floatData = new float[numberOfPackets][floatsPerPacket];
			
			while (packetsFilled < numberOfPackets) {
				for (int b1 = 0, b2 = lastBall; b1 < ballsPerPacket && b2 < inRange.size(); b1++, b2++) {
					Ball ball = inRange.get(b2);
					int offset = numberOfItems *  b1;
					lastBall++;
					
					floatData[packetsFilled][offset] = ball.getID();
					floatData[packetsFilled][offset + 1] = ball.getType();
					floatData[packetsFilled][offset + 2] = ball.phys.pos.x;
					floatData[packetsFilled][offset + 3] = ball.phys.pos.y;
					floatData[packetsFilled][offset + 4] = ball.phys.vel.x;
					floatData[packetsFilled][offset + 5] = ball.phys.vel.y;
					floatData[packetsFilled][offset + 6] = ball.getOwnerID();
					
				}
				data[i][packetsFilled] = Bitmaths.floatArrayToBytes(floatData[packetsFilled]);
				packetsFilled++;
			}
			
		}
		// data[i].length
		// clientMaxPackets[i]
		// Send packets to client
		for (int i = 0; i < clients.size(); i++) {
			for (int j = 0; j < clientMaxPackets[i]; j++) {
				udp.sendData(Bitmaths.pushByteToData((byte) 2, data[i][j]), clients.get(i).getIpv4Address(), clients.get(i).getClientPort());
			}
		}
		
		//System.out.println("num Packets after sending: " + numPackets);
	}
	
	/**
	 * Also adds an offset to the packet to send time and packet sequence.
	 * 12 bytes used for offset
	 */
	private void sendTestBalls2() {
		// packet id, ballID, ball type, x, y, velx, vely, ownerID 
		Collection<Ball> allBalls = storage.getBalls();
		List<Client> clients = clientHandler.getClients();
		
		// sends data to all clients at the same time
		int numberOfItems = Packet.NEW_BALLS.getNumberOfItems();
		int ballsPerPacket = Packet.NEW_BALLS.getNumObj();
		
		// Store balls within a certain area around the client screen inside the Client class TODO
		
		int packetsNo = (int) Math.ceil((float) allBalls.size() / (float) ballsPerPacket);
		byte[][][] data = new byte[clients.size()][packetsNo][Packet.FREE_PAYLOAD_SIZE];
		
		int[] clientMaxPackets = new int[clients.size()];
		
		for (int i = 0; i < clients.size(); i++) {
			
			Client client = clients.get(i);
			//Check if client is ready;
			if(!client.isReadyForPacket(100, Packet.NEW_BALLS)) continue;
			
			List<Ball> inRange = new ArrayList<>();
			
			for (Ball ball: allBalls) {
				// check if ball is in client area (simple circ)
				//TODO: use disp method to find differnce.
				float dx = ball.phys.pos.x - client.centrePos.x;
				float dy = ball.phys.pos.y - client.centrePos.y;

				//Finds minimum difference in position.
				if(dx > 1) {
					dx = dx-2;
				}
				if(dx < -1) {
					dx = dx+2;
				}
				
				if(dy > 1) {
					dy = dy-2;
				}
				if(dy < -1) {
					dy = dy+2;
				}
				
				if (dx*dx + dy*dy <= (client.radOfVision + ball.getRad())*(client.radOfVision + ball.getRad())) {
					inRange.add(ball);	
				}	
			}
			
			int numberOfPackets = (int) Math.ceil((double) inRange.size() / ballsPerPacket);
			int floatsPerPacket = ballsPerPacket * numberOfItems;
			int packetsFilled = 0;
			int lastBall = 0;
			
			clientMaxPackets[i] = numberOfPackets;
			
			float[][] floatData = new float[numberOfPackets][floatsPerPacket];
			
			while (packetsFilled < numberOfPackets) {
				for (int b1 = 0, b2 = lastBall; b1 < ballsPerPacket && b2 < inRange.size(); b1++, b2++) {
					Ball ball = inRange.get(b2);
					int offset = numberOfItems *  b1;
					lastBall++;
					
					floatData[packetsFilled][offset] = ball.getID();
					floatData[packetsFilled][offset + 1] = ball.getType();
					floatData[packetsFilled][offset + 2] = ball.phys.pos.x;
					floatData[packetsFilled][offset + 3] = ball.phys.pos.y;
					floatData[packetsFilled][offset + 4] = ball.phys.vel.x;
					floatData[packetsFilled][offset + 5] = ball.phys.vel.y;
					floatData[packetsFilled][offset + 6] = ball.getOwnerID();
					
				}
				data[i][packetsFilled] = Bitmaths.floatArrayToBytes(floatData[packetsFilled]);
				packetsFilled++;
			}
			
		}
		// Send packets to client
		for (int i = 0; i < clients.size(); i++) {
			for (int j = 0; j < clientMaxPackets[i]; j++) {
				// array to replace with header information
				byte[] headerInfo = new byte[0];
				
				// Add packet id and packet sequence
				headerInfo = Bitmaths.pushByteToData(Packet.NEW_BALLS.getID(), headerInfo);
				headerInfo = Bitmaths.pushByteArrayToData(Bitmaths.intToBytes(clients.get(i).udpPacketsSent.incrementAndGet()), headerInfo);
				
				byte[] completePacket = Bitmaths.pushByteArrayToData(headerInfo, data[i][j]);
				
				udp.sendData(completePacket, clients.get(i).getIpv4Address(), clients.get(i).getClientPort());
				
			}
		}
		
	}
	
	public void sendAllClientInfo() {
		final int numItemsPerObj = Packet.CLIENTDATA.getNumberOfItems();
		final int maxClientsPerPacket = Packet.CLIENTDATA.getNumObj();
		
		// Prepare data only for clients that are ready to accept packets.
		final List<Client> clients = clientHandler.getClients();
		List<Client> readyClients = new ArrayList<>();
		
		var clientsData = new Number[clients.size() * numItemsPerObj];
		var numClients = 0;
		
		for (var client : clients) {
			if (!client.isReadyForPacket(200, Packet.CLIENTDATA)) continue;
			
			// Fill data to prepare for sending. (Front of the array will be filled first).
			readyClients.add(client);
			clientsData[numClients * 6 + 0] = client.centrePos.x;
			clientsData[numClients * 6 + 1] = client.centrePos.y;
			clientsData[numClients * 6 + 2] = client.velocity.x;
			clientsData[numClients * 6 + 3] = client.velocity.y;
			clientsData[numClients * 6 + 4] = client.radOfInf;
			clientsData[numClients * 6 + 5] = client.getIdentity();
			
			numClients++;
		}
		
		int packetNo = (int)Math.ceil((float)numClients / (float)maxClientsPerPacket);
		boolean isSinglePacket = packetNo == 1;
		
		int payloadSize = isSinglePacket 
				? numClients * Packet.CLIENTDATA.getObjectSize() : Packet.CLIENTDATA.getMaxPayload();
		
		int numbersPerPacket = isSinglePacket ? 
				numClients * numItemsPerObj : numItemsPerObj * maxClientsPerPacket;
		
		byte[][] data = new byte[packetNo][payloadSize];
		Number[][] splitInput = new Number[packetNo][numbersPerPacket];
		
		var inputFilled = 0;
		var offset = 0;
		
		while (inputFilled < packetNo) {

			int numToFill = numbersPerPacket;
			
			// Trim the size of the array at the last packet
			if (!isSinglePacket && inputFilled >= packetNo - 1) {
				numToFill = (numClients * numItemsPerObj) % (maxClientsPerPacket * numItemsPerObj);
				
				splitInput[inputFilled] = new Number[numToFill];
			}
			
			for (var i = 0; i < numToFill; i++) {
				splitInput[inputFilled][i] = clientsData[i + offset];
			}
			
			inputFilled++;
			offset += maxClientsPerPacket * numItemsPerObj;
		}
		
		// Fill packets with data
		// array to replace with header information
		byte[] headerInfo = new byte[0];
		for(int i = 0; i < packetNo; i++) {
			data[i] = Bitmaths.numberArrayToBytes(splitInput[i]);
		}
		
		// Send data to all clients at same time.
		for (var client : readyClients) {
			for (var i = 0; i < packetNo; i++) {
				headerInfo = new byte[0];
				headerInfo = Bitmaths.pushByteToData(Packet.CLIENTDATA.getID(), headerInfo);
				headerInfo = Bitmaths.pushByteArrayToData(Bitmaths.intToBytes(client.udpPacketsSent.incrementAndGet()), headerInfo);
				byte[] completePacket = Bitmaths.pushByteArrayToData(headerInfo, data[i]);
				
				udp.sendData(completePacket, client.getIpv4Address(), client.getClientPort());
			}
		}
		
	}
	
	/**
	 * Synchronises the client's clock to the server.
	 * This stops the client manipulating time and correctly works out time sensitive information.
	 */
	public void clockSynchronise() {
		final List<Client> clients = clientHandler.getClients();
		
		for (Client client : clients) {
			byte[] data = Bitmaths.longToBytes(System.currentTimeMillis());
			data = Bitmaths.pushByteToData(Packet.CLOCK_SYN.getID(), data);
			data = Bitmaths.pushByteArrayToData(Bitmaths.intToBytes(client.udpPacketsSent.incrementAndGet()), data);
			
			udp.sendData(data, client.getIpv4Address(), client.getClientPort());
		}
		
	}
	
	public static void main(String args[]){
		Thread t = Thread.currentThread();
		t.setName("Main-Loop");
		
		Main main = new Main();
		main.createDisplay();
		main.setupConnections();
		main.mainLoop();
	}
}
