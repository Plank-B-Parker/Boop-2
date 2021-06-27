package main;

import java.util.ArrayList;

import balls.Ball;
import display.Display;
import math.Physics;
import math.Vec2f;
import math.VecPool;

public class Player {
	
	public long ID = 0;
	boolean isClient; 		// checks if this player is the current client.
	
	
	public Vec2f centrePos = new Vec2f(); 	//centre of screen of client.
	public Vec2f velocity = new Vec2f();
	public float radOfInf = 0.5f;			//radius of region balls are attracted to the client.
	private static float maxSpeed = 0.3f;
	
	public static float attractionCoefficient = 0.001f; //multiplied by number of owned balls to give attraction strength.
	public static float influenceCoefficient = 0.01f; //multiplied by number of balls to give area of influence. 
	//NOTE: May make radius of influence proportional to number of local balls so, rate of area increase slows
	//		as it gets bigger.
	
	public ArrayList<Ball> ownedBalls = new ArrayList<>();	//list of balls that the player possesses.
	public ArrayList<Ball> localBalls = new ArrayList<>(); // All balls in the territory.
	
	public Player(boolean isClient, long ID, Vec2f pos) {
		this.ID = ID;
		this.isClient = isClient;
		centrePos = pos;
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
	
	public static Vec2f direction = new Vec2f(0,0);
	public static void processInputs(Keyboard keyboard, Mouse mouse) {
		//Reset direction.
		Vec2f.scale(direction, direction, 0);
	
		//Calculate direction from keyboard.
		if (keyboard.isActive(Key.W)) {
			direction.y += 1;
		}

		if (keyboard.isActive(Key.A)) {
			direction.x -= 1;
		}

		if (keyboard.isActive(Key.S)) {
			direction.y -= 1;
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
		
		Vec2f.scale(PlayerHandler.Me.velocity, direction, maxSpeed);
	}
	
	//TODO: Change to client prediction thing laterrrr.
	public void serverUpdate(float posX, float posY, float velX, float velY, float radOfInf) {
		centrePos.x = posX;
		centrePos.y = posY;
		velocity.x = velX;
		velocity.y = velY;
		this.radOfInf = radOfInf;
	}
	
	
}

