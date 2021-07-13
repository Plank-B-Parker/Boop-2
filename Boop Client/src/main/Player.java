package main;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import balls.Ball;
import math.Physics;
import math.Vec2f;
import math.VecPool;

public class Player {
	
	public long ID = 0;
	boolean isClient; 		// checks if this player is the current client.
	
	private long msPing = 0;
	
	public float timeSinceLastUpdate = 0;
	
	public Color colour;
	public String name;

	public Vec2f centrePos = new Vec2f(); 	//centre of screen of client.
	public Vec2f velocity = new Vec2f();
	public float radOfInf = 0.5f;			//radius of region balls are attracted to the client.
	private static float maxSpeed = 0.3f;
	
	//Bellow are the variables above, from the server.
	private Vec2f centreInServer = new Vec2f();	
	private Vec2f velocityInServer = new Vec2f();
	private float radInServer = 0.5f;
	
	//Acceleration used to match speed and position on server.
	private Vec2f acceleration = new Vec2f();
	private Vec2f trackingVel = new Vec2f();  //Velocity used to smoothly reduce displacement error.
	private float cP = 0.001f;
	private float cV = -0.005f;
	
	
	public static final float attractionCoefficient = 0.001f; //multiplied by number of owned balls to give attraction strength.
	public static final float influenceCoefficient = 0.01f; //multiplied by number of balls to give area of influence. 
	//NOTE: May make radius of influence proportional to number of local balls so, rate of area increase slows
	//		as it gets bigger.
	
	public List<Ball> ownedBalls = new ArrayList<>();	//list of balls that the player possesses.
	public List<Ball> localBalls = new ArrayList<>(); // All balls in the territory.
	
	public Player(boolean isClient, long ID, String name, Color colour, Vec2f pos) {
		this.ID = ID;
		this.isClient = isClient;
		centrePos = pos;
		this.name = name;
		this.colour = colour;
	}
	
	VecPool tempVecs = new VecPool();
	//checks if a ball is with in the attraction zone of the player.
	public boolean isInReach(Ball b) {
		tempVecs.startOfMethod();
		/////////////////////////
		Vec2f disp = tempVecs.getVec();
		Physics.disp(disp, centrePos, b.phys.pos);
		///////////////////////
		tempVecs.endOfMethod();
		
		return (disp.lengthSq() <= (radOfInf + b.getRad())*(radOfInf+b.getRad()));
	}
	
	public void updatePos(float dt) {
		Vec2f.increment(centrePos, centrePos, velocity, dt);
		
		if (centrePos.y < -1) centrePos.y += 2;
		if (centrePos.x < -1) centrePos.x += 2;
		if (centrePos.y > 1) centrePos.y -= 2;
		if (centrePos.x > 1) centrePos.x -= 2;
	}
	
	public void updateVel(float dt) {
		Vec2f.increment(trackingVel, trackingVel, acceleration, dt);
		if(this == PlayerHandler.Me)
			Vec2f.scale(velocity, direction, maxSpeed);
		else
			Vec2f.scale(velocity, velocity, 0);
		
		Vec2f.add(velocity, velocity, trackingVel);
	}
	
	public void updateTimer(float dt) {
		timeSinceLastUpdate += dt;
	}
	
	public void resetTimer() {
		timeSinceLastUpdate = 0;
	}
	
	public static Vec2f direction = new Vec2f(0,0);
	public static void processInputs(Keyboard keyboard, Mouse mouse) {
		//Reset direction.
		Vec2f.scale(direction, direction, 0);
	
		//Calculate direction from keyboard.
		if (keyboard.isActive(Key.W)) {
			direction.y -= 1;
		}

		if (keyboard.isActive(Key.A)) {
			direction.x -= 1;
		}

		if (keyboard.isActive(Key.S)) {
			direction.y += 1;
		}

		if (keyboard.isActive(Key.D)) {
			direction.x += 1;
		}
		
		//Keep direction length = 1.
		if (direction.lengthSq() != 0)
			direction.normalise();
		
		//Add mouse direction
		Vec2f.add(direction, direction, mouse.mouseDir);
		//Normalise direction if too big.
		if (direction.lengthSq() > 1) {
			direction.normalise();
		}
		
		// Deadzone
		if (direction.lengthSq() < 0.01) {
			direction.x = 0;
			direction.y = 0;
		}
		
	}
	
	public void serverUpdate(float posX, float posY, float velX, float velY, float radOfInf) {
		centreInServer.x = posX;
		centreInServer.y = posY;
		velocityInServer.x = velX;
		velocityInServer.y = velY;
		this.radInServer = radOfInf;
	}
	
	public void updateAcc() {
		// acc = cP*(posServer - pos) + cV(velServer - vel)
		
		//cP*(posServer - pos)
		var posTerm = Vec2f.sub(centreInServer, centrePos);
		posTerm = Vec2f.scale(posTerm, cP);
		
		//cV*(velServer - vel)
		var velTerm = Vec2f.sub(velocityInServer, velocity);
		velTerm = Vec2f.scale(velTerm, cV);
		
		//posTerm + velTerm
		acceleration = Vec2f.add(posTerm, velTerm);
	}

	public final long getMsPing() {
		return msPing;
	}

	public final void setMsPing(long msPing) {
		this.msPing = msPing;
	}
	
}

