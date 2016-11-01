package com.netty.game.clientframe.view;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class OutPutPanel extends Surface {

	private static final long serialVersionUID = -9170905628392706399L;
	private static OutPutPanel instance;
	private JTextArea outputArea;
	private JButton clearButton;

	private OutPutPanel(int x, int y, int width, int height) {
		super("Output");
		init(x, y, width, height);
	}

	public static OutPutPanel getInstance() {
		return instance;
	}

	public static OutPutPanel getInstance(int x, int y, int width, int height) {
		if (instance == null) {
			instance = new OutPutPanel(x, y, width, height);
		}
		return instance;
	}

	private void init(int x, int y, int width, int height) {
		this.setBounds(x, y, width, height);
		setLayout(new BorderLayout());
		add(clearButton = new JButton("Clear"), BorderLayout.EAST);
		//
		outputArea = new JTextArea();
		outputArea.setLineWrap(true);//
		JScrollPane scroll;
		add(scroll = new JScrollPane(outputArea), BorderLayout.CENTER);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		clearButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				outputArea.setText("");
			}
		});
	}

	public void addMsg(String msg) {
		String oldMsg = outputArea.getText();
		if (!oldMsg.isEmpty()) {
			oldMsg += "\r\n";
		}
		StringBuffer sb = new StringBuffer(oldMsg);
		sb.append(msg);
		outputArea.setText(sb.toString());
	}
}
