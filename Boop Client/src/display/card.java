package display;

import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

public abstract class Card extends JPanel{

	Display display;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public Card(Display display) {
		init(display);
		this.display = display;
	}
	
	
	/**
	 * Initialises all the components in the card.
	 * @param display
	 */
	protected abstract void init(Display display);
	
	/**
	 * 
	 * @param action
	 * @throws IOException 
	 */
	public abstract void processAction(String action) throws IOException;
	
	protected JButton createButton(int x, int y, int width, int height, String name, String action, Display display) {
		JButton button = new JButton(name);
		button.setBounds(x, y, width, height);
		button.addActionListener(display);
		button.setFocusable(false);
		button.setActionCommand(action);
		button.setFont(Display.TEXT_FONT);
		return button;
	}
	
	protected JComboBox<String> createStringComboBox(int x, int y, int width, int height, String action, Display display){
		JComboBox<String> box = new JComboBox<>();
		box.setBounds(x, y, width, height);
		box.setSelectedIndex(-1);
		box.addActionListener(display);
		if (action != null) {
			box.setActionCommand(action);
			box.setName(action);
		}
		
		box.setFont(Display.COMBOBOX_FONT);
		return box;
	}
	
	
}