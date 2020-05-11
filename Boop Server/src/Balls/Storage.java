package Balls;

import java.util.ArrayList;
import java.util.List;

import Math.physics;

public class Storage {

	private List<Ball> balls = new ArrayList<>();
	
	public Storage() {
		
	}
	
	public void updateBalls(float dt) {
		physics.checkCollision(balls);
		for(Ball ball: balls) {
			ball.update(dt);
		}
	}
	
	public void add(Ball b) {
		balls.add(b);
	}
	
	public void remove(Ball b) {
		balls.remove(b);
	}
	
	public Ball getBall(int ID) {
		return balls.get(ID);
	}

}
