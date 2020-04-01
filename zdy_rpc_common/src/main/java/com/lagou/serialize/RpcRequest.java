package com.lagou.serialize;

public class RpcRequest {
	/**
	 * 请求对象的ID
	 * 
	 */

	private String requestId;

	/**
	 * 
	 * 类名
	 * 
	 */

	private String className;

	/**
	 * 
	 * 方法�?
	 * 
	 */

	private String methodName;

	/**
	 * 
	 * 参数类型
	 * 
	 */

	private Class<?>[] parameterTypes;

	/**
	 * 
	 * 入参
	 * 
	 */

	private Object[] parameters;

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public Class<?>[] getParameterTypes() {
		return parameterTypes;
	}

	public void setParameterTypes(Class<?>[] parameterTypes) {
		this.parameterTypes = parameterTypes;
	}

	public Object[] getParameters() {
		return parameters;
	}

	public void setParameters(Object[] parameters) {
		this.parameters = parameters;
	}
	
	

}