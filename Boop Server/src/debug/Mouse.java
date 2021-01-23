package debug;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import math.Vec2f;

public class Mouse implements MouseMotionListener{
	
	private Vec2f mouse_pos = new Vec2f();
	
	public Mouse() {
		
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		mouse_pos.set((float) e.getPoint().getX(), (float) e.getPoint().getY());
	}

	public Vec2f getMousePos() {
		return mouse_pos;
	}

}
