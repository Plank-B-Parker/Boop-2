package display;

import java.awt.Canvas;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import main.GameState;
import main.Keyboard;
import main.Main;
import main.PlayerHandler;

public class Display implements ActionListener{
	Main main;
	Canvas canvas;
	JFrame frame;

	public static int WINDOW_WIDTH = 1280, WINDOW_HEIGHT = 720;
	public static final Font TEXT_FONT = new Font("Calibri", Font.PLAIN, 18);
	public static final Font COMBOBOX_FONT = new Font("Calibri", Font.PLAIN, 16);
	public static final Font HEADER_FONT = new Font("Calibri", Font.PLAIN, 22);
	
	//Scaling and offset for rendering.
	private static float radOfVision = 0f; 	//Radius of vision that client sees.
	private static float radInServer = 0f;  //Radius of vision in server.
	private static float growthRate = 1f;  	//How fast radius of Vision grows.
	
	public static float aspectRatio = (float)WINDOW_WIDTH/(float)WINDOW_HEIGHT;
	
	public Display(Main main, Canvas canvas) {
		this.main = main;
		this.canvas = canvas;
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		
		createPanels();
		createWindow();
	}
	
	private long lastPlayerID = PlayerHandler.Me.ID;
	public void createWindow() {
		frame = new JFrame("Client: " + PlayerHandler.Me.ID);

		frame.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent componentEvent) {
				WINDOW_HEIGHT = frame.getHeight();
				WINDOW_WIDTH = frame.getWidth();
				aspectRatio = (float)WINDOW_WIDTH/(float)WINDOW_HEIGHT;
			}
		});
		
		canvas.setSize(1920, 1080);
		canvas.setBackground(Color.BLACK);
		
		frame.getContentPane().setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
		frame.pack();
		frame.setLayout(null);
		frame.setBackground(Color.RED);
		// Content is at the front of the panel
		frame.add(cards);
		frame.add(canvas);
		// ^^ Content is at the back of the panel
		frame.setResizable(true);
		frame.setLocationRelativeTo(null);
		frame.setFocusable(true);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent we) {
				main.stopGame();
				System.exit(0);
			}
		});
		frame.validate();
		frame.setVisible(true);
	}
	
	CardLayout cl;
	JPanel cards;
	ArrayList<Card>cardArray = new ArrayList<>();
	
	public void createPanels() {
		// Adds card layout to panel so that you can swap visible internal components
		cl = new CardLayout();
		cards = new JPanel(cl);
		cards.setBounds(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
		cards.setBackground(Color.WHITE);
		
		
		Card startMenu = new StartMenu(GameState.START_MENU.toString(), this);
		Card optionsMenu = new OptionsMenu(GameState.OPTIONS_MENU.toString(), this);
		// TODO Options menu requires development
		// Open/close on ESC key. Use the "show" method.
		// Have a disconnect and settings option eventually
		// Possibly create a new cards objects for stuff to be accessed during game-play
		
		cards.add(startMenu, startMenu.getName());
		cardArray.add(startMenu);
		
		cards.add(optionsMenu, optionsMenu.getName());
		cardArray.add(optionsMenu);
		
		// Shows the only JPanel that has this name in the parent container
		showCard(GameState.START_MENU.toString());
		cards.validate();
		cards.setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String action = e.getActionCommand();
		
		if (! main.running) {
			return;
		}
		
		try {
			
			for(Card card: cardArray) {
				card.processAction(action);
			}
			
		} catch (IOException ex) {
			ex.printStackTrace();
			showPopUp("IO problems", "Error", JOptionPane.ERROR_MESSAGE);
		}
		
	}
	
	
	//Not sure where this method is being used
	public static boolean isStringIPValid(String ipAddress){
		try {
			InetAddress.getByName(ipAddress);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	public void showCard(String cardName) {
		if(cardName.equals(GameState.MAIN_GAME.toString())) {
			cards.setVisible(false);
			main.gameStateFocus.put(GameState.START_MENU, false);
			main.gameStateFocus.put(GameState.OPTIONS_MENU, false);
			main.gameStateFocus.put(GameState.MAIN_GAME, true);
		} else {
			cards.setVisible(true);
			cl.show(cards, cardName);
			main.gameStateFocus.put(GameState.START_MENU, false);
			main.gameStateFocus.put(GameState.OPTIONS_MENU, false);
			main.gameStateFocus.put(GameState.valueOf(cardName), true);
	}
	}
	
	
	public void showPopUp(String message, String title, int messageType) {
		if (cards.isVisible()) {
			JOptionPane.showMessageDialog(cards, message, title, messageType);
		}
		else {
			JOptionPane.showMessageDialog(canvas, message, title, messageType);
		}
	}

	public void addKeyListener(Keyboard keyboard) {
		frame.addKeyListener(keyboard);
	}

	public void setVisible(boolean b) {
		frame.setVisible(b);
	}
	
	public void updatePlayerID(long playerID) {
		if (lastPlayerID != playerID) {
			lastPlayerID = playerID;
			frame.setTitle("Client: " + playerID);
		}
	}
	
	public void updateRadiusOfVision(float dt) {
		radOfVision += growthRate*(radInServer - radOfVision)*dt;
	}
	
	public static void setDiameterInServerFromRadOfInf(float radOfInf) {
		radInServer = 3*radOfInf;
	}
	
	public static float getDiameterOfVision() {
		return 2*radOfVision;
	}

}
