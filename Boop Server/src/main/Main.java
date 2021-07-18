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
		
		for(int i = 0; i < 100; i++) {
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
		var frame = new JFrame("Server");
		
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
	
	private static final double TICK_RATE = 60.0;
	private double fps_cap = 60.0;
	
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
				
		final double NS_PER_UPDATE = 1000000000.0 / TICK_RATE;
		double nsPerFrameLimit = 1000000000.0 / fps_cap;

		long previous = System.nanoTime();
		long timeAfterLastTick = 0;
		long timeAfterLastTransmit = 0;
		long timer = System.nanoTime();
		long frameTimer = System.nanoTime();
		
		int ticks = 0;
		int frames = 0;
		
		while(running) {
			long current = System.nanoTime();
			long elapsed = current - previous;
			previous = current;
			timeAfterLastTick += elapsed;
			timeAfterLastTransmit += elapsed;
			
			
			// The program is running faster than set limits and should be slowed down.
			if (elapsed < NS_PER_UPDATE && elapsed < nsPerFrameLimit) {
				
				double freeTime = Math.min(NS_PER_UPDATE - elapsed, nsPerFrameLimit - elapsed);
				long sleepTime = (long) Math.floor(freeTime / 2.0) / 1000000;
				
				try {
					if (sleepTime > 2) 
						Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
					Thread.currentThread().interrupt();
				}
			}
			
			// 60 physics update per second
			while (timeAfterLastTick >= NS_PER_UPDATE) {
				
				if(! physpaused) {
					// If the physics is not deterministic, use the real delta time for physics calculations
					if (!deterministicPhysics) fixedUpdate(timeAfterLastTick/1000000000f);
					
					// If the physics is deterministic, set a fixed delta time for physics calculations
					else fixedUpdate(1f/60f);
				}
				
				if (physpaused && keyboard.isActive(Key.L)) {
					fixedUpdate(1f/60f);
				}
				
				timeAfterLastTick -= NS_PER_UPDATE;
				ticks++;
			}
			
			// maximum 60 transmits per second
			while (timeAfterLastTransmit >= NS_PER_UPDATE) {
				// Send data and other stuff here
				
				// check if clients are connected before attempting to send data.
				final List<Client> clients = clientHandler.getClients();
				if (clients.isEmpty()) break;
				
				clientHandler.pingClients();
				
				// Send position, velocity, influence and ID about every client to every client.
				udp.sendAllClientInfo(clients);
				
				final List<Ball> balls = storage.getBalls();
				
				// send ballz
				udp.sendBalls(balls, clients);
				
				
				timeAfterLastTransmit -= NS_PER_UPDATE;
			}
			
			if (System.nanoTime() - frameTimer >= nsPerFrameLimit) {
				render(timeAfterLastTick / 1000000000f);
				processInputs();
				frameTimer += nsPerFrameLimit;
				frames++;
			}
			
			if (System.nanoTime() - timer >= 1000000000) {
				packetsRecievedPerSec = udp.RecievedPacketsUDP.get() - packetsRecievedLastTotal;
				packetsSentPerSec = udp.SentPacketsUDP.get() - packetsSentLastTotal;
				
				packetsRecievedLastTotal = udp.RecievedPacketsUDP.get();
				packetsSentLastTotal = udp.SentPacketsUDP.get();
				
				TPS = ticks;
				FPS = frames;
				ticks = 0;
				frames = 0;
				timer += 1000000000;
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
			int radInf = (int) ((client.radOfInf) * 0.5 * windowHeight);
			
			g.drawOval(x - rad, y - rad, 2*rad, 2*rad);
			g.setColor(Color.BLUE);
			g.drawOval(x - radInf, y - radInf, 2*radInf, 2*radInf);
			
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
		var t = Thread.currentThread();
		t.setName("Main-Loop");
		
		var main = new Main();
		main.createDisplay();
		main.setupConnections();
		main.mainLoop();
	}
}
