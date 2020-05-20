package Debug;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.EnumMap;
import java.util.Map;

import Mian.main;

public class Keyboard implements KeyListener{
	public main main;
	
	private Map<Key, Boolean> keymap = new EnumMap<>(Key.class);
	private Map<Key, Boolean> keyReleased = new EnumMap<>(Key.class);
	
	private int timeStep = 0;
	
	public Keyboard(main main) {
		this.main = main;
		setupKeyMaps();
	}
	
	public void setupKeyMaps() {
		for (Key key: Key.values()) {
			keymap.put(key, false);
			keyReleased.put(key, true);
		}
		
	}
	
	// Still trying to think of a way to make this button checking easier to read/check
	@Override
	public void keyPressed(KeyEvent e) {
		switch(e.getKeyCode()) {
			case KeyEvent.VK_SPACE:
				// Checks if the key is active and if the user has released the key before
				if (getValueFromKeyMap(Key.SPACE) && getValueFromKeyRelease(Key.SPACE)) {
					keymap.put(Key.SPACE, false);
					keyReleased.put(Key.SPACE, false);
					break;
				}
				if (! getValueFromKeyMap(Key.SPACE) && getValueFromKeyRelease(Key.SPACE)) {
					keymap.put(Key.SPACE, true);
					keyReleased.put(Key.SPACE, false);
					break;
				}
				break;
				
			case KeyEvent.VK_K:
				if (getValueFromKeyMap(Key.K) && getValueFromKeyRelease(Key.K)) {
					keymap.put(Key.K, false);
					keyReleased.put(Key.K, false);
					break;
				}
				if (! getValueFromKeyMap(Key.K) && getValueFromKeyRelease(Key.K)) {
					keymap.put(Key.K, true);
					keyReleased.put(Key.K, false);
					break;
				}
				break;
				
			case KeyEvent.VK_L:
				if(getValueFromKeyMap(Key.L) && getValueFromKeyRelease(Key.L)) {
					keymap.put(Key.L, false);
					keyReleased.put(Key.L, false);
					timeStep += 1;
					break;
				}
				if(! getValueFromKeyMap(Key.L) && getValueFromKeyRelease(Key.L)) {
					keymap.put(Key.L, true);
					keyReleased.put(Key.L, false);
					timeStep += 1;
					break;
				}
				break;
				
			default:
				System.out.println("Not valid key");
		}
	}
	
	// Sets the keys which have been released
	@Override
	public void keyReleased(KeyEvent e) {
		switch(e.getKeyCode()) {
		case KeyEvent.VK_SPACE:
			keyReleased.put(Key.SPACE, true);
			break;
		case KeyEvent.VK_K:
			keyReleased.put(Key.K, true);
			break;
		case KeyEvent.VK_L:
			keyReleased.put(Key.L, true);
			break;
			
		default:
			System.out.println("Not valid release key");
		}
	}
	
	@Override
	public void keyTyped(KeyEvent e) {
		
	}
	
	public boolean getValueFromKeyMap(Key key) {
		return Boolean.TRUE.equals(keymap.get(key));
	}
	
	public boolean getValueFromKeyRelease(Key key) {
		return Boolean.TRUE.equals(keyReleased.get(key));
	}

	public int getTimeStep() {
		return timeStep;
	}
	
	public void decrementTimeStep() {
		timeStep--;
		if (timeStep < 0) timeStep = 0;
	}

	public void resetTimeStep() {
		timeStep = 0;
	}
	
}
