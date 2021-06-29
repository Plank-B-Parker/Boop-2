package bot;

import java.awt.event.MouseEvent;

import display.Display;
import main.Mouse;
import math.Vec2f;

public class BotMouse extends Mouse {

	private boolean acceptInput;

	public BotMouse(boolean acceptInput) {
		this.acceptInput = acceptInput;
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (acceptInput) super.mouseMoved(e);
	}

	public void moveMouse(float x, float y) {
		mouseDir = new Vec2f(Display.WINDOW_WIDTH*x, -Display.WINDOW_HEIGHT*y);
		mouseMoved = true;
	}

}