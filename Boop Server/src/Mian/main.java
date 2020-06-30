package Mian;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferStrategy;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.WindowConstants;


import Balls.Ball;
import Balls.Storage;
import Debug.Key;
import Debug.Keyboard;
import Debug.Mouse;
import Math.Bitmaths;
import Math.Vec2f;
import Networking.Client;
import Networking.ClientAccept;
import Networking.UDP;

public class main {
	
	public static final int windowWidth = 1280, windowHeight = 720;
	private Canvas canvas = new Canvas();
	public Storage storage = new Storage();
	public Keyboard keyboard;
	public Mouse mouse;
	
	private Ball debug_ball = new Ball(1);
	
	BufferStrategy bs;
	
	public static final boolean deterministicPhysics = true;
	
	private int Counter = 0;
	
	public main() {
		
		keyboard = new Keyboard(this);
		mouse = new Mouse();
		
		//Add keyboard listener.
		canvas.addKeyListener(keyboard);
		canvas.addMouseMotionListener(mouse);
		
		storage.add(debug_ball);
		
		Random random;
		
		// If the physics is deterministic, use a a set seed. Otherwise, use a random seed.
		if (deterministicPhysics) random = new Random(3);
		else random = new Random();
		
		for(int i = 0; i < 200; i++) {
			Ball ball = new Ball(1);
			ball.setPos(2f*(random.nextFloat() - 0.5f), 2f*(random.nextFloat() - 0.5f));
			//ball.setPos(0, -0.98f*i);
			//ball.phys.vel.x = 2f*(random.nextFloat() - 0.5f);
			//ball.phys.vel.y = 2f*(random.nextFloat() - 0.5f);
			storage.add(ball);
		}
		
//		for(int i = 1; i <= 10; i++) {
//			for(int j = 1; j <= 10; j++) {
//				Ball ball = new Ball(1);
//				Ball ball2 = new Ball(1);
//				Ball ball3 = new Ball(1);
//				Ball ball4 = new Ball(1);
//				ball.setPos(i*0.1f, j*0.1f);
//				ball2.setPos(-i*0.1f, j*0.1f);
//				ball3.setPos(i*0.1f, -j*0.1f);
//				ball4.setPos(-i*0.1f, -j*0.1f);
//				balls.add(ball);
//				balls.add(ball2);
//				balls.add(ball3);
//				balls.add(ball4);
//			}
//		}
		
		
		
		for(int i = 0; i < 10; i++) {
			Ball ball = new Ball(2);
			ball.setPos(2f*(random.nextFloat() - 0.5f), 2f*(random.nextFloat() - 0.5f));
			//ball.setPos(0, -0.98f*i);
			ball.phys.vel.x = 2f*(random.nextFloat() - 0.5f);
			ball.phys.vel.y = 2f*(random.nextFloat() - 0.5f);
			storage.add(ball);
		}
		
//		Ball a = new Ball(2);
//		a.setPos(-1f, 0);
//		a.setVel(1f, 0);
//		a.phys.acc.x = 0f;
//		balls.add(a);
//		
//		Ball b = new Ball(2);
//		b.setPos(0.04f, 0);
//		b.setVel(0,0);
//		balls.add(b);
		
//		balls.getBall(0).setVel(0, 0.1f);
//		balls.getBall(1).setVel(0, -0.1f);
		
	}
	
	public void createDisplay() {
		JFrame frame = new JFrame("Server");
		
		canvas.setSize(1920, 1080);
		canvas.setFocusable(true);
		
		frame.setSize(windowWidth, windowHeight);
		frame.setLayout(null);
		frame.getContentPane().setBackground(Color.BLACK);
		frame.getContentPane().add(canvas);
		frame.setResizable(true);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.validate();
	}
	
	ClientAccept clientAcceptor;
	UDP udp;
	
	public void setupConnections() {
		clientAcceptor = new ClientAccept();
		udp = new UDP();
		clientAcceptor.startServer();
		udp.startUDP();
	}
	
	private boolean running = false;
	public static boolean physpaused = false;
	private int TPS = 0;
	private int FPS = 0;
	private int packetsSentPerSec = 0;
	
