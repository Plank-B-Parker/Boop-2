package Debug;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import Mian.main;

public class Keyboard extends KeyAdapter{

	public main main;
	
	public Keyboard(main main) {
		this.main = main;
	}
	public void keyPressed(KeyEvent e) {
		int key = e.getKeyCode();
		
		//pauses simulation.
		if(key == KeyEvent.VK_P) {
			if(main.paused)
				main.paused = false;
			else
				main.paused = true;
		}
		
		//Steps in the simulation.
		if(key == KeyEvent.VK_SPACE && main.paused) {
			main.fixedUpdate(1/60f);
		}
				
	}
}
