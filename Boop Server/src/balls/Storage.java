package balls;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import main.Main;
import math.Vec2f;
import math.Physics;

public class Storage {

	public int numBalls = 0;
	
	private List<Ball> balls = new ArrayList<>();
	private List<Ball> removedBalls = new ArrayList<>();
	
	public Storage() {
		balls = Collections.synchronizedList(balls);
	}
	
	public void updateBalls(float dt) {
		Physics.checkCollision(this);
		
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

		List<Ball>ballsToRemove = new ArrayList<Ball>();
		synchronized (balls) {
			for(Ball ball: balls) {
				if(ball.getID() != -1) {
					if(ball.toBeRemoved()) ballsToRemove.add(ball);
				}
			}
			for(Ball ball: ballsToRemove) remove(ball);
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
		b.setID(createNewBallID());
		balls.add(b);
		numBalls++;
	}
	
	int maxID = 0;
	private int createNewBallID() {
		return ++maxID;
	}
	
	private int removedCount = 0;
	public void remove(Ball b) {
		balls.remove(b);
		numBalls--;
		//Top secret: Classified code right here.
		removedBalls.add(b);
		System.out.println(removedCount++);
	}
	
	public Ball getBall(int index) {
		return balls.get(index);
	}
	
	public Collection<Ball> getBalls() {
		return Collections.unmodifiableCollection(balls);
	}

}