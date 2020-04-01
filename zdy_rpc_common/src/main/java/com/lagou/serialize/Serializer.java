package com.lagou.serialize;

public interface Serializer {
	public byte[] serialize(Object object);
	public <T> T deserialize(Class<T> clazz, byte[] bytes);
}