	public void mainLoop() {
		running = true;
				
		final double MS_PER_UPDATE = 1000.0 / 60.0;
		long previous = System.currentTimeMillis();
		long timeAfterLastTick = 0;
		double timer = System.currentTimeMillis();
		
		int ticks = 0;
		int frames = 0;
		int packetsSent = 0; 
		
		while(running) {
			long current = System.currentTimeMillis();
			long elapsed = current - previous;
			previous = current;
			timeAfterLastTick += elapsed;
			
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
			
			// Code branch occurs 30 times a second
			if (System.currentTimeMillis() - timer >= MS_PER_UPDATE * 2) {
				// Send data and other stuff here
				
				// check if clients are connected then remove them from list
				clientAcceptor.checkClientsConnection();
				
				// send ballz
				sendTestBalls();
				
				Counter++;
			}
			
			render(timeAfterLastTick/1000f);
			frames++;
			
			if (System.currentTimeMillis() - timer >= 1000) {
				//Remove this later:
				System.out.println("num packets sent: " + udp.sentPackets);
				udp.sentPackets = 0;
				
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
		List<Client> clients = new ArrayList<>(clientAcceptor.clients.size());
		clients = List.copyOf(clientAcceptor.clients);
		
		storage.updateBalls(dt);
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
		g.setColor(Color.RED);
		for (Client client: clientAcceptor.clients) {
			int x = (int) ((client.centrePos.x + 1) * 0.5 * windowHeight);
			int y = (int) ((client.centrePos.y + 1) * 0.5 * windowHeight);
			int rad = (int) ((client.radOfInf) * 0.5 * windowHeight);
			
			g.drawOval(x - rad, y - rad, 2*rad, 2*rad);
		}
	}

	private void drawPerformance(Graphics2D g2d) {
		g2d.setColor(Color.RED);
		g2d.setFont(new Font("Calibri", Font.PLAIN, 20)); 
		g2d.drawString("FPS: " + FPS, 50, 50);
		g2d.drawString("Ticks: " + TPS, 50, 75);
		
		g2d.drawString("TX: " + packetsSentPerSec, 140, 50);
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
		Client[] clients = new Client[clientAcceptor.clients.size()];
		System.arraycopy(clientAcceptor.clients.toArray(), 0, clients, 0, clientAcceptor.clients.size());
		
		// sends data to all clients at the same time
		int bytesPerBall = 28;
		int numberOfItems = 7;
		int ballsPerPacket = (UDP.MAX_PAYLOAD_SIZE - 1) / bytesPerBall; // 50 @ payloadSize = 28
		
		// Store balls within a certain area around the client screen inside the Client class TODO
		
		// replace 10 with the max number of packets required TODO
		byte[][][] data = new byte[clients.length][10][UDP.MAX_PAYLOAD_SIZE];
		
		for (int i = 0; i < clients.length; i++) {
			List<Ball> inRange = new ArrayList<>();
			
			for (Ball ball: allBalls) {
				// check if ball is in client area (simple rect)
//				if (ball.phys.pos.x + ball.getRad() >= clients[i].topLeftCorner.x &&
//					ball.phys.pos.x - ball.getRad() <= clients[i].botRightCorner.x &&
//					ball.phys.pos.y - ball.getRad() <= clients[i].botRightCorner.y &&
//					ball.phys.pos.y + ball.getRad() >= clients[i].topLeftCorner.y) {
//						inRange.add(ball);	
//				}
				inRange.add(ball);	
			}
			
		//	System.out.println("num balls sent: " + inRange.size());
			
			int numberOfPackets = (int) Math.ceil((double) inRange.size() / ballsPerPacket);
			int floatsPerPacket = ballsPerPacket * numberOfItems;
			int packetsFilled = 0;
			int lastBall = 0;
			
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
		
		int numPackets = 0;
		// Send packets to client
		for (int i = 0; i < clients.length; i++) {
			for (int j = 0; j < data[i].length; j++) {
				udp.sendData(Bitmaths.pushByteToData((byte) 2, data[i][j]), clients[i].getIpv4Address(), clients[i].getClientPort());
				numPackets++;
			}
		}
		
		//System.out.println("num Packets after sending: " + numPackets);
	}
	
	public static void main(String args[]){
		Thread t = Thread.currentThread();
		t.setName("Main-Loop");
		
		main main = new main();
		main.createDisplay();
		main.setupConnections();
		main.mainLoop();
	}
}
