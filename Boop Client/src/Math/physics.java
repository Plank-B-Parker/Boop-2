package Math;

public class physics {
	
	//For pos, x and y go between -1 and 1.
	//If they go above 1, then loop back to -1.
	//If they go below -1, they loop to 1. 
	Vec2f pos;
	Vec2f targetPos;
	Vec2f posError;
	
	Vec2f vel;
	//Vec2f targetVel;
	//Vec2f velError;
	
	Vec2f acc;
	
	//Time between getting updates from server.
	private static float server_dt = 1f/30f;
	
	
	public physics(Vec2f pos) {
		// TODO Auto-generated constructor stub
	}
	
	//Try changing pos proportional to their error.
	public void update(float dt) {
		calcAcc();
		//Predict target pos.
		Vec2f.increment(vel, vel, acc, dt);
		Vec2f.increment(targetPos, targetPos, vel, dt);
		Vec2f.increment(pos, pos, vel, dt);
		
		//Calc the errors.
		Vec2f.sub(posError, targetPos, pos);
		
		//Correct position.
		Vec2f.increment(pos, pos, posError, dt/server_dt);
		
		normalisePos();
	}
	
	//Sets pos components to go between -1 and -1.
	private void normalisePos() {
		pos.x = pos.x % 1f;
		pos.y = pos.y % 1f;
		
		targetPos.x = targetPos.x % 1f;
		targetPos.y = targetPos.y % 1f;
	}

	private void calcAcc() {
		acc.set(0, 0);
	}

}
