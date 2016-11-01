package com.altratek.altraserver.message.response;

import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.altratek.altraserver.domain.RoomVariable;
import com.altratek.altraserver.message.*;

public class RVarsUpdateMessage extends ResponseMessage
{
	public RVarsUpdateMessage(int fromRoom, HashMap<String, RoomVariable> updatedRoomVariables, List<SocketChannel> recipients)
	{
		super(RspMsgType.rVarsUpdate,fromRoom, recipients);
		putShort(updatedRoomVariables.size());
		for(Iterator<Entry<String, RoomVariable>> it = updatedRoomVariables.entrySet().iterator(); it.hasNext();)
		{
			Entry<String, RoomVariable> entry = it.next();
			String varName = entry.getKey();
			RoomVariable rv = entry.getValue();
			putString(varName);
			putChar(rv.getType().charAt(0));
			putString(rv.getValue());
		}
	}
}