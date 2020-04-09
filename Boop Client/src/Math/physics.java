package Math;

public class physics {
	
	//For pos, x and y go between -1 and 1.
	//If they go above 1, then loop back to -1.
	//If they go below -1, they loop to 1. 
	Vec2f pos;
	Vec2f targetPos;
	
	Vec2f vel;
	Vec2f targetVel;
	
	Vec2f acc;
	
	//Time between getting updates from server.
	private static double server_dt;
	
	
	public physics() {
		// TODO Auto-generated constructor stub
	}
	
	public void update(float dt) {
		calcAcc();
		//Predict target vel and pos.
		Vec2f.increment(targetVel, targetVel, acc, dt);
		Vec2f.increment(targetPos, targetPos, targetVel, dt);
		
		
	}

	private void calcAcc() {
		acc.set(0, 0);
		
	}

}
