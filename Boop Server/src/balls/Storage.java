package balls;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import math.Physics;
import networking.Client;

public class Storage {

	public int numBalls = 0;
	
	private List<Ball> balls = new ArrayList<>();
	private List<Ball> removedBalls = new ArrayList<>();
	
	
	public Storage() {
		
	}
	
	
	public void updateBalls(List<Client> clients, float dt) {
		Physics.checkCollision(this);
		
		//Set acceleration to 0 to begin with calculate drag force.
		for(Ball ball: balls) {
			ball.phys.nullifyForces();
			ball.phys.calcDrag();
		}
		
		if(clients != null) { 
			//Calculate attraction of all magnetic balls.
			for(Client client: clients) {
				for(Ball ball: client.localBalls) {
					ball.phys.calcAttraction(client.localBalls);
					ball.phys.calcClientAttraction(client.centrePos, 0.01f);
				}
			}
		}
		
		for(Ball ball: balls) {
			ball.phys.update(dt);
		}

		List<Ball>ballsToRemove = new ArrayList<>();
		for(Ball ball: balls) {
			if(ball.getID() != -1 && ball.toBeRemoved()) {
				ballsToRemove.add(ball);
			}
		}
		
		for(Ball ball: ballsToRemove) remove(ball);
		
	}
	
	public void renderBalls(Graphics2D g, float dt) {
		float energy = 0;
		
		for (Ball ball: balls) {
			ball.render(g, dt);
			energy += ball.phys.calcEnergy(balls);
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
	
	/**
	 * @return A reference to an unmodifiable list of all balls. (Elements are still mutable).
	 */
	public List<Ball> getBalls() {
		return Collections.unmodifiableList(balls);
	}

}
