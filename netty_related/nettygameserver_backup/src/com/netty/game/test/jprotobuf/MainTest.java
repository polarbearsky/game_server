package com.netty.game.test.jprotobuf;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.baidu.bjf.remoting.protobuf.Codec;
import com.baidu.bjf.remoting.protobuf.ProtobufProxy;

public class MainTest {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//testJprotobuf_Case2();
		loadBean();
	}

	private static void loadBean(){
		long startTime = System.currentTimeMillis();
		String myPackage = "com.netty.game.test.jprotobuf.bean";
		Map<String, Codec<?>> key2Codec = new HashMap<>();
		Set<Class<?>> classSet = PackageFileScan.getClasses(myPackage);
		for(Class<?> clazz : classSet){
			MsgAnnotation annotation = clazz.getAnnotation(MsgAnnotation.class);
			if(annotation == null){
				return;
			}
			if(key2Codec.containsKey(annotation.value())){
				System.err.println(String.format("duplicate codec key[%s]!", annotation.value()));
			}
			Codec<?> codec = ProtobufProxy.create(clazz);
			key2Codec.put(annotation.value(), codec);
		}
		System.err.println(key2Codec.size());
		System.out.println(String.format("use time : %dms!", System.currentTimeMillis() - startTime));
	}
	
	private static void testJprotobuf(){
		/*long start = System.currentTimeMillis();
		
		Codec<RequestMsg> simpleTypeCodec = ProtobufProxy.create(RequestMsg.class);

        RequestMsg stt = new RequestMsg();
        stt.setName("你好世界");
        stt.setValue(100);
        try {
            // 序列化
            byte[] bb = simpleTypeCodec.encode(stt);
            // 反序列化
            RequestMsg newStt = simpleTypeCodec.decode(bb);
            System.out.println(newStt.getName());
            System.out.println(newStt.getValue());
            
            System.out.println(System.currentTimeMillis() - start);
            
            由注解对象动态生成Protobuf的IDL描述文件内容
            String code = ProtobufIDLGenerator.getIDL(RequestMsg.class);
            System.out.println(code);
            
            ProtobufProxy增加生成中间编译java子节码文件功能
            //ProtobufProxy.create(SimpleTypeTest.class, false, new File("D:/backup/aoqiwork/work/netty/"));
            //ProtobufIDLProxy.create(code, false, new File("D:/backup/aoqiwork/work/netty/"));
        } catch (IOException e) {
            e.printStackTrace();
        }*/
	}
	
	
	private static void testJprotobuf_Case2(){
		long start = System.currentTimeMillis();
		
		UserInfoMsg userInfoMsg = new UserInfoMsg();
		userInfoMsg.setUserId(333);
		userInfoMsg.setUserName("polarbear");
		
		Codec<UserInfoMsg> userInfoDeCodec = ProtobufProxy.create(UserInfoMsg.class);
		
		RequestMsg requestMsg = new RequestMsg();
		requestMsg.setType(0);
		requestMsg.setAction(1);
		requestMsg.setCmd("0_1");
		try {
			requestMsg.setBytes(userInfoDeCodec.encode(userInfoMsg));
			
			Codec<RequestMsg> requestMsgDeCodec = ProtobufProxy.create(RequestMsg.class);
			
			// 序列化
            byte[] bb = requestMsgDeCodec.encode(requestMsg);
            
            // 反序列化
            RequestMsg newRequest = requestMsgDeCodec.decode(bb);
            
            System.out.println(newRequest.getType());
            System.out.println(newRequest.getAction());
            System.out.println(newRequest.getCmd());
            
            UserInfoMsg newUserInfo = userInfoDeCodec.decode(newRequest.getBytes());
            System.out.println(newUserInfo.getUserId());
            System.out.println(newUserInfo.getUserName());
            
            System.out.println("use time : " + (System.currentTimeMillis() - start));
		} catch (Throwable throwable) {
			throwable.printStackTrace();
		}
	}
}
