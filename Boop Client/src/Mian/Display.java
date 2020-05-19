package Mian;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

public class Display implements ActionListener{
	main main;
	
	
	public Display(main main) {
		this.main = main;
		createPanels();
		createWindow();
	}
	
	public void createWindow() {
		JFrame frame = new JFrame("Client");
		
		main.setSize(1920, 1080);
		
		frame.setSize(1280, 720);
		frame.setLayout(null);
		frame.getContentPane().setLayout(null);
		frame.getContentPane().setBackground(Color.BLACK);
		//frame.getContentPane().add(main);
		frame.add(cards);
		frame.setResizable(true);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		frame.setFocusable(true);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.validate();
	}
	
	CardLayout cl;
	JPanel cards;
	public void createPanels() {
		// Adds card layout to panel so that you can swap visible internal components
		cl = new CardLayout();
		cards = new JPanel(cl);
		
		JPanel empty = new JPanel();
		empty.setLayout(null);
		empty.setVisible(false);
		
		JPanel startMenu = new JPanel();
		startMenu.setLayout(null);
		startMenu.setVisible(true);
		createStartComponents(startMenu);
		
		JPanel optionsMenu = new JPanel();
		optionsMenu.setLayout(null);
		optionsMenu.setVisible(false);
		createGameMenuComponents(optionsMenu);
		
		cards.add(startMenu, "startmenu");
		cards.add(optionsMenu, "options");
		cards.add(empty, "empty");
		
		// Shows the only JPanel that has this name in the parent container
		cl.show(cards, "startmenu");
		cards.setVisible(true);
		cards.validate();
	}
	
	private void createStartComponents(Container container) {
		JButton button = createButton(200, 200, 100, 60, "Start");
		button.setFocusable(false);
		container.add(button);
		container.validate();
	}
	
	private void createGameMenuComponents(Container container) {
		JButton button = createButton(200, 200, 100, 60, "Disconnect");
		button.setFocusable(false);
		container.add(button);
		container.validate();
	}
	
	public JButton createButton(int x, int y, int width, int height, String name) {
		JButton button = new JButton(name);
		button.setBounds(x, y, width, height);
		button.addActionListener(this);
		button.setFocusable(false);
		button.setVisible(true);
		return button;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		
		
	}

}
