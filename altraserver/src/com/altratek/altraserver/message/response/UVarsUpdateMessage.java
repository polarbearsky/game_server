package com.altratek.altraserver.message.response;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.altratek.altraserver.domain.UserVariable;
import com.altratek.altraserver.message.*;

public class UVarsUpdateMessage extends ResponseMessage
{
	public UVarsUpdateMessage(int roomId, int userId, HashMap<String, UserVariable> userVariables, List<SocketChannel> recipients)
	{
		super(RspMsgType.uVarsUpdate, roomId, recipients);
		putInt(userId);
		putShort(userVariables.size());
		for (Iterator<Entry<String, UserVariable>> it = userVariables.entrySet().iterator(); it.hasNext();)
		{
			Entry<String, UserVariable> entry = it.next();
			String varName = entry.getKey();
			UserVariable uv = entry.getValue();
			putString(varName);
			putChar(uv.getType().charAt(0));
			putString(uv.getValue());
		}
	}
}