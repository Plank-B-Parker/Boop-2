package Math;

public class physics {
	
	public Vec2f pos;
	public Vec2f vel;
	public Vec2f acc;
	
	public physics() {
		
	}
	
	public void update(float dt) {
		calcAcc();
		Vec2f.increment(vel, vel, acc, dt);
		Vec2f.increment(pos, pos, vel, dt);
	}
	
	//Sets pos components to go between -1 and -1.
	private void normalisePos() {
		pos.x = pos.x % 1f;
		pos.y = pos.y % 1f;
	}
	
	private void calcAcc() {
		acc.set(0, 0);
	}
	
}
