package balls;

import java.awt.Color;
import java.awt.Graphics2D;

import display.Display;
import display.Renderer;
import main.PlayerHandler;
import math.Physics;
import math.Vec2f;
import math.VecPool;

public class Ball {
	
	//ID of -1 means ball is empty, -2 means it's client created.
	private int ID = -1;
	private int type;
	private float rad;
	private Color colour;
	private long ownerID = -1;
	
	//Client sees client pos.
	public Vec2f clientPos = new Vec2f();
	public Vec2f clientVel = new Vec2f();
	public Vec2f clientAcc = new Vec2f();
	
	//Coefficients to control how responsive client is.
	private float cA = 0.01f; //acceleration coefficient.
	private float cV = -0.25f; //damping coefficient.
	
	private float timeForCorrection = 1f/15f;
	private float timeLeftForCorrection = 1f/15f;
	
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
		clientPos.set(data[2], data[3]);
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
	
	public void render(Renderer r, float dt, PlayerHandler players, boolean debugging) {
		//FOR DEBUGGING: 
		if(debugging) {
			//ExactCoords
			r.setColour(Color.PINK);
			r.fillCircle(phys.pos, rad);
			//TIMER
			r.setColour(Color.WHITE);
			r.drawString("t:" + (float)(Math.round(100*timeAlive))/100f, clientPos, -10, -10);
			//
		}
		
		Color colour = this.colour;
		if(ownerID == -2) {
			colour = Color.WHITE;
		}
		if(ownerID > 0) {
			boolean pain = (players.getPlayerByID(ownerID) == null);
			if(pain == true && pain != false) {
				System.out.println("ouch, it hurts man. Not cool - class : Ball ");
				System.out.println("is players list null?: " + players == null);
				System.out.println("number of players: " + players.size());
				System.out.println("their ID: " + players.get(0).ID);
			}
			
			colour = players.getPlayerByID(ownerID).colour;
		}
		
		r.setColour(colour);
		
		r.fillCircle(clientPos, rad);
	}
	
	public void updateClientPrediction(float dt) {
		calcClientCorrAcc(dt);
		Vec2f.increment(clientVel, clientVel, clientAcc, dt);
		Vec2f.increment(clientPos, clientPos, clientVel, dt);
		normaliseClientPos();
	}
	
	//Uses implicit method for fun. 
	//But is a bit more complicated, ask Ibraheem for details.
	private void calcClientCorrAcc(float dt) {
		vecPool.startOfMethod();
		/////////////////////////
		
		//cl_acc = (acc - (cV/cA)*(vel - cl_vel) + (1/cA)*(pos - "cl_pos"))/(1 - dt/cA + (dt^2)*(cV/cA)) 

		// -(cV/cA)*(vel - cl_vel)
		Vec2f velTerm = Vec2f.sub(vecPool.getVec(), phys.vel, clientVel);
		Vec2f.scale(velTerm, velTerm, -cV/cA);
		
		//(1/cA)*(pos - "cl_pos")
		Vec2f cl_pos = Vec2f.increment(vecPool.getVec(), clientPos, clientVel, dt);
		Vec2f posTerm = Vec2f.minDisp(vecPool.getVec(), phys.pos, cl_pos);
		Vec2f.scale(posTerm, posTerm, 1f/cA);
		
		//1/(1 - dt/cA + (dt^2)*(cV/cA)
		float scale = 1f/(1 + dt/cA + dt*dt*cV/cA);
		
		//Put all terms together in clientAcc.
		Vec2f.add(clientAcc, phys.acc, velTerm);
		Vec2f.add(clientAcc, clientAcc, posTerm);
		Vec2f.scale(clientAcc, clientAcc, scale);
		
		///////////////////////
		vecPool.endOfMethod();
	}
	
	public void moveBall(Vec2f direction, float scalar) {
		Vec2f.increment(phys.pos, phys.pos, direction, scalar);
		Vec2f.increment(clientPos, clientPos, direction, scalar);
	}
	
	private void normaliseClientPos() {
		if(clientPos.x > 1) 
			clientPos.x = -1 + clientPos.x%1f;
		if(clientPos.x < -1)
			clientPos.x = 1 + clientPos.x%1f;
		
		if(clientPos.y > 1) 
			clientPos.y = -1 + clientPos.y%1f;
		if(clientPos.y < -1)
			clientPos.y = 1 + clientPos.y%1f;
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
		
		setActualPos(x,y);
		setActualVel(velx, vely);
		
		resetTimer();
	}

	public int getID() {
		return ID;
	}
	
	public void setOwnerID(long ID) {
		ownerID = ID;
	}
	
	private void setActualPos(float x, float y) {
		phys.pos.x = x;
		phys.pos.y = y;
	}
	
	private void setActualVel(float x, float y) {
		phys.vel.x = x;
		phys.vel.y = y;
	}
	
	public void setPos(float x, float y) {
		phys.pos.x = x;
		phys.pos.y = y;
		
		clientPos.x = x;
		clientPos.y = y;
	}
	
	public void setPos(Vec2f pos) {
		phys.pos.x = pos.x;
		phys.pos.y = pos.y;
		
		clientPos.x = pos.x;
		clientPos.y = pos.y;
	}
	
	public void setVel(float x, float y) {
		phys.vel.x = x;
		phys.vel.y = y;
		
		clientVel.x = x;
		clientVel.y = y;
	}
	
	public void setVel(Vec2f vel) {
		phys.vel.x = vel.x;
		phys.vel.y = vel.y;
		
		clientVel.x = vel.x;
		clientVel.y = vel.y;
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

	public long getOwnerID() {
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
