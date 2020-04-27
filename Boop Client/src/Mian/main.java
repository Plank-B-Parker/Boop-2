package Mian;

import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.image.BufferStrategy;

public class main extends Canvas implements Runnable{

	public boolean running;
	private int FPS = 0;

	public main() {
		// TODO Auto-generated constructor stub
	}
	
	public void run() {
		long last = System.nanoTime();
	    double targetFrames = 60;
	    double targetDt = 1000000000/ targetFrames;
	    long timer = 0;
	    int frames = 0;
	        
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
	    	if(timer/1000000000 > 1) {
	    		FPS = frames;
	    		frames = 0;
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
			createBufferStrategy(3);
			return;
		}
		Graphics g= bufS.getDrawGraphics();
		//DRAWING//
		
		
		//DRAWING//
		g.dispose();
		bufS.show();
	}

	public static void main(String args[]){
		
	}

}
