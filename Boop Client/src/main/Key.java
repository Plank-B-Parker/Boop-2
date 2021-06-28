package main;

import java.awt.event.KeyEvent;

public enum Key {

	W("Up", KeyEvent.VK_W, true, false, -1),
	A("Left", KeyEvent.VK_A, true, false, -1),
	S("Down", KeyEvent.VK_S, true, false, -1),
	D("Right", KeyEvent.VK_D, true, false, -1),

	F("Exact Coordinates", KeyEvent.VK_F, false, true, -1);
	
	
	private String name;
	private int keyCode;
	private boolean hold;
	private boolean toggle;
	private long delay;
	
	private Key(String name, int keyCode, boolean hold, boolean toggle, long delay){
		this.name = name;
		this.keyCode = keyCode;
		this.hold = hold;
		this.toggle = toggle;
		this.delay = delay;
	}

	public String getName() {
		return name;
	}

	public int getKeyCode() {
		return keyCode;
	}

	public boolean isHold() {
		return hold;
	}

	public boolean isToggle() {
		return toggle;
	}

	public long getDelay() {
		return delay;
	}
}
