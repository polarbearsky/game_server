package com.netty.game.clientframe.view;

import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

@SuppressWarnings("serial")
public abstract class Surface extends JPanel {

	public Surface(String title) {
		setBorder(new TitledBorder(title));
	}
}
