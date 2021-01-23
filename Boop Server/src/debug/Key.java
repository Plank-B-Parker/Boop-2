package debug;

import java.awt.event.KeyEvent;

public enum Key {
	SPACE("Debug Grid", KeyEvent.VK_SPACE, false, true, -1),
	K("Paused", KeyEvent.VK_K, false, true, -1),
	L("Step-Forward", KeyEvent.VK_L, false, false, -1),
	G("Pointer Ball", KeyEvent.VK_G, false, true, -1);
	
	
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
