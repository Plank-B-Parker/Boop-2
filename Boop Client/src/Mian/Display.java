package Mian;

import java.awt.Canvas;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

import Math.Vec2f;

public class Display implements ActionListener{
	main main;
	Canvas canvas;
	JComboBox<String> ipInput;
	public static final int WINDOW_WIDTH = 1280, WINDOW_HEIGHT = 720;
	public static final Font TEXT_FONT = new Font("Calibri", Font.PLAIN, 18);
	public static final Font COMBOBOX_FONT = new Font("Calibri", Font.PLAIN, 16);
	public static final Font HEADER_FONT = new Font("Calibri", Font.PLAIN, 22);
	
	//Scaling and offset for rendering.
	public static Vec2f centreInServer = new Vec2f(); 		//Where the centre of the screen is on the server.
	public static float screenHeightOnServer = 0f; 	//The height of the screen on the server.
	public static final float aspectRatio = 1280f/720f;
	
	public Display(main main, Canvas canvas) {
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
				main.disconnectServer();
				System.exit(0);
			}
		});
		frame.validate();
		//canvas.setVisible(true);
		frame.setVisible(true);
	}
	
	CardLayout cl;
	JPanel cards;
	public void createPanels() {
		// Adds card layout to panel so that you can swap visible internal components
		cl = new CardLayout();
		cards = new JPanel(cl);
		cards.setBounds(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
		cards.setBackground(Color.WHITE);
		
		JPanel startMenu = new JPanel();
		startMenu.setLayout(null);
		startMenu.setBackground(Color.DARK_GRAY);
		createStartComponents(startMenu);
		
		// Options menu requires development
		// Open/close on ESC key. Use the "show" method.
		// Have a disconnect and settings option eventually
		JPanel optionsMenu = new JPanel();
		optionsMenu.setLayout(null);
		optionsMenu.setBackground(Color.DARK_GRAY);
		createGameMenuComponents(optionsMenu);
		
		cards.add(startMenu, "startmenu");
		cards.add(optionsMenu, "options");
		
		// Shows the only JPanel that has this name in the parent container
		cl.show(cards, "startmenu");
		cards.validate();
		cards.setVisible(true);
	}
	
	private void createStartComponents(Container container) {
		JButton start = createButton(590, 300, 100, 60, "Start", Actions.START.name());
		JButton exit = createButton(590, 400, 100, 60, "Exit", Actions.CLOSE.name());
		start.setFont(TEXT_FONT);
		exit.setFont(TEXT_FONT);
		ipInput = createStringComboBox(700, 200, 100, 30, null);
		ipInput.addItem("127.0.0.1");
		ipInput.setEditable(true);
		ipInput.setFont(COMBOBOX_FONT);
		container.add(start);
		container.add(exit);
		container.add(ipInput);
		container.validate();
	}
	
	private void createGameMenuComponents(Container container) {
		JButton button = createButton(200, 200, 100, 60, "Disconnect", Actions.DISCONNECT.name());
		container.add(button);
		container.validate();
	}
	
	public JButton createButton(int x, int y, int width, int height, String name, String action) {
		JButton button = new JButton(name);
		button.setBounds(x, y, width, height);
		button.addActionListener(this);
		button.setFocusable(false);
		button.setActionCommand(action);
		button.setFont(TEXT_FONT);
		return button;
	}
	
	public JComboBox<String> createStringComboBox(int x, int y, int width, int height, String action){
		JComboBox<String> box = new JComboBox<>();
		box.setBounds(x, y, width, height);
		box.setSelectedIndex(-1);
		box.addActionListener(this);
		if (action != null) {
			box.setActionCommand(action);
			box.setName(action);
		}
		
		box.setFont(COMBOBOX_FONT);
		return box;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String action = e.getActionCommand();
		
		if (! main.running) {
			return;
		}
		
		try {
			if (action.equals(Actions.START.name())) {
				String ipV4Address = (String) ipInput.getEditor().getItem();
				InetAddress ip = isIPValid(ipV4Address);
				if (ip == null) {
					showPopUp("Invalid ip address entered", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				else if (! ip.isReachable(3000)) {
					showPopUp("IP cannot be reached", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				main.connectToServer(ip);
				showGame();
			}
			
			else if (action.equals(Actions.CLOSE.name())) {
				// close program and connections here
			}
			
		} catch (IOException ex) {
			ex.printStackTrace();
			showPopUp("IO problems", "Error", JOptionPane.ERROR_MESSAGE);
		}
		
	}
	
	public static boolean isStringIPValid(String ipAddress){
		try {
			InetAddress.getByName(ipAddress);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	public static InetAddress isIPValid(String ipAddress){
		InetAddress address = null;
		try {
			address = InetAddress.getByName(ipAddress);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		return address;
	}
	
	public void showGame() {
		cards.setVisible(false);
	}
	
	public void showStartMenu() {
		cards.setVisible(true);
		cl.show(cards, "startmenu");
	}
	
	public void showOptionsMenu() {
		cards.setVisible(true);
		cl.show(cards, "options");
	}
	
	private void showPopUp(String message, String title, int messageType) {
		if (cards.isVisible()) {
			JOptionPane.showMessageDialog(cards, message, title, messageType);
		}
		else {
			JOptionPane.showMessageDialog(canvas, message, title, messageType);
		}
	}

}
