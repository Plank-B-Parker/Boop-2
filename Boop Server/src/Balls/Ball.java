package Balls;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

import Math.Vec2f;
import Math.physics;
import Mian.main;

public class Ball {

	private int ID = -1;
	private int type;
	private float rad;
	private Color colour;
	private int ownerID;
	
	//Contains physics attributes: pos, vel.
	public physics phys = new physics(this);
		
	
	public Ball(int type) {
		this.type = type;
		switch(type) {
		case 1:
			phys.mass = 1;
			phys.bounciness = 0.1f;
			rad = 0.01f;
			colour = Color.BLUE;
			break;
		case 2:
			phys.mass = 1;
			phys.bounciness = 0.1f;
			rad = 0.01f;
			colour = Color.BLUE;
			break;
		case 3:
			phys.mass = 1;
			phys.bounciness = 0.1f;
			rad = 0.01f;
			colour = Color.BLUE;
			break;
		case 4:
			phys.mass = 1;
			phys.bounciness = 0.1f;
			rad = 0.01f;
			colour = Color.BLUE;
			break;
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
	
	public void render(Graphics2D g, float dt) {
		Vec2f pos = phys.pos;
		Vec2f vel = phys.pos;
		
		float x = pos.x;
		float y = pos.y;
		
		//Scaling for screen.
		int X = (int)((x + 1)*0.5*main.windowHeight);
		int Y = (int)((y + 1)*0.5*main.windowHeight);
		int Rad = (int)(0.5*rad*main.windowHeight);
		
		//Second set of coordinates for edge.
		int X2 = X;
		int Y2 = Y;
		
		if(x + rad > 1) {
			X2 = X - main.windowHeight;
		}
		else if(x - rad < -1) {
			X2 = X + main.windowHeight;
		}
		
		if(y + rad > 1) {
			Y2 = Y - main.windowHeight;
		}
		else if(y - rad < -1) {
			Y2 = Y + main.windowHeight;
		}
		
		g.setColor(colour);
		g.fillOval(X - Rad, Y - Rad, 2*Rad, 2*Rad);
		if(X2 != X || Y2 != Y) {
			g.fillOval(X2 - Rad, Y2 - Rad, 2*Rad, 2*Rad);
		}
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

	public float getRad() {
		return rad;
	}

	public int getOwnerID() {
		return ownerID;
	}

	public Color getColour() {	
		return colour;
	}

}
