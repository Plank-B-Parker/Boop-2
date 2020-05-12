package Balls;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import Math.Vec2f;
import Math.physics;
import Mian.main;

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
	
	public void renderBalls(Graphics2D g, float dt) {
		for(Ball ball: balls) {
			ball.render(g, dt);
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
