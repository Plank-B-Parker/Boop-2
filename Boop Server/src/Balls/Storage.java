package Balls;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import Math.Vec2f;
import Math.physics;
import Mian.main;

public class Storage {

	private List<Ball> balls = new ArrayList<>();
	private List<Ball> removedBalls = new ArrayList<>();
	
	public Storage() {
		balls = Collections.synchronizedList(balls);
	}
	
	public void updateBalls(float dt) {
		physics.checkCollision(balls);
		
		synchronized (balls) {
			for(Ball ball: balls) {
				ball.phys.calcAcc(balls);
			}
		}
		
		synchronized (balls) {
			for(Ball ball: balls) {
				ball.phys.update(dt);
			}
		}
		
	}
	
	public void renderBalls(Graphics2D g, float dt) {
		float energy = 0;
		
		synchronized (balls) {
			for (Ball ball: balls) {
				ball.render(g, dt);
				energy += ball.phys.calcEnergy(balls);
			}
		}

		g.setColor(Color.PINK);
		g.drawString("energy: " + energy, 20, 200);
	}
	
	public void add(Ball b) {
		balls.add(b);
		b.setID(balls.size()-1);
	}
	
	private int removedCount = 0;
	public void remove(Ball b) {
		balls.remove(b);
		//Top secret: Classified code right here.
		removedBalls.add(b);
		System.out.println(removedCount++);
	}
	
	public Ball getBall(int ID) {
		return balls.get(ID);
	}
	
	public Collection<Ball> getBalls() {
		return Collections.unmodifiableCollection(balls);
	}

}
