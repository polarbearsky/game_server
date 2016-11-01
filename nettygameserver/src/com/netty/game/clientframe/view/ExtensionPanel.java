package com.netty.game.clientframe.view;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import com.baidu.bjf.remoting.protobuf.Codec;
import com.netty.game.jprotobuf.JProtobufBeanManager;
import com.netty.game.jprotobuf.MsgAnnotation.RequestResponse;

public class ExtensionPanel extends Surface {

	private static final long serialVersionUID = 7946561987221651497L;
	private static ExtensionPanel instance;
	public JTextField commandText;
	public JTextField paramsText;
	public JButton callButton;

	public static ExtensionPanel getInstance(int x, int y, int width, int height) {
		if (instance == null) {
			instance = new ExtensionPanel(x, y, width, height);
		}
		return instance;
	}

	public static ExtensionPanel getInstance() {
		return instance;
	}

	private ExtensionPanel(int x, int y, int width, int height) {
		super("Extensions");
		init(x, y, width, height);
	}

	private void init(int x, int y, int width, int height) {
		this.setBounds(x, y, width, height);
		setLayout(null);
		JLabel commandLabel;
		JLabel paramsLabel;

		add(commandLabel = new JLabel("Command:"));
		add(commandText = new JTextField());
		commandText.setText("115_1_0");

		add(paramsLabel = new JLabel("Params:"));
		add(paramsText = new JTextField());
		paramsText.setText("");

		//
		add(callButton = new JButton("Call"));

		commandLabel.setBounds(20, 20, 70, 20);
		commandText.setBounds(90, 20, 120, 20);

		callButton.setBounds(220, 20, 80, 20);

		paramsLabel.setBounds(20, 80, 150, 20);
		paramsText.setBounds(90, 80, 680, 20);
		
		callButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String cmd = commandText.getText().trim();
				if (cmd.isEmpty()) {
					JOptionPane.showMessageDialog(null, "Command is null!");
					return;
				}
				Codec<?> codec = JProtobufBeanManager.instance.getCodec(RequestResponse.REQUEST, cmd);
				if(codec == null){
					JOptionPane.showMessageDialog(null, "Command codec not exist!");
					return;
				}
				String parmStr = paramsText.getText().trim();
				
				/*String extensionId_str = extensionText.getText();
				if (extensionId_str.isEmpty()) {
					JOptionPane.showMessageDialog(null, "ExtensionIdΪ��!");
					return;
				}
				short xtId = Short.parseShort(extensionId_str);
				String command = commandText.getText();
				if (command.isEmpty()) {
					JOptionPane.showMessageDialog(null, "CommandΪ��!");
					return;
				}
				String parmStr = paramsText.getText();
				try {
					LoginHandle.instance.client.sendAsny(xtId, command, parmStr);
				} catch (Exception e1) {
					e1.printStackTrace();
				}*/
			}
		});
	}
}
