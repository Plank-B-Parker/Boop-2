package main;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class Keyboard implements KeyListener {

	private Map<Key, Boolean> keyActive = new EnumMap<>(Key.class);
	private Map<Key, Boolean> keyReleased = new EnumMap<>(Key.class);

	// Change map currently has no purpose, might be removed later
	private Map<Key, Boolean> keyChangedMap = new EnumMap<>(Key.class);

	private volatile boolean somethingHappened = false;

	private final Map<GameState, Boolean> gameStateFocus;

	public Keyboard(Map<GameState, Boolean> gameStateFocus) {
		// Cannot change the values of the keys in gameStatesFocus
		this.gameStateFocus = Collections.unmodifiableMap(gameStateFocus);

		// Setup default values for EnumMaps
		for (var key : Key.getEnums()) {
			keyActive.put(key, false);
			keyReleased.put(key, true);
			keyChangedMap.put(key, false);
		}
	}

	// A key has been pressed (1 event per press and repeating events if held down)
	@Override
	public void keyPressed(KeyEvent e) {
		for (var key : Key.getEnums()) {
			if (e.getKeyCode() == key.getKeyCode()) {
				handleKeyPress(key);
			}
		}
	}

	// A key has been released (only 1 event upon releasing a key)
	@Override
	public void keyReleased(KeyEvent e) {
		for (var key : Key.getEnums()) {
			if (e.getKeyCode() == key.getKeyCode()) {
				handleKeyRelease(key);
			}
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {

	}

	/**
	 * 
	 * @param key
	 */
	private void handleKeyPress(Key key) {
		// The game state for this corresponding key is not in focus
		if (!isInFocus(key.getScope()))
			return;

		// A key can only be pressed if it was released
		if (getValueFromKeyRelease(key)) {
			keyReleased.put(key, false);

			// Toggle activity of the key (Only keys that are not hold are affected)
			boolean keyActivity = getValueFromKeyMap(key);
			keyActive.put(key, !keyActivity);

			keyChangedMap.put(key, true);
			somethingHappened = true;
		}

	}

	private void handleKeyRelease(Key key) {
		// The game state for this corresponding key is NOT in focus
		if (!isInFocus(key.getScope()))
			return;

		keyReleased.put(key, true);
		// Hold keys are deactivated but toggle keys are still active
		if (key.isHold())
			keyActive.put(key, false);
		keyChangedMap.put(key, true);
		somethingHappened = true;
	}

	// Boolean.True.Equals() is used as the result from the Maps can be null
	private boolean getValueFromKeyMap(Key key) {
		return Boolean.TRUE.equals(keyActive.get(key));
	}

	private boolean getValueFromKeyRelease(Key key) {
		return Boolean.TRUE.equals(keyReleased.get(key));
	}

	private boolean getValueFromKeyChanged(Key key) {
		return Boolean.TRUE.equals(keyChangedMap.get(key));
	}

	private boolean isInFocus(GameState state) {
		return Boolean.TRUE.equals(gameStateFocus.get(state));
	}

	// Used to check if a key is active
	// If the key is not a toggle or hold key it will automatically decrement the
	// count
	public boolean isActive(Key key) {
		return getValueFromKeyMap(key);
	}

	/**
	 * Used to detect activity changes of a particular Key action. Currently has no
	 * use.
	 * 
	 * @param key the action to check with
	 */
	public boolean hasChanged(Key key) {
		if (getValueFromKeyChanged(key)) {
			keyChangedMap.put(key, false);
			return true;
		}

		return false;
	}

	public boolean hasSomethingHappened() {
		return somethingHappened;
	}

	/**
	 * Clears the interactions state of the keyboard and should be called after
	 * processing with the variable
	 */
	public void clearSomethingHappened() {
		somethingHappened = false;
	}

}
