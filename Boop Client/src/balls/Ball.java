package balls;

import java.awt.Color;
import java.awt.Graphics2D;

import main.Main;
import math.Vec2f;
import math.VecPool;
import math.Physics;
import display.Display;

public class Ball {
	
	//ID of -1 means ball is empty, -2 means it's client created.
	private int ID = -1;
	private int type;
	private float rad;
	private Color colour;
	private int ownerID;
	
	private boolean timed = true;
	private float timeAlive = 0;

	private boolean toBeRemoved = false;

	private VecPool vecPool = new VecPool();
	
	//Contains physics attributes: pos, vel.
	public Physics phys = new Physics(this);
	
	//For balls from server.
	public Ball(float[] data) {
		setBall(data);
	}
	
	public Ball(int type) {
		setType(type);
	}
	
	//data[] = [ID, Type, posX, posY, velX, velY, ownerID]//
	public void setBall(float[] data) {
		ID = (int)data[0];
		type = (int)data[1];
		setType(type);
		phys.clientPos.set(data[2], data[3]);
		phys.pos.set(data[2], data[3]);
		phys.vel.set(data[4], data[5]);
		ownerID = (int)data[6];
		
		resetTimer();
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
	
	//Render method below renders whole server.
	public void render2(Graphics2D g, float dt) {

		vecPool.startOfMethod();

		Vec2f pos = vecPool.getVec();

		Vec2f.disp(pos, phys.clientPos, Display.centreInServer);

		float x = pos.x;
		float y = pos.y;

		double a = Math.sqrt(2)*Display.diameterInServer / 4;
		double b = Math.sqrt(2)/Display.diameterInServer;
		
		//Scaling for screen.
		int X = (int)((x + a)*b*Display.WINDOW_WIDTH);
		int Y = (int)(((y + a)*b*Display.WINDOW_WIDTH) - (Display.WINDOW_WIDTH - Display.WINDOW_HEIGHT)/2);
		int Rad = (int)(b*rad*Display.WINDOW_WIDTH);
		
		//Second set of coordinates for edge.
		int X2 = X;
		int Y2 = Y;
		
		if(x + rad > 1) {
			X2 = (int) (X - 2*b*Display.WINDOW_WIDTH);
		}
		else if(x - rad < -1) {
			X2 = (int) (X + 2*b*Display.WINDOW_WIDTH);
		}
		
		if(y + rad > 1) {
			Y2 = (int) (Y - 2*b*Display.WINDOW_WIDTH);
		}
		else if(y - rad < -1) {
			Y2 = (int) (Y + 2*b*Display.WINDOW_WIDTH);
		}
		
		g.setColor(colour);
		g.fillOval(X - Rad, Y - Rad, 2*Rad, 2*Rad);
		if(X2 != X || Y2 != Y) {
			g.fillOval(X2 - Rad, Y2 - Rad, 2*Rad, 2*Rad);
		}
		
		/////FOR DEBUGGING: TIMER
//		g.setColor(Color.WHITE);
//		g.drawString("t:" + (float)(Math.round(100*timeAlive))/100f, X, Y);
		////////

		vecPool.endOfMethod();
		
		// render3(g,dt);
	}
	
	public void render3(Graphics2D g, float dt) {
		Vec2f pos = phys.pos;
		
		float x = pos.x;
		float y = pos.y;
		
		//Scaling for screen.
		int X = (int)((x + 1)*0.5*Display.WINDOW_HEIGHT);
		int Y = (int)((y + 1)*0.5*Display.WINDOW_HEIGHT);
		int Rad = (int)(0.5*rad*Display.WINDOW_HEIGHT);
		
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
		
		g.setColor(Color.red);
		g.fillOval(X - Rad, Y - Rad, 2*Rad, 2*Rad);
		if(X2 != X || Y2 != Y) {
			g.fillOval(X2 - Rad, Y2 - Rad, 2*Rad, 2*Rad);
		}
	}
	
	
	public void render(Graphics2D g, float dt) {
		float posX = phys.clientPos.x;
		float posY = phys.clientPos.y;
		
		float serverWidth = Display.diameterInServer*Display.aspectRatio;
		float serverHeight = Display.diameterInServer;
		
		float x = posX - Display.centreInServer.x;
		float y = posY - Display.centreInServer.y;
		
		if(x > 1) {
			x = x-2;
		}
		if(x < -1) {
			x = x+2;
		}
		
		if(y > 1) {
			y = y-2;
		}
		if(y < -1) {
			y = y+2;
		}
		
		x += serverWidth/2;
		y += serverHeight/2;
		
		int X = (int)(Display.WINDOW_WIDTH*x/serverWidth);
		int Y = (int)(Display.WINDOW_HEIGHT*y/serverHeight);
		
		int Rad = (int)(Display.WINDOW_HEIGHT*rad/serverHeight);
		
		g.setColor(colour);
		g.fillOval(X - Rad, Y - Rad, 2*Rad, 2*Rad);
		
		render4(g,dt);
	}
	
	public void render4(Graphics2D g, float dt) {
		float posX = phys.pos.x;
		float posY = phys.pos.y;
		
		float serverWidth = Display.diameterInServer*Display.aspectRatio;
		float serverHeight = Display.diameterInServer;
		
		float x = posX - Display.centreInServer.x;
		float y = posY - Display.centreInServer.y;
		
		if(x > 1) {
			x = x-2;
		}
		if(x < -1) {
			x = x+2;
		}
		
		if(y > 1) {
			y = y-2;
		}
		if(y < -1) {
			y = y+2;
		}
		
		x += serverWidth/2;
		y += serverHeight/2;
		
		int X = (int)(Display.WINDOW_WIDTH*x/serverWidth);
		int Y = (int)(Display.WINDOW_HEIGHT*y/serverHeight);
		
		int Rad = (int)(Display.WINDOW_HEIGHT*rad/serverHeight);
		
		g.setColor(Color.red);
		g.fillOval(X - Rad, Y - Rad, 2*Rad, 2*Rad);
	}
	
	//For checking if ball hasn't been updated in a while.
	public void updateTimer(float dt) {
		if(timed) {
			timeAlive += dt;
		}
	}
	public void resetTimer() {
		timeAlive = 0;
	}
	public float getTimeAlive() {
		return timeAlive;
	}
	public void startTimer() {
		timed = true;
	}
	public void stopTimer(){
		timed = false;
	}
	public void updateBall(float[] data) {
		type = (int)data[1];
		float x = data[2];
		float y = data[3];
		float velx = data[4];
		float vely = data[5];
		ownerID = (int)data[6];
		
		setPos(x,y);
		setVel(velx, vely);
		
		resetTimer();
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
			rad = 0.01f;
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

	public void remove() {
		toBeRemoved = true;
	}

	public boolean toBeRemoved(){
		return toBeRemoved;
	}

}
