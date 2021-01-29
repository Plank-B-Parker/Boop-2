package display;

import java.awt.Canvas;
import java.awt.CardLayout;
import java.awt.Color;
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

import main.Main;
import math.Vec2f;

public class Display implements ActionListener{
	Main main;
	Canvas canvas;

	public static int WINDOW_WIDTH = 1280, WINDOW_HEIGHT = 720;
	public static final Font TEXT_FONT = new Font("Calibri", Font.PLAIN, 18);
	public static final Font COMBOBOX_FONT = new Font("Calibri", Font.PLAIN, 16);
	public static final Font HEADER_FONT = new Font("Calibri", Font.PLAIN, 22);
	
	//Scaling and offset for rendering.
	public static Vec2f centreInServer = new Vec2f(); 		//Where the centre of the screen is on the server.
	public static float diameterInServer = 0f; 	//The height of the screen on the server.
	public static float aspectRatio = WINDOW_WIDTH/WINDOW_HEIGHT;
	
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
	
	public void createWindow() {
		JFrame frame = new JFrame("Client");

		frame.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent componentEvent) {
				WINDOW_HEIGHT = frame.getHeight();
				WINDOW_WIDTH = frame.getWidth();
				aspectRatio = WINDOW_WIDTH/WINDOW_HEIGHT;
			}
		});
		
		canvas.setSize(1920, 1080);
		canvas.setBackground(Color.BLACK);
		
		frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
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
				if (main.serverLink.getServerConnection()) {
					main.disconnectServer();
				}
				
				System.exit(0);
			}
		});
		frame.validate();
		//canvas.setVisible(true);
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
		
		
		Card startMenu = new StartMenu("startmenu", this);
		Card optionsMenu = new OptionsMenu("options", this);
		// Options menu requires development
		// Open/close on ESC key. Use the "show" method.
		// Have a disconnect and settings option eventually
		
		cards.add(startMenu, startMenu.getName());
		cardArray.add(startMenu);
		
		cards.add(optionsMenu, optionsMenu.getName());
		cardArray.add(optionsMenu);
		
		// Shows the only JPanel that has this name in the parent container
		cl.show(cards, "startmenu");
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
		if(cardName == null || cardName == "" || cardName == "Game")
			cards.setVisible(false);
		else
			cards.setVisible(true);
			cl.show(cards, cardName);
	}
	
	
	public void showPopUp(String message, String title, int messageType) {
		if (cards.isVisible()) {
			JOptionPane.showMessageDialog(cards, message, title, messageType);
		}
		else {
			JOptionPane.showMessageDialog(canvas, message, title, messageType);
		}
	}

}
