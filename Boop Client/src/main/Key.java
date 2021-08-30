package main;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains information about the functionality of a physical key and which GameState it's applicable in
 */
public enum Key {
	
	MOVE_UP(KeyEvent.VK_W, true, -1, GameState.MAIN_GAME),
	MOVE_LEFT(KeyEvent.VK_A, true, -1, GameState.MAIN_GAME),
	MOVE_DOWN(KeyEvent.VK_S, true, -1, GameState.MAIN_GAME),
	MOVE_RIGHT(KeyEvent.VK_D, true, -1, GameState.MAIN_GAME),

	RENDER_BALLS_EXACT(KeyEvent.VK_F, false, -1, GameState.MAIN_GAME);
	
	// Cache array to save time and in this case elements are immutable.
	private static final Key[] values = values();
	
	// Enforce immutability to stop setter methods from changing values.
	
	private final int keyCode; 		// which button on the keyboard that this action represents
	private final boolean hold; 	// whether the action is a hold button or a toggle
	private final long msDelay; 	// how long it takes before the key is considered active (only for Hold)
	private final GameState scope; 	// The scope of which a Key is active for
	
	private Key(int keyCode, boolean hold, long msDelay, GameState scope){
		this.keyCode = keyCode;
		this.hold = hold;
		this.msDelay = msDelay;
		this.scope = scope;
	}

	public int getKeyCode() {
		return keyCode;
	}

	public boolean isHold() {
		return hold;
	}

	public long getMsDelay() {
		return msDelay;
	}
	
	public GameState getScope() {
		return scope;
	}
	
	public static Key[] getEnums() {
		return values;
	}
	
	public static List<Key> getAllKeysByKeyCode(int keyCode) {
		List<Key> keys = new ArrayList<>();
		for (var key : values) {
			if (key.getKeyCode() == keyCode) keys.add(key);
		}
		
		return keys;
	}
}
