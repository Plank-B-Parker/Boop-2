package Mian;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import Balls.Ball;
import Balls.Storage;
import Debug.Keyboard;
import Networking.Client;
import Networking.ClientAccept;
import Networking.UDP;

public class main {
	
	public static final int windowWidth = 1280, windowHeight = 720;
	private Canvas canvas = new Canvas();
	public Storage balls = new Storage();
	
	BufferStrategy bs;
	
	public static final boolean deterministicPhysics = true;
	
	public main() {
		
		//Add keyboard listener.
		canvas.addKeyListener(new Keyboard(this));
		
		
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
			balls.add(ball);
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
			balls.add(ball);
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
		
		frame.setSize(windowWidth, windowHeight);
		frame.setLayout(null);
		frame.getContentPane().setBackground(Color.BLACK);
		frame.getContentPane().add(canvas);
		frame.setResizable(true);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		frame.setFocusable(true);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.validate();
	}
	
	ClientAccept clientAcceptor;
	UDP udp;
	public int port = 27000;
	
	public void setupConnections() {
		clientAcceptor = new ClientAccept(port);
		UDP udp = new UDP(port);
		clientAcceptor.startServer();
		udp.startUDP();
	}
	
	private boolean running = false;
	public static boolean paused = false;
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
			
			// Process hardware inputs here <----
			
			// 60 physics update per second
			while (timeAfterLastTick >= MS_PER_UPDATE) {
				
				if(!paused) {
					// If the physics is not deterministic, use the real delta time for physics calculations
					if (!deterministicPhysics) fixedUpdate(timeAfterLastTick/1000f);
					
					// If the physics is deterministic, set a fixed delta time for physics calculations
					else fixedUpdate(1f/60f);
				}
				
				timeAfterLastTick -= MS_PER_UPDATE;
				ticks++;
			}
			
			// Code branch occurs 30 times a second
			if (System.currentTimeMillis() - timer >= MS_PER_UPDATE * 2) {
				
				// Send data and other stuff here
			}
			
			render(timeAfterLastTick/1000f);
			frames++;
			
			if (System.currentTimeMillis() - timer >= 1000) {
				TPS = ticks;
				FPS = frames;
				ticks = 0;
				frames = 0;
				timer += 1000;
			}
		}
		
	}
	
	public void fixedUpdate(float dt) {
		List<Client> clients = new ArrayList<>(clientAcceptor.clients.size());
		clients = List.copyOf(clientAcceptor.clients);
		
		balls.updateBalls(dt);
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
			
			renderGrid(g2d);
			
			balls.renderBalls(g2d, dt);
			
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
	}

	private void drawPerformance(Graphics2D g2d) {
		g2d.setColor(Color.RED);
		g2d.setFont(new Font("Calibri", Font.PLAIN, 20)); 
		g2d.drawString("FPS: " + FPS, 50, 50);
		g2d.drawString("Ticks: " + TPS, 50, 75);
		
		g2d.drawString("TX: " + packetsSentPerSec, 140, 50);
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
