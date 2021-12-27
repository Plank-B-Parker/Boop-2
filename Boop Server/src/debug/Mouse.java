package debug;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import main.Main;
import math.Vec2f;

public class Mouse implements MouseListener, MouseMotionListener{
	
	private Vec2f mouse_pos = new Vec2f();
	public boolean isHeld = false;
	
	public Mouse() {
		
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		isHeld = true;
		float x = (float) e.getPoint().getX()/Main.windowHeight * 2 - 1; 
		float y = (float) e.getPoint().getY()/Main.windowHeight * 2 - 1; 
		mouse_pos.set(x, y);
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		float x = (float) e.getPoint().getX()/Main.windowHeight * 2 - 1; 
		float y = (float) e.getPoint().getY()/Main.windowHeight * 2 - 1; 
		mouse_pos.set(x, y);
	}

	public Vec2f getMousePos() {
		return mouse_pos;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		isHeld = false;
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

}
