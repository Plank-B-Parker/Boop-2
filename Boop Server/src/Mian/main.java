package Mian;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import Balls.Ball;
import Balls.Storage;
import Networking.Client;
import Networking.ClientAccept;
import Networking.UDP;

public class main {
	
	public static final int windowWidth = 1280, windowHeight = 720;
	private Canvas canvas = new Canvas();
	public Storage balls = new Storage();
	
	BufferStrategy bs;
	
	public main() {
		for(int i = 0; i < 10; i++) {
			Ball ball = new Ball(1);
			ball.getVel().x = 2*((float)(Math.random()) - 0.5f);
			ball.getVel().y = 2*((float)(Math.random()) - 0.5f);
			balls.add(ball);
		}
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
				fixedUpdate(timeAfterLastTick);
				timeAfterLastTick -= MS_PER_UPDATE;
				ticks++;
			}
			
			// Code branch occurs 30 times a second
			if (System.currentTimeMillis() - timer >= MS_PER_UPDATE * 2) {
				
				// Send data and other stuff here
			}
			
			render(timeAfterLastTick);
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
		//System.arraycopy(clientAcceptor.clients, 0, clients, 0, clients.size());
		
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
			
			balls.renderBalls(g2d, dt);
			
			drawPerformance(g2d);
			
			g2d.dispose();
			bs.show();
		}while(bs.contentsLost());
	}

	private void drawPerformance(Graphics2D g2d) {
		g2d.setColor(Color.RED);
		g2d.setFont(new Font("SansSerif", Font.PLAIN, 20)); 
		g2d.drawString("fps: " + FPS, 50, 50);
		g2d.drawString("ticks: " + TPS, 50, 75);
		
		g2d.drawString("tx: " + packetsSentPerSec, 140, 50);
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
