package display;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import main.PlayerHandler;
import math.Vec2f;

//Global Coordinates: Server coordinates.
//Local (Dimensionless Screen) Coordinates: Screen coordinates, but as fraction instead of pixels.
//Screen Coordinates: Screen Coordinates (in pixels).

public class Renderer {
	public Graphics2D g;
	public Vec2f centre;
	
	public Renderer() {
		//TODO?
	}
	
	public void setGraphics(Graphics2D g2d) {
		g = g2d;
	}

	public void setColour(Color colour) {
		g.setColor(colour);
	}

	public void setFont(Font font) {
		g.setFont(font);
	}
	
	public void setCentre(Vec2f centre) {
		this.centre = centre;
	}

	public void drawString(String s, int pixOffsetX, int pixOffsetY) {
		drawString(s, centre, pixOffsetX, pixOffsetY);
		
	}
	public void drawString(String s, Vec2f pos) {
		drawString(s, pos, 0, 0);
	}
	
	public void drawString(String s, Vec2f pos, int pixOffsetX, int pixOffsetY) {
		var coords = globalToLocalCoords(pos, centre);
		Vec2f.scale(coords, coords, Display.WINDOW_WIDTH);
		g.drawString(s, coords.x + pixOffsetX, coords.y + pixOffsetY);
	}

	public void drawLineSegment(int pixX1, int pixY1, int pixX2, int pixY2) {
		g.drawLine(pixX1, pixY1, pixX2, pixY2);
	}
	public void drawLineSegment(Vec2f beginning, Vec2f end, boolean shortestPath) {
		//Draws two lines, from beginning to end and then end to beginning
		//Want to use this to draw a faint grid in the background.
		
		//First get a line direction pointing from beginning to end.
		var lineDirection = Vec2f.minDisp(end, beginning);
		
		if(!shortestPath) {
			if(lineDirection.x > 0)
				lineDirection.x -= 2;
			else if(lineDirection.x != 0)
				lineDirection.x += 2;
			
			if(lineDirection.y > 0)
				lineDirection.y -= 2;
			else if(lineDirection.y != 0)
				lineDirection.y += 2;
		}
		
		//Now get two pairs of coordinates to draw two lines.
		var b1 = Vec2f.minDisp(beginning, centre);
		var e1 = Vec2f.add(b1, lineDirection);
		
		var e2 = Vec2f.minDisp(end, centre);
		var b2 = Vec2f.sub(e2, lineDirection);
		
		//Scale to dimensionless screen coordinates
		scaleDispToLocal(b1);
		scaleDispToLocal(b2);
		scaleDispToLocal(e1);
		scaleDispToLocal(e2);
		
		//Scale to screen coordinates
		Vec2f.scale(b1, b1, Display.WINDOW_WIDTH);
		Vec2f.scale(b2, b2, Display.WINDOW_WIDTH);
		Vec2f.scale(e1, e1, Display.WINDOW_WIDTH);
		Vec2f.scale(e2, e2, Display.WINDOW_WIDTH);
		
		g.drawLine((int)b1.x, (int)b1.y, (int)e1.x, (int)e1.y);
		g.drawLine((int)b2.x, (int)b2.y, (int)e2.x, (int)e2.y);
	}
	
	public void drawCircle(Vec2f pos, float rad) {
		drawCircle(pos, rad, 0,0);
	}
	
	public void drawCircle(Vec2f pos, int rad) {
		drawCircle(pos, rad, 0,0);
	}
	
	public void drawCircle(int X, int Y, int R) {
		drawCircle(Vec2f.Origin, R, X, Y);
	}
	
	public void drawCircle(Vec2f pos, float rad, int pixOffsetX, int pixOffsetY) {
		var coords = globalToLocalCoords(pos, centre);
		
		var R = (int) (scaleLengthToLocal(rad)*Display.WINDOW_WIDTH);
		Vec2f.scale(coords, coords, Display.WINDOW_WIDTH);
		
		g.drawOval((int)(coords.x + pixOffsetX - R), (int)(coords.y + pixOffsetY - R), R, R);
	}
	
	public void drawCircle(Vec2f pos, int rad, int pixOffsetX, int pixOffsetY) {
		var coords = globalToLocalCoords(pos, centre);
		
		Vec2f.scale(coords, coords, Display.WINDOW_WIDTH);
		
		g.drawOval((int)(coords.x + pixOffsetX - rad), (int)(coords.y + pixOffsetY - rad), rad, rad);
	}
	
	public void fillCircle(Vec2f pos, float rad) {
		fillCircle(pos, rad, 0,0);
	}
	
	public void fillCircle(Vec2f pos, int rad) {
		fillCircle(pos, rad, 0,0);
	}
	
	public void fillCircle(int X, int Y, int R) {
		fillCircle(Vec2f.Origin, R, X, Y);
	}
	
	public void fillCircle(Vec2f pos, float rad, int pixOffsetX, int pixOffsetY) {
		var coords = globalToLocalCoords(pos, centre);
		
		var R = (int) (scaleLengthToLocal(rad)*Display.WINDOW_WIDTH);
		Vec2f.scale(coords, coords, Display.WINDOW_WIDTH);
		
		g.fillOval((int)(coords.x + pixOffsetX - R), (int)(coords.y + pixOffsetY - R), R, R);
	}
	
	public void fillCircle(Vec2f pos, int rad, int pixOffsetX, int pixOffsetY) {
		var coords = globalToLocalCoords(pos, centre);
		
		Vec2f.scale(coords, coords, Display.WINDOW_WIDTH);
		
		g.fillOval((int)(coords.x + pixOffsetX - rad), (int)(coords.y + pixOffsetY - rad), rad, rad);
	}
	
	public static Vec2f globalToLocalCoords(Vec2f coord) {
		return globalToLocalCoords(coord, Vec2f.Origin);
	}
	public static Vec2f globalToLocalCoords(Vec2f coord, Vec2f centre) {
		var disp = Vec2f.minDisp(coord, centre);
		scaleDispToLocal(disp);
		
		return disp;
	}
	
	private static Vec2f scaleDispToLocal(Vec2f disp) {
		//Scaling constants.
		var a = Math.sqrt(2)*Display.getDiameterOfVision() / 4;
		var b = Math.sqrt(2)/Display.getDiameterOfVision();
		
		//Scaling for screen.
		disp.x = (float)((disp.x + a)*b);
		disp.y = (float)(((disp.y + a)*b) - 0.5*(1 -1f/Display.aspectRatio));
		return disp;
	}
	
	private static float scaleLengthToLocal(float length) {
		var b = Math.sqrt(2)/Display.getDiameterOfVision();
		return (float) (b*length);
	}
}
