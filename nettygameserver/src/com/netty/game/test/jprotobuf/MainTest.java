package com.netty.game.test.jprotobuf;

import java.io.IOException;

import com.baidu.bjf.remoting.protobuf.Codec;
import com.baidu.bjf.remoting.protobuf.ProtobufIDLGenerator;
import com.baidu.bjf.remoting.protobuf.ProtobufProxy;

public class MainTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		long start = System.currentTimeMillis();
		
		Codec<SimpleTypeTest> simpleTypeCodec = ProtobufProxy.create(SimpleTypeTest.class);

        SimpleTypeTest stt = new SimpleTypeTest();
        stt.setName("你好世界");
        stt.setValue(100);
        try {
            // 序列化
            byte[] bb = simpleTypeCodec.encode(stt);
            // 反序列化
            SimpleTypeTest newStt = simpleTypeCodec.decode(bb);
            System.out.println(newStt.getName());
            System.out.println(newStt.getValue());
            
            System.out.println(System.currentTimeMillis() - start);
            
            /*由注解对象动态生成Protobuf的IDL描述文件内容*/
            String code = ProtobufIDLGenerator.getIDL(SimpleTypeTest.class);
            System.out.println(code);
            
            /*ProtobufProxy增加生成中间编译java子节码文件功能*/
            //ProtobufProxy.create(SimpleTypeTest.class, false, new File("D:/backup/aoqiwork/work/netty/"));
            //ProtobufIDLProxy.create(code, false, new File("D:/backup/aoqiwork/work/netty/"));
        } catch (IOException e) {
            e.printStackTrace();
        }

	}

}
