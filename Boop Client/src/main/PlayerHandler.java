package main;

import java.util.ArrayList;

import math.Vec2f;

public class PlayerHandler extends ArrayList<Player> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8656568538126391881L;
	
	public volatile static Player Me = new Player(true, -1, new Vec2f(0,0));
	
	public PlayerHandler() {
		add(Me);
	}

	public Player getPlayerByID(long ID) {
		for(Player P: this) {
			if(P.ID == ID) return P;
		}
		return null;
	}
	
	public void updatePlayers(float dt) {
		for(Player player: this) {
			player.updatePos();
		}
	}
}
