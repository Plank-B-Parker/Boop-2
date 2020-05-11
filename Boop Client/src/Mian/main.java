package Mian;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferStrategy;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

public class main extends Canvas implements Runnable{

	public boolean running = false;

	public main() {
		// TODO Auto-generated constructor stub
	}
	
	public void createDisplay() {
		JFrame frame = new JFrame("Client");
		
		setSize(1920, 1080);
		
		frame.setSize(1280, 720);
		frame.setLayout(null);
		frame.getContentPane().setBackground(Color.BLACK);
		frame.getContentPane().add(this);
		frame.setResizable(true);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		frame.setFocusable(true);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.validate();
	}
	
	private int FPS = 0;
	private int TPS = 0;
	private int packetsSentPerSec = 0;
	
	public void run() {
		
		final double MS_PER_UPDATE = 1000.0 / 60.0;
		long previous = System.currentTimeMillis();
		long timeAfterLastTick = 0;
	    long timer = System.currentTimeMillis();
	    
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
				fixedUpdate();
				timeAfterLastTick -= MS_PER_UPDATE;
				ticks++;
			}
			
			// Code branch occurs 30 times a second
			if (System.currentTimeMillis() - timer >= MS_PER_UPDATE * 2) {
				
				// Send data and other stuff here
			}
			
			render(timeAfterLastTick / MS_PER_UPDATE);
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
	
	private void fixedUpdate() {
		
	}
	
	BufferStrategy bs;
	private void render(double extrapolate) {
		bs = getBufferStrategy();
		if (bs == null) {
			createBufferStrategy(2);
			return;
		}
		
		do {
			Graphics2D g2d = (Graphics2D) bs.getDrawGraphics();
			
			g2d.clearRect(0, 0, getWidth(), getHeight());;
			
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
		
		g2d.drawString("tx: " + packetsSentPerSec, 140, 50);
	}
	
	public static void main(String args[]){
		Thread t = Thread.currentThread();
		t.setName("Main-Loop");
		
		main main = new main();
		main.createDisplay();
		main.running = true;
		main.run();
	}

}
