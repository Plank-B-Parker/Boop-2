package main;

import java.awt.event.KeyEvent;

public enum Key {
	
	W(KeyEvent.VK_W, 0);
	
	int keyCode, methodIndex;
	
	private Key(int keyCode, int methodIndex) throws ExceptionInInitializerError{
		this.keyCode = keyCode;
		this.methodIndex = methodIndex;
	}
}
