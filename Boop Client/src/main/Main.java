package main;

import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferStrategy;
import java.io.IOException;
import java.net.InetAddress;

import balls.Storage;
import display.Display;
import display.Renderer;
import math.Bitmaths;
import math.Vec2f;
import networking.PacketData;
import networking.ServerLink;
import networking.UdpLink;

public class Main {

	public volatile boolean running = false;
	protected Canvas canvas = new Canvas();
	public Display display;
	
	private long[] timeSinceLastPacket = new long[PacketData.getEnums().length];
	public volatile boolean disconnectedByServer = false;
	
	public Storage storage = new Storage();
	public PlayerHandler players = new PlayerHandler();
	public Renderer renderer = new Renderer();
	
	protected Keyboard keyboard;
	protected Mouse mouse;

	protected boolean doRender = true;
	public boolean debugging;
	
	public void createDisplay() {
		display = new Display(this, canvas);

		keyboard = new Keyboard();
		mouse = new Mouse();
		
		display.addKeyListener(keyboard);
		canvas.addMouseMotionListener(mouse);
	}
	
	
	private ServerLink serverLink;
	private ServerLink.ServerLinkHandler serverLinkHandler;
	private UdpLink udpLink;
	private UdpLink.UdpLinkHandler udpLinkHandler;
	
	// Connections are created but not running. Starts from a button in StartMenu class
	public void setupConnections() {
		serverLink = new ServerLink(this);
		serverLinkHandler = serverLink.new ServerLinkHandler();
		udpLink = new UdpLink(this);
		udpLinkHandler = udpLink.new UdpLinkHandler();
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
				
				timeAfterLastTransmit -= NS_PER_UPDATE;
			}
			
			
			if (doRender && System.nanoTime() - frameTimer >= nsPerFrameLimit) {
				display.updateRadiusOfVision(1f/60f);
				render(timeAfterLastTick / 1000000000f);
				processInputs();
				frames++;
				frameTimer += nsPerFrameLimit;
			}
			
			if (System.nanoTime() - timer >= 1000000000) {
				packetsRecievedPerSec = UdpLink.recievedPacketsUDP.get() - packetsRecievedLastTotal;
				packetsSentPerSec = UdpLink.sentPacketsUDP.get() - packetsSentLastTotal;
				
				packetsRecievedLastTotal = UdpLink.recievedPacketsUDP.get();
				packetsSentLastTotal = UdpLink.sentPacketsUDP.get();
				
				display.updatePlayerID(PlayerHandler.Me.ID);
				
				TPS = ticks;
				FPS = frames;
				ticks = 0;
				frames = 0;
				timer += 1000000000;
			}

	    }
	}
	
	private void processInputs() {
		// Updates current state of data based on latest input.
		if(mouse.mouseMoved || keyboard.somethingHapended) {
			mouse.mouseMoved = false;
			keyboard.somethingHapended = false;
		}
		Player.processInputs(keyboard, mouse);
		
	}
	
	private void sendInputs() {
		// sends any input changes to the server
		
		if (!isServerReadyForPacket(100, PacketData.CLIENT_DIR)) return;
		
		byte[] data = new byte[0];
		data = Bitmaths.pushByteArrayToData(Bitmaths.floatToBytes(Player.direction.x), data);
		data = Bitmaths.pushByteArrayToData(Bitmaths.floatToBytes(Player.direction.y), data);
		data = Bitmaths.pushByteToData(PacketData.CLIENT_DIR.getID(), data);
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
			//Render Order: Background, Balls, HUD.
			
			Graphics2D g2d = (Graphics2D) bs.getDrawGraphics();
			renderer.setGraphics(g2d);
			
			//Background:
			g2d.clearRect(0, 0, Display.WINDOW_WIDTH, Display.WINDOW_HEIGHT);
			drawBackGround(renderer);
			
			//Balls.
			storage.renderBalls(g2d, dt, players, keyboard.isActive(Key.F));

			//HUD.
			drawPerformance(g2d);
			drawPlayerCentre(g2d);	//Drawing client centre for debug.
			
			g2d.dispose();
			bs.show();
		}while(bs.contentsLost());
	}
	
	private void drawBackGround(Renderer r) {
		r.g.clearRect(0, 0, Display.WINDOW_WIDTH, Display.WINDOW_HEIGHT);
		
		
		r.g.setColor(Color.gray);
		r.g.setStroke(new BasicStroke(3));
		var numlines = 4;
		for(float i = -1; i <= 1; i+= 1f/(float)numlines) {
			r.drawLineSegment(new Vec2f(i,-1), new Vec2f(i,0.999f), false);
			r.drawLineSegment(new Vec2f(-1,i), new Vec2f(0.999f, i), false);
		}

		r.g.setColor(Color.DARK_GRAY);
		r.g.setFont(new Font("SansSerif", Font.PLAIN, 15));
		for(float x = -1; x <= 1; x += 1/(float)numlines) {
			for(float y = -1; y <= 1; y += 1/(float)numlines) {
				r.drawString("(" + x + ", " + y + ")",new Vec2f(x + 0.0025f,y + 0.0075f), PlayerHandler.Me.centrePos);
			}
		}
		
		
	}
	
	//Make a renderer class with methods like this.
	private void drawPlayerCentre(Graphics2D g) {
		g.setColor(Color.ORANGE);

		
		Vec2f pos = Vec2f.minDisp(PlayerHandler.Me.getExactCentre(), PlayerHandler.Me.centrePos);
		
		float x = pos.x;
		float y = pos.y;

		double a = Math.sqrt(2)*Display.getDiameterOfVision() / 4;
		double b = Math.sqrt(2)/Display.getDiameterOfVision();
		
		//Scaling for screen.
		int X = (int)((x + a)*b*Display.WINDOW_WIDTH);
		int Y = (int)(((y + a)*b*Display.WINDOW_WIDTH) - (Display.WINDOW_WIDTH - Display.WINDOW_HEIGHT)/2);
		
		g.fillOval(X - 3, Y - 3, 2*3, 2*3);
		
		
		g.setColor(Color.MAGENTA);
		g.fillOval(Display.WINDOW_WIDTH/2, Display.WINDOW_HEIGHT/2, 3, 3);
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
	public boolean isServerReadyForPacket(float msDelayBetweenPackets, PacketData packet) {
		long currentTime = System.currentTimeMillis();
		long lastPacketTime = timeSinceLastPacket[packet.ordinal()];
		long dt = currentTime - lastPacketTime;
		
		
		if (dt < msDelayBetweenPackets) {
			return false;
		}
		
		timeSinceLastPacket[packet.ordinal()] = currentTime;
		return true;
	}
	
	// Called by the StartMenu Class when user starts game (A separate event thread!!!).
	public void connectToServer(InetAddress serverIP) throws IOException {
		serverLink.connectToServer(serverIP);
		serverLinkHandler.startServerLinkHandler();
		udpLink.connectToServerUDP(serverIP, serverLink.getMyPort());
		udpLinkHandler.startUdpLinkHandler();
	}
	
	public void disconnectServer() {
		serverLink.stopRunningTCP();
		serverLinkHandler.stopServerLinkHandler();
		udpLink.stopRunningUDP();
		udpLinkHandler.stopUdpLinkHandler();
		System.out.println("Successfully disconnected");
	}
	
	public void startGame() {
		if (running) return;
		
		running = true;
		mainLoop();
		
	}
	
	public void stopGame() {
		// disconnect
		disconnectServer();
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
