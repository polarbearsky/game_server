package com.netty.game.test.jprotobuf;

import com.baidu.bjf.remoting.protobuf.FieldType;
import com.baidu.bjf.remoting.protobuf.annotation.Protobuf;

public class SimpleTypeTest {
	@Protobuf(fieldType = FieldType.STRING, order = 1, required = true)
    private String name;

    @Protobuf(fieldType = FieldType.INT32, order = 2, required = false)
    private int value;

    public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
