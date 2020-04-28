package Balls;

import java.util.ArrayList;
import java.util.Stack;

public class Storage{

	private ArrayList<Ball> balls = new ArrayList<>();
	//List of indexes of writable balls.
	private Stack<Integer> writable = new Stack<>();
	
	public Storage() {
		
	}
	
	public void add(float[] data) {
		if(writable.size() > 0) {
			balls.get(writable.pop()).setBall(data);
		}else {
			balls.add(new Ball(data));
		}
	}
	
	//Figure out how to do this later.
	public void remove(int ID) {
		
	}
	
	public Ball get(int index) {
		return balls.get(index);
	}

}
