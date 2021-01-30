package main;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferStrategy;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Random;

import balls.Storage;
import display.Display;
import math.Bitmaths;
import math.Vec2f;
import networking.ServerLink;
import networking.UdpLink;

public class Main {

	public volatile boolean running = false;
	private Canvas canvas = new Canvas();
	public Display display;
	public ServerLink serverLink;
	public UdpLink udpLink;
	
	public volatile boolean disconnectedByServer = false;
	
	public Storage balls = new Storage();
	
	public Vec2f pos;
	
	private Keyboard keyboard;
	private Mouse mouse;
	
	public Main() {

		keyboard = new Keyboard();
		mouse = new Mouse();
		canvas.addKeyListener(keyboard);
		canvas.addMouseMotionListener(mouse);
		
		Random random = new Random();
		
		/*for(int i = 0; i < 10; i++) {
			Ball ball = new Ball(2);
			ball.setPos(2f*(random.nextFloat() - 0.5f), 2f*(random.nextFloat() - 0.5f));
			//ball.setPos(0, -0.98f*i);
			ball.phys.vel.x = 2f*(random.nextFloat() - 0.5f);
			ball.phys.vel.y = 2f*(random.nextFloat() - 0.5f);
			balls.add(ball);
		}*/
		
	}
	
	public void createDisplay() {
		display = new Display(this, canvas);
	}
	
	public void setupConnections() {
		serverLink = new ServerLink(this);
		udpLink = new UdpLink(this);
	}
	
	private int FPS = 0;
	private int TPS = 0;
	private int packetsSentPerSec = 0;
	private int packetsRecievedPerSec = 0;
	private int packetsRecievedLastTotal = 0;
	private int packetsSentLastTotal = 0;
	
	private int count = 0;
	public void mainLoop() {
		
		final double MS_PER_UPDATE = 1000.0 / 60.0;
		long previous = System.currentTimeMillis();
		long timeAfterLastTick = 0;
	    long timer = System.currentTimeMillis();
	    long networkTimer = System.currentTimeMillis();
	    
	    int ticks = 0;
	    int frames = 0;
	    
	    while(running) {
	    	long current = System.currentTimeMillis();
			long elapsed = current - previous;
			previous = current;
			timeAfterLastTick += elapsed;
			
			processInputs();
			
			// 60 physics update per second
			while (timeAfterLastTick >= MS_PER_UPDATE) {
				// use deterministic from server?????????
				fixedUpdate(1f / 60f);
				timeAfterLastTick -= MS_PER_UPDATE;
				ticks++;
			}
			
			// Code branch occurs 30 times a second
			if (System.currentTimeMillis() - networkTimer >= MS_PER_UPDATE * 2) {
				if (disconnectedByServer && serverLink.getServerConnection()) {
					disconnectServer();
					disconnectedByServer = false;
				}
				
				if (serverLink.getServerConnection()) {

					// Checks if any hold keys have been pressed or released and sends any changes to the server
					Key[] keys = new Key[] {Key.W, Key.A, Key.S, Key.D};
					for (int i = 0; i < keys.length; i++) {

						Key key = keys[i];
						
						if (keyboard.hasChanged(key)){

							// Sends the key number as a byte, with the 6th bit set to 1 if the key
							// has been pressed or 0 if it has been released

							byte data = (byte) i;

							if (keyboard.isActive(key)) {
								data = (byte) (data | (1 << 6));
							}
							
							udpLink.sendData(new byte[] {1, data});
						}
					}

					if(mouse.mouseMoved) {
						mouse.mouseMoved = false;
						byte[] data = new byte[0];
						data = Bitmaths.pushByteArrayToData(Bitmaths.floatToBytes(mouse.playerVel.x), data);
						data = Bitmaths.pushByteArrayToData(Bitmaths.floatToBytes(mouse.playerVel.y), data);
						data = Bitmaths.pushByteToData((byte)5, data);
						udpLink.sendData(data);
					}

					udpLink.processServerUpdate();
				}

				// Code branch occurs 1 time a second
				if (System.currentTimeMillis() - timer >= MS_PER_UPDATE * 60) {
					if (disconnectedByServer && serverLink.getServerConnection()) {
						disconnectServer();
						disconnectedByServer = false;
					}
					
					if (serverLink.getServerConnection()) {

						// Checks if any hold keys have been pressed or released and sends any changes to the server
						Key[] keys = new Key[] {Key.W, Key.A, Key.S, Key.D};
						for (int i = 0; i < keys.length; i++) {

							Key key = keys[i];
							
							if (keyboard.hasChanged(key)){

								// Sends the key number as a byte, with the 6th bit set to 1 if the key
								// has been pressed or 0 if it has been released

								byte data = (byte) i;

								if (keyboard.isActive(key)) {
									data = (byte) (data | (1 << 6));
								}
								
								udpLink.sendData(new byte[] {1, data});
							}
						}
						
						
//						if(mouse.mouseMoved) {
//							mouse.mouseMoved = false;
//							byte[] data = new byte[0];
//							data = Bitmaths.pushByteArrayToData(Bitmaths.floatToBytes(mouse.mouseDir.x), data);
//							data = Bitmaths.pushByteArrayToData(Bitmaths.floatToBytes(mouse.mouseDir.y), data);
//							data = Bitmaths.pushByteToData((byte)5, data);
//							udpLink.sendData(data);
//						}


					}

					udpLink.processServerUpdate();
				}
				
				networkTimer += MS_PER_UPDATE * 2;
			}
			
			render(timeAfterLastTick / 1000f);
			frames++;
			
			if (System.currentTimeMillis() - timer >= 1000) {
				packetsRecievedPerSec = udpLink.recievedPacketsUDP.get() - packetsRecievedLastTotal;
				packetsSentPerSec = udpLink.sentPacketsUDP.get() - packetsSentLastTotal;
				
				packetsRecievedLastTotal = udpLink.recievedPacketsUDP.get();
				packetsSentLastTotal = udpLink.sentPacketsUDP.get();
				TPS = ticks;
				FPS = frames;
				count =0;
				ticks = 0;
				frames = 0;
				timer += 1000;
			}
	    	
	    }
	}
	
