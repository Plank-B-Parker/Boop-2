package Balls;

import java.util.ArrayList;
import java.util.List;

public class Storage {

	private List<Ball> balls = new ArrayList<>();
	
	public Storage() {
		
	}
	
	public void updateBalls(double dt) {
		
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
