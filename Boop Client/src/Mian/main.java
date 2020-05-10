package Mian;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferStrategy;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

public class main extends Canvas implements Runnable{

	public boolean running = false;
	private int FPS = 0;
	private int UPS = 0;
	private int packetsSentPerSec = 0;

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
	
	public void run() {
		long last = System.nanoTime();
	    double targetFrames = 60;
	    double targetDt = 1000000000/ targetFrames;
	    long timer = 0;
	    int frames = 0;
	    int packetsSent = 0;
	        
	    while(running) {
	    	long now = System.nanoTime();
	    	long dt = now - last;
	    	if(dt/targetDt > 1) {
	    		render();
	    		tick(dt);
	    		now  = last;
	    		timer += dt;
	    		frames++;
	    		
	    	}
	    	if(timer/1000000000 >= 1) {
	    		FPS = frames;
	    		packetsSentPerSec = packetsSent;
	    		frames = 0;
	    		packetsSent = 0;
	    	}
	    }
	}
	
	//Updates every frame.
	private void tick(long dt) {
		
	}
	//^
	private void render() {
		BufferStrategy bufS = getBufferStrategy();
		if(bufS == null){
			createBufferStrategy(2);
			return;
		}
		Graphics g= bufS.getDrawGraphics();
		//DRAWING//
		
		
		//DRAWING//
		g.dispose();
		bufS.show();
	}

	public void drawPerformance(Graphics g) {
		g.setColor(Color.RED);
		g.setFont(new Font("SansSerif", Font.PLAIN, 20));
		g.drawString("fps: " + FPS, 50, 50);
		
		g.drawString("tx: " + packetsSentPerSec, 70, 50);
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
