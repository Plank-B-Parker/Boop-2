package display;

import java.awt.Color;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;

import main.Actions;

public class StartMenu extends Card{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	JComboBox<String> ipInput;
	
	public StartMenu(String name, Display display) {
		super(name, display);
	}

	protected void init(Display display) {
		setLayout(null);
		setBackground(new Color(255,255,255,0));
		createComponents(display);
	}
	
	private void createComponents(Display display) {
		JButton start = createButton(590, 300, 100, 60, "Start", Actions.START.name());
		JButton exit = createButton(590, 400, 100, 60, "Exit", Actions.CLOSE.name());
		start.setFont(Display.TEXT_FONT);
		exit.setFont(Display.TEXT_FONT);
		ipInput = createStringComboBox(700, 200, 100, 30, null);
		ipInput.addItem("127.0.0.1");
		ipInput.setEditable(true);
		ipInput.setFont(Display.COMBOBOX_FONT);
		this.add(start);
		this.add(exit);
		this.add(ipInput);
		this.validate();
	}

	@Override
	public void processAction(String action) throws IOException {
		if (action.equals(Actions.START.name())) {
			String ipV4Address = (String) ipInput.getEditor().getItem();
			InetAddress ip = isIPValid(ipV4Address);
			if (ip == null) {
				display.showPopUp("Invalid ip address entered", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
//			else if (! ip.isReachable(3000)) {
//				showPopUp("IP cannot be reached", "Error", JOptionPane.ERROR_MESSAGE);
//				return;
//			}
			display.main.connectToServer(ip);
			display.showCard("Game");
		}
		
		else if (action.equals(Actions.CLOSE.name())) {
			// close program and connections here
		}
	}
	
	private static InetAddress isIPValid(String ipAddress){
		InetAddress address = null;
		try {
			address = InetAddress.getByName(ipAddress);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		return address;
	}

}
