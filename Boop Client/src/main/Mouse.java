package main;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import display.Display;
import math.Vec2f;

public class Mouse implements MouseListener, MouseMotionListener{

	//Stack<Vec2f> directionsMoved = new Stack<>();
	Vec2f mouseDir = new Vec2f();
	boolean mouseMoved = false;
	
	public Mouse() {
		
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		switch(e.getButton()) {
		case MouseEvent.BUTTON1:
			System.out.println("Left Click");
			break;
		default:
			break;
		}
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		mouseMoved = true;
		float r_max = (float) (0.6*Math.min(Display.WINDOW_WIDTH, Display.WINDOW_HEIGHT));
		
		mouseDir.x = e.getX() - Display.WINDOW_WIDTH/2;
		mouseDir.y = e.getY() - Display.WINDOW_HEIGHT/2;
		
		Vec2f.scale(mouseDir, mouseDir, 1.0f/r_max);
		
	}

}