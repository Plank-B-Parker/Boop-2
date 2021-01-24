package display;

import java.awt.Color;
import java.io.IOException;

import javax.swing.JButton;

import main.Actions;

public class OptionsMenu extends Card{

	public OptionsMenu(Display display) {
		super(display);
	}

	protected void init(Display display) {
		setLayout(null);
		setBackground(Color.DARK_GRAY);
		
		JButton button = createButton(200, 200, 100, 60, "Disconnect", Actions.DISCONNECT.name());
		add(button);
		validate();
	}

	@Override
	public void processAction(String action) throws IOException {
		// TODO Auto-generated method stub
		
	}

}
