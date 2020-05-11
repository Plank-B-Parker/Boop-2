package Balls;

import Math.Vec2f;
import Math.physics;

public class Ball {

	//ID = -1 if the ball is writable.
	private int ID = -1;
	private int type;
	private float rad;
	private int ownerID;
	
	//Contains physics attributes: pos, vel.
	private physics phys = new physics();
	
	
	public Ball(float[] data) {
		setBall(data);
	}
	
	//data[] = [ID, Type, posX, posY, velX, velY, ownerID]//
	public void setBall(float[] data) {
		ID = (int)data[0];
		type = (int)data[1];
		switch(type) {
		case 1:
			phys.mass = 1;
			phys.bounciness = 1;
		case 2:
			phys.mass = 1;
			phys.bounciness = 1;
		case 3:
			phys.mass = 1;
			phys.bounciness = 1;
		case 4:
			phys.mass = 1;
			phys.bounciness = 1;
		}
		
		phys.setPos(data[2], data[3]);
		phys.setVel(data[4], data[5]);
		ownerID = (int)data[6];
		
		//Determine radius from type.
	}
	
	public void update(float dt) {
		phys.update(dt);
	}

	public int getID() {
		return ID;
	}
	
	public Vec2f getPos() {
		return phys.targetPos;
	}
	public Vec2f getVel() {
		return phys.vel;
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

	public float getBounciness() {
		return phys.bounciness;
	}
	
	public float getMass() {
		return phys.mass;
	}

}
