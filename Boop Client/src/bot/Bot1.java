package bot;

public class Bot1 extends Bot {

	public Bot1(boolean doRender, boolean acceptMouseInput) {
		super(doRender, acceptMouseInput);
	}

	public void runBot() {

		boolean goingLeft = true;
		moveMouse(-1, 1);

	    long timer = System.currentTimeMillis();
	    while(running) {
			if (doRender && System.currentTimeMillis() - timer >= 1000) {

				goingLeft = !goingLeft;
				if (goingLeft) moveMouse(-1, 1);
				else moveMouse(1, 1);

				timer += 1000;
			}
	    }
	}
}
