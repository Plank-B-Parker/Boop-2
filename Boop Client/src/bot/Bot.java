package bot;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import display.Display;
import main.GameState;
import main.Keyboard;
import main.Main;

public abstract class Bot extends Main {

	public Bot(boolean doRender, boolean acceptMouseInput) {
		createDisplay(doRender, acceptMouseInput);
		setupConnections();

		Thread loopThread = new Thread() {
			public void run() {
				running = true;
				mainLoop();
			}
		};
		loopThread.setName("Main-loop");
		loopThread.start();

		try {
			connectToServer(InetAddress.getByName("127.0.0.1"));
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public abstract void runBot();

	public void createDisplay(boolean doRender, boolean acceptMouseInput) {
		Display.WINDOW_WIDTH = 720;
		Display.WINDOW_HEIGHT = 720;

		display = new Display(this, canvas);
		display.setVisible(doRender);

		keyboard = new Keyboard(gameStateFocus);
		mouse = new BotMouse(acceptMouseInput);
		
		display.addKeyListener(keyboard);
		canvas.addMouseMotionListener(mouse);
	}
	
	public void connectToServer(InetAddress serverIP) throws IOException {
		display.showCard(GameState.MAIN_GAME.toString());
		super.connectToServer(serverIP);
	}

	protected void moveMouse(float x, float y) {
		((BotMouse) mouse).moveMouse(x, y);
	}
}
