package com.netty.game.clientframe.view;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;

import com.netty.game.clientframe.ActionCommand;
import com.netty.game.clientframe.ClientLogger;
import com.netty.game.clientframe.bean.LoginData;
import com.netty.game.clientframe.handler.GameClientHandler;

public class LoginPanel extends Surface implements ActionListener {

	private static final long serialVersionUID = 8603475133506381683L;
	private static LoginPanel instance;
	public JTextField ipText;
	public JTextField portText;
	public JTextField nameText;
	public JTextField pwdText;
	public JButton loginButton;

	private GameClientHandler clientHandler;
	
	public static LoginPanel getInstance(int x, int y, int width, int height) {
		if (instance == null) {
			instance = new LoginPanel("Login", x, y, width, height);
		}
		return instance;
	}

	public static LoginPanel getInstance() {
		return instance;
	}

	private LoginPanel(String title, int x, int y, int width, int height) {
		super(title);
		init(x, y, width, height);
	}

	private void init(int x, int y, int width, int height) {
		this.setBounds(x, y, width, height);
		setLayout(null);
		JLabel ipLabel;
		JLabel upLabel;

		add(ipLabel = new JLabel("IP/Port:"));
		add(ipText = new JTextField());
		add(portText = new JTextField());
		//
		add(upLabel = new JLabel("U/P:"));
		add(nameText = new JTextField());
		add(pwdText = new JTextField());
		add(loginButton = new JButton("Login"));

		//
		ipLabel.setBounds(20, 20, 50, 20);
		ipText.setBounds(80, 20, 90, 20);
		ipText.setText("127.0.0.1");
		portText.setBounds(180, 20, 90, 20);
		portText.setText("8989");

		//
		upLabel.setBounds(20, 60, 50, 20);
		nameText.setBounds(80, 60, 90, 20);
		pwdText.setBounds(180, 60, 90, 20);
		
		loginButton.setBounds(280, 60, 90, 20);
		loginButton.addActionListener(this);
		
		loginButton.setActionCommand(ActionCommand.LOGIN);
		nameText.setText("polarbear");
		pwdText.setText("polarbear");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		switch (command) {
		case ActionCommand.LOGIN:
			clientHandler = new GameClientHandler(getLoginData());
			clientHandler.connect();
			break;
		case ActionCommand.LOG_OUT:
			if(clientHandler != null){
				clientHandler.disConnect();
			}
			break;
		default:
			ClientLogger.warn(String.format("unknown action command[%s]", command));
			break;
		}
	}

	private LoginData getLoginData() {
		String ip = ipText.getText().trim();
		int port = Integer.parseInt(portText.getText().trim());
		String userName = nameText.getText().trim();
		String pwd = pwdText.getText().trim();
		
		return new LoginData(ip, port, userName, pwd);
	}
}