	private void processInputs() {
		
	}
	
	private void fixedUpdate(float dt) {
		balls.updateBalls(dt);
		updatePos(dt);
	}

	static float movementSpeed = 0.3f;
	private void updatePos(float dt) {
		if (keyboard.isActive(Key.W)) {
			Display.centreInServer.y -= movementSpeed*dt;
			if (Display.centreInServer.y < -1) Display.centreInServer.y += 2;
		}

		if (keyboard.isActive(Key.A)) {
			Display.centreInServer.x -= movementSpeed*dt;
			if (Display.centreInServer.x < -1) Display.centreInServer.x += 2;
		}

		if (keyboard.isActive(Key.S)) {
			Display.centreInServer.y += movementSpeed*dt;
			if (Display.centreInServer.y > 1) Display.centreInServer.y -= 2;
		}

		if (keyboard.isActive(Key.D)) {
			Display.centreInServer.x += movementSpeed*dt;
			if (Display.centreInServer.x > 1) Display.centreInServer.x -= 2;
		}
		
		Vec2f.increment(Display.centreInServer, Display.centreInServer, mouse.playerVel, dt);
		
		if (Display.centreInServer.y < -1) Display.centreInServer.y += 2;
		if (Display.centreInServer.x < -1) Display.centreInServer.x += 2;
		if (Display.centreInServer.y > 1) Display.centreInServer.y -= 2;
		if (Display.centreInServer.x > 1) Display.centreInServer.x -= 2;
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
			balls.renderBalls(g2d, dt);
			drawPerformance(g2d);
			
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
	
	// Called by the Display Class when user starts game
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
		Thread t = Thread.currentThread();
		t.setName("Main-Loop");
		
		Main main = new Main();
		main.createDisplay();
		main.setupConnections();
		main.running = true;
		main.mainLoop();
	}

}
