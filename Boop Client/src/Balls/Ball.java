package Balls;

import java.awt.Color;
import java.awt.Graphics2D;

import Math.Vec2f;
import Math.physics;
import Mian.Display;
import Mian.main;

public class Ball {
	// COPIED from server file
	
	//ID of -1 means ball is empty.
	private int ID = -1;
	private int type;
	private float rad;
	private Color colour;
	private int ownerID;
	
	//Contains physics attributes: pos, vel.
	public physics phys = new physics(this);
	
	//Careful with ball creation on the client, I've set it up so that only existing balls can be created.
	//Till we can figure out how to create balls on the client safely.
	public Ball(float[] data) {
		setBall(data);
	}
	
	//data[] = [ID, Type, posX, posY, velX, velY, ownerID]//
	public void setBall(float[] data) {
		ID = (int)data[0];
		type = (int)data[1];
		setType(type);
		phys.targetPos.set(data[2], data[3]);
		phys.pos.set(data[2], data[3]);
		phys.vel.set(data[4], data[5]);
		ownerID = (int)data[6];
		
		//Determine radius from type.
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
	
	public void render(Graphics2D g, float dt) {
		Vec2f pos = phys.pos;
		Vec2f vel = phys.pos;
		
		float x = pos.x;
		float y = pos.y;
		
		//Scaling for screen.
		int X = (int)((x + 1)*0.5 * Display.WINDOW_HEIGHT);
		int Y = (int)((y + 1)*0.5 * Display.WINDOW_HEIGHT);
		int Rad = (int)(0.5*rad * Display.WINDOW_HEIGHT);
		
		//Second set of coordinates for edge.
		int X2 = X;
		int Y2 = Y;
		
		if(x + rad > 1) {
			X2 = X - Display.WINDOW_HEIGHT;
		}
		else if(x - rad < -1) {
			X2 = X + Display.WINDOW_HEIGHT;
		}
		
		if(y + rad > 1) {
			Y2 = Y - Display.WINDOW_HEIGHT;
		}
		else if(y - rad < -1) {
			Y2 = Y + Display.WINDOW_HEIGHT;
		}
		
		g.setColor(colour);
//		g.fillOval(X - Rad, Y - Rad, 2*Rad, 2*Rad);
//		if(X2 != X || Y2 != Y) {
//			g.fillOval(X2 - Rad, Y2 - Rad, 2*Rad, 2*Rad);
//		}
		
		g.fillOval(X - Rad, Y - Rad, 2*Rad, 2*Rad);
		if(X2 != X || Y2 != Y) {
			g.fillOval(X2 - Rad, Y2 - Rad, 2*Rad, 2*Rad);
		}
	}
	
	
	public void update(float dt) {
		phys.update(dt);
	}

	public int getID() {
		return ID;
	}
	
	public void setPos(float x, float y) {
		phys.pos.x = x;
		phys.pos.y = y;
	}
	
	public void setVel(float x, float y) {
		phys.vel.x = x;
		phys.vel.y = y;
	}
	
	public int getType() {
		return type;
	}
	
	public void setType(int type) {
		this.type = type;
		switch(type) {
		case 1:
			phys.mass = 1;
			phys.bounciness = 0.1f;
			rad = 0.01f;
			colour = Color.BLUE;
			break;
		case 2:
			phys.mass = 16;
			phys.bounciness = 0.1f;
			rad = 0.04f;
			colour = Color.RED;
			break;
		case 3:
			phys.mass = 1;
			phys.bounciness = 0.1f;
			rad = 0.1f;
			colour = Color.ORANGE;
			break;
		case 4:
			phys.mass = 1;
			phys.bounciness = 0.1f;
			rad = 0.01f;
			colour = Color.BLUE;
			break;
		}
	}

	public float getRad() {
		return rad;
	}
	
	public void setRad(float rad) {
		this.rad = rad;
	}

	public int getOwnerID() {
		return ownerID;
	}

	public Color getColour() {	
		return colour;
	}
	
	public void setColour(Color colour) {
		this.colour = colour;
	}
	
	public void setID(int ID) {
		this.ID = ID;
	}

}
