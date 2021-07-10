package main;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferStrategy;
import java.io.IOException;
import java.net.InetAddress;

import balls.Storage;
import display.Display;
import math.Bitmaths;
import math.Vec2f;
import networking.Packet;
import networking.ServerLink;
import networking.UdpLink;

public class Main {

	public volatile boolean running = false;
	protected Canvas canvas = new Canvas();
	public Display display;
	public ServerLink serverLink;
	public UdpLink udpLink;
	
	private long[] timeSinceLastPacket = new long[Packet.values().length];
	public volatile boolean disconnectedByServer = false;
	
	public Storage storage = new Storage();
	public PlayerHandler players = new PlayerHandler();
	
	public Vec2f pos;
	
	protected Keyboard keyboard;
	protected Mouse mouse;

	protected boolean doRender = true;
	
	public void createDisplay() {
		display = new Display(this, canvas);

		keyboard = new Keyboard();
		mouse = new Mouse();
		
		display.addKeyListener(keyboard);
		canvas.addMouseMotionListener(mouse);
	}
	
	public void setupConnections() {
		serverLink = new ServerLink(this);
		udpLink = new UdpLink(this);
	}
	
	private static final double TICK_RATE = 60.0;
	private double fps_cap = 60.0;
	
	private int FPS = 0;
	private int TPS = 0;
	private int packetsSentPerSec = 0;
	private int packetsRecievedPerSec = 0;
	private int packetsRecievedLastTotal = 0;
	private int packetsSentLastTotal = 0;
	
	public void mainLoop() {
		
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
			
			// 60 physics update per second
			while (timeAfterLastTick >= NS_PER_UPDATE) {
				// use deterministic from server?????????
				fixedUpdate(1f / 60f);
				timeAfterLastTick -= NS_PER_UPDATE;
				ticks++;
			}
			
			while (timeAfterLastTransmit >= NS_PER_UPDATE) {
				if (disconnectedByServer && serverLink.getServerConnection()) {
					disconnectServer();
					disconnectedByServer = false;
				}
				
				if (!serverLink.getServerConnection() || !udpLink.isConnected()) break;
				
				sendInputs();
				udpLink.processServerUpdate();
				
				timeAfterLastTransmit -= NS_PER_UPDATE;
			}
			
			
			if (doRender && System.nanoTime() - frameTimer >= nsPerFrameLimit) {
				render(timeAfterLastTick / 1000000000f);
				processInput();
				frames++;
				frameTimer += nsPerFrameLimit;
			}
			
			if (System.nanoTime() - timer >= 1000000000) {
				packetsRecievedPerSec = udpLink.recievedPacketsUDP.get() - packetsRecievedLastTotal;
				packetsSentPerSec = udpLink.sentPacketsUDP.get() - packetsSentLastTotal;
				
				packetsRecievedLastTotal = udpLink.recievedPacketsUDP.get();
				packetsSentLastTotal = udpLink.sentPacketsUDP.get();
				
				display.updatePlayerID(PlayerHandler.Me.ID);
				
				TPS = ticks;
				FPS = frames;
				ticks = 0;
				frames = 0;
				timer += 1000000000;
			}

	    }
	}
	
	private void processInput() {
		// Updates current state of data based on latest input.
		if(mouse.mouseMoved || keyboard.somethingHapended) {
			mouse.mouseMoved = false;
			keyboard.somethingHapended = false;
		}
		Player.processInputs(keyboard, mouse);
		
	}
	
	private void sendInputs() {
		// sends any input changes to the server
			
		//System.out.println("(" +  Player.direction.x + ", " +  Player.direction.y+ ")");
		
		if (!isServerReadyForPacket(100, Packet.DUMMY)) return;
		
		byte[] data = new byte[0];
		data = Bitmaths.pushByteArrayToData(Bitmaths.floatToBytes(Player.direction.x), data);
		data = Bitmaths.pushByteArrayToData(Bitmaths.floatToBytes(Player.direction.y), data);
		data = Bitmaths.pushByteToData((byte)5, data);
		udpLink.sendData(data);
	}
	
	private void fixedUpdate(float dt) {
		storage.moveBallsList();
		players.updatePlayers(storage.getBalls(), dt);
		storage.updateBalls(players, dt);
	}
	
	BufferStrategy bs;
	private void render(float dt) {
		bs = canvas.getBufferStrategy();
		if (bs == null) {
			canvas.createBufferStrategy(2);
			return;
		}
		do {
			//having two graphics objects is really expensive.
			Graphics2D g2d = (Graphics2D) bs.getDrawGraphics();
			
			g2d.clearRect(0, 0, Display.WINDOW_WIDTH, Display.WINDOW_HEIGHT);
			storage.renderBalls(g2d, dt);

			if (keyboard.isActive(Key.F)) {
				storage.renderExactCoordinates(g2d, dt);
			}

			drawPerformance(g2d);
			
			//Drawing client centre for debug.
			g2d.setColor(Color.MAGENTA);
			g2d.fillOval(Display.WINDOW_WIDTH/2, Display.WINDOW_HEIGHT/2, 3, 3);
			
			g2d.dispose();
			bs.show();
		}while(bs.contentsLost());
	}

	public void drawPerformance(Graphics2D g2d) {
		g2d.setColor(Color.RED);
		g2d.setFont(new Font("SansSerif", Font.PLAIN, 20));
		g2d.drawString("fps: " + FPS, 50, 50);
		g2d.drawString("ticks: " + TPS, 50, 75);
		
		g2d.drawString("Tx: " + packetsSentPerSec, 140, 50);
		g2d.drawString("Rx: " + packetsRecievedPerSec, 140, 75);
	}
	
	/**
	 * Check if it's time to send the server a particular packet given a delay between the last one.
	 * @param msDelayBetweenPackets Delay in milliseconds between each packet of this type.
	 * @param packet The packet type being sent.
	 */
	public boolean isServerReadyForPacket(float msDelayBetweenPackets, Packet packet) {
		long currentTime = System.currentTimeMillis();
		long lastPacketTime = timeSinceLastPacket[packet.ordinal()];
		long dt = currentTime - lastPacketTime;
		
		
		if (dt < msDelayBetweenPackets) {
			return false;
		}
		
		timeSinceLastPacket[packet.ordinal()] = currentTime;
		return true;
	}
	
	// Called by the Display Class when user starts game (A separate event thread!!!).
	public void connectToServer(InetAddress serverIP) throws IOException {
		serverLink.connectToServer(serverIP);
		udpLink.connectToServerUDP(serverIP, serverLink.getMyPort());
	}
	
	public void disconnectServer() {
		serverLink.stopRunningTCP();
		udpLink.stopRunningUDP();
		System.out.println("Successfully disconnected");
	}
	
	public void startGame() {
		if (running) return;
		
		running = true;
		mainLoop();
		
	}
	
	public void stopGame() {
		// disconnect
		running = false;
	}
	
	
	public static void main(String args[]){
		var t = Thread.currentThread();
		t.setName("Main-Loop");
		
		var main = new Main();
		main.createDisplay();
		main.setupConnections();
		
		main.running = true;
		main.mainLoop();
	}

}
