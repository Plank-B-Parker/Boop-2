package Debug;

import java.awt.event.KeyEvent;

public enum Key {
	SPACE("Debug Grid", KeyEvent.VK_SPACE, false, -1),
	K("Paused", KeyEvent.VK_K, false, -1),
	L("Step-Forward", KeyEvent.VK_L, false, -1);
	
	
	private String name;
	private int keyCode;
	private boolean hold;
	private long delay;
	
	private Key(String name, int keyCode, boolean hold, long delay){
		this.name = name;
		this.keyCode = keyCode;
		this.hold = hold;
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

	public long getDelay() {
		return delay;
	}
}
