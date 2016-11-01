package com.netty.game.clientframe;

import com.netty.game.clientframe.bean.RequestBeanManager;
import com.netty.game.clientframe.view.MainFrame;

public class ClientEntrace {

	public static void main(String[] args) {
		RequestBeanManager.instance.init();
		
		MainFrame.instance.init();
	}

}
