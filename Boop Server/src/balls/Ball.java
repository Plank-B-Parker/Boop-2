package balls;

import java.awt.Color;
import java.awt.Graphics2D;
import main.Main;
import math.Physics;
import math.Vec2f;

public class Ball {

	private int ID = -1;
	private BallType type;
	
	//-1 if no one owns it, -2 if ball is contested.
	public long ownerID = -1;
	
	private boolean toBeRemoved = false;

	//Contains physics attributes: pos, vel.
	public Physics phys = new Physics(this);

	
	public Ball(int type) {
		setType(type);
	}
	
	public Ball(BallType type) {
		this.type = type;
		phys.setType(this.type);
	}
	
	public float[] getData() {
		float data[] = new float[7];
		data[0] = ID;
		data[1] = type.ordinal();
		data[2] = phys.pos.x;
		data[3] = phys.pos.y;
		data[4] = phys.vel.x; 
		data[5] = phys.vel.y;
		data[6] = ownerID;
		return data;
	}
	
	public void render(Graphics2D g, float dt) {
		Vec2f pos = phys.pos;
		//Vec2f vel = phys.pos;
		
		float x = pos.x;
		float y = pos.y;
		
		//Scaling for screen.
		int X = (int)((x + 1)*0.5*Main.windowHeight);
		int Y = (int)((y + 1)*0.5*Main.windowHeight);
		int Rad = (int)(0.5*type.getRadius()*Main.windowHeight);
		
		//Second set of coordinates for edge.
		int X2 = X;
		int Y2 = Y;
		
		if(x + type.getRadius() > 1) {
			X2 = X - Main.windowHeight;
		}
		else if(x - type.getRadius() < -1) {
			X2 = X + Main.windowHeight;
		}
		
		if(y + type.getRadius() > 1) {
			Y2 = Y - Main.windowHeight;
		}
		else if(y - type.getRadius() < -1) {
			Y2 = Y + Main.windowHeight;
		}
		
		g.setColor(type.getColour());
		g.fillOval(X - Rad, Y - Rad, 2*Rad, 2*Rad);
		if(X2 != X || Y2 != Y) {
			g.fillOval(X2 - Rad, Y2 - Rad, 2*Rad, 2*Rad);
		}
	}
	
	public void moveBall(Vec2f direction, float scalar) {
		Vec2f.increment(phys.pos, phys.pos, direction, scalar);
	}
	
	public int getID() {
		return ID;
	}
	
	public void setPos(float x, float y) {
		phys.pos.x = x;
		phys.pos.y = y;
	}
	
	public void setPos(Vec2f pos) {
		phys.pos.x = pos.x;
		phys.pos.y = pos.y;
	}
	
	public void setVel(float x, float y) {
		phys.vel.x = x;
		phys.vel.y = y;
	}
	
	public void setVel(Vec2f vel) {
		phys.vel.x = vel.x;
		phys.vel.y = vel.y;
	}
	
	public BallType getType() {
		return type;
	}
	
	public void setType(int type) {
		this.type = BallType.getType(type);
		phys.setType(this.type);
	}

	public float getRad() {
		return type.getRadius();
	}

	public long getOwnerID() {
		return ownerID;
	}
	
	public void setOwnerID(long ID) {
		ownerID = ID;
	}

	public Color getColour() {	
		return type.getColour();
	}
	
	public void setID(int ID) {
		this.ID = ID;
	}

	public void remove() {
		toBeRemoved = true;
	}

	public boolean toBeRemoved(){
		return toBeRemoved;
	}

}
