package com.netty.game.clientframe.view;

import javax.swing.JFrame;
import javax.swing.UIManager;

public class MainFrame extends JFrame {

	public static final MainFrame instance = new MainFrame();
	private static final long serialVersionUID = -8782176856041697861L;
	private static final int WIDTH = 800;
	private static final int HEIGHT = 600;
	private static final int LOGIN_HEIGHT = 100;
	private static final int EXTENSION_HEIGHT = 135;
	private static final int OUT_HEIGHT = HEIGHT - LOGIN_HEIGHT - EXTENSION_HEIGHT - 25;

	private MainFrame() {
		_init();
	}

	public void init(){
		
	}
	
	private void _init() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		setTitle("NettyServerDebuger V1.0");
		setSize(WIDTH, HEIGHT);
		setResizable(false);
		setLocation(150, 150);
		setLayout(null);
		add(LoginPanel.getInstance(0, 0, WIDTH, LOGIN_HEIGHT));
		add(ExtensionPanel.getInstance(0, LOGIN_HEIGHT, WIDTH, EXTENSION_HEIGHT));
		add(OutPutPanel.getInstance(0, LOGIN_HEIGHT + EXTENSION_HEIGHT, WIDTH, OUT_HEIGHT));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
	}

}
