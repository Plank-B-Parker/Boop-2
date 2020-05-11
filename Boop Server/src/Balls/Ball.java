package Balls;

import Math.Vec2f;
import Math.physics;

public class Ball {

	private int ID = -1;
	private int type;
	private float rad;
	private int ownerID;
	
	//Contains physics attributes: pos, vel.
	private physics phys = new physics();
		
	
	public Ball(int type) {
		this.type = type;
		switch(type) {
		case 1:
			phys.mass = 1;
			phys.bounciness = 1;
			rad = 0.01f;
		case 2:
			phys.mass = 1;
			phys.bounciness = 1;
			rad = 0.01f;
		case 3:
			phys.mass = 1;
			phys.bounciness = 1;
			rad = 0.01f;
		case 4:
			phys.mass = 1;
			phys.bounciness = 1;
			rad = 0.01f;
		}
	}
	
	public float[] getData() {
		float data[] = new float[7];
		data[0] = ID;
		data[1] = type;
		data[2] = phys.pos.x;
		data[3] = phys.pos.y;
		data[4] = phys.vel.x; 
		data[5] = phys.vel.y;
		data[6] = ownerID;
		return data;
	}
	
	public void update(float dt) {
		phys.update(dt);
	}
	
	public int getID() {
		return ID;
	}
	
	public Vec2f getPos() {
		return phys.pos;
	}
	public Vec2f getVel() {
		return phys.vel;
	}
	
	public float getMass() {
		return phys.mass;
	}
	
	public float getBounciness() {
		return phys.bounciness;
	}
	public int getType() {
		return type;
	}

	public float getRad() {
		return rad;
	}

	public int getOwnerID() {
		return ownerID;
	}

}
