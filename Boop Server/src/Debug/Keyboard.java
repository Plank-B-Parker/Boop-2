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
	private Map<Key, Integer> keyCountMap = new EnumMap<>(Key.class);
	
	public Keyboard(main main) {
		this.main = main;
		setupKeyMaps();
	}
	
	public void setupKeyMaps() {
		for (Key key: Key.values()) {
			keymap.put(key, false);
			keyReleased.put(key, true);
			if (! key.isToggle() && ! key.isHold()) {
				keyCountMap.put(key, 0);
			}
		}
		
	}
	
	// Still trying to think of a way to make this button checking easier to read/check
	@Override
	public void keyPressed(KeyEvent e) {
		switch(e.getKeyCode()) {
			case KeyEvent.VK_SPACE:
				handleKeyPress(Key.SPACE);
				break;
				
			case KeyEvent.VK_K:
				handleKeyPress(Key.K);
				break;
				
			case KeyEvent.VK_L:
				handleKeyPress(Key.L);
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
				handleKeyRelease(Key.SPACE);
				break;
			case KeyEvent.VK_K:
				handleKeyRelease(Key.K);
				break;
			case KeyEvent.VK_L:
				handleKeyRelease(Key.L);
				break;
				
			default:
				System.out.println("Not valid release key");
		}
	}
	
	@Override
	public void keyTyped(KeyEvent e) {
		
	}

	private void handleKeyPress(Key key) {
		if (getValueFromKeyRelease(key)) {
			if(getValueFromKeyMap(key)) {
				keymap.put(key, false);
				keyReleased.put(key, false);
				if (! key.isToggle() && ! key.isHold()) keyCountMap.put(key, keyCountMap.get(key) + 1);
			}
			else if(! getValueFromKeyMap(key)) {
				keymap.put(key, true);
				keyReleased.put(key, false);
				if (! key.isToggle() && ! key.isHold()) keyCountMap.put(key, keyCountMap.get(key) + 1);
			}
		}
	}

	private void handleKeyRelease(Key key) {
		keyReleased.put(key, true);
		if (key.isHold()) keymap.put(key, false);
	}
	
	private boolean getValueFromKeyMap(Key key) {
		return Boolean.TRUE.equals(keymap.get(key));
	}
	
	private boolean getValueFromKeyRelease(Key key) {
		return Boolean.TRUE.equals(keyReleased.get(key));
	}

	private int getCount(Key key) {
		return keyCountMap.get(key);
	}
	
	private void decrementCount(Key key) {
		int currentCount = keyCountMap.get(key);
		keyCountMap.put(key, currentCount - 1);
		if (currentCount < 0) keyCountMap.put(key, 0);
	}

	public void resetCount(Key key) {
		keyCountMap.put(key, 0);
	}

	// Used to check if a key is active
	// If the key is not a toggle or hold key it will automatically decrement the count
	public boolean isActive(Key key) {
		if (key.isToggle() || key.isHold()) return getValueFromKeyMap(key);
		
		else if (getCount(key) > 0) {
			decrementCount(key);
			return true;
		}

		return false;
	}
	
}
