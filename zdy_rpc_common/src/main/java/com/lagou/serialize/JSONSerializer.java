package com.lagou.serialize;

import com.alibaba.fastjson.JSON;

public class JSONSerializer implements Serializer{

	public byte[] serialize(Object object) {
		// TODO Auto-generated method stub
		 return JSON.toJSONBytes(object);
	}

	public <T> T deserialize(Class<T> clazz, byte[] bytes) {
		// TODO Auto-generated method stub
		 return JSON.parseObject(bytes, clazz);
	}

  

}