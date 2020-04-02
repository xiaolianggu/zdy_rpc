package com.lagou.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.lagou.serialize.RpcRequest;
import com.lagou.zk.ZooKeeperSession;

public class RpcConsumer {

	private static ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

	private static Long outTime = 5 * 60 * 1000L;

	public Object createProxy(final Class<?> serviceClass, final String providerName) {
		return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[] { serviceClass },
				new InvocationHandler() {
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						RpcRequest request = new RpcRequest();
						request.setRequestId(UUID.randomUUID().toString());
						request.setClassName(serviceClass.getCanonicalName());
						request.setParameters(args);
						request.setParameterTypes(method.getParameterTypes());
						request.setMethodName(method.getName());

						List<ClientConnect> clientConnectList = ZooKeeperSession.clientConnectCache
								.get(serviceClass.getName());
						if (clientConnectList == null || clientConnectList.isEmpty()) {
							System.out.println("调用失败，没有服务提供");
							return null;
						}
						ClientConnect clientConnect = clientConnectList.get(0);
						Long lastInvokeTime = getLastInvokeTime( clientConnect) ;
						for (int i = 1; i < clientConnectList.size(); i++) {
							Long invokeTime = getLastInvokeTime(clientConnectList.get(i)) ;
							if(invokeTime!=0) {
								if(lastInvokeTime == 0) {
									clientConnect =  clientConnectList.get(i);
								}else if(invokeTime < lastInvokeTime){
									clientConnect =  clientConnectList.get(i);
								}
							}
						}
						clientConnect.getUserClientHandler().setPara(request);
						long beginTime = new Date().getTime();
						Object obj = executor.submit(clientConnect.getUserClientHandler()).get();
						long endTime = new Date().getTime();
						String methodInvokeTime = beginTime + "," + endTime;
						String path = "/services/" + clientConnect.getIp() + ":" + clientConnect.getPort()
						+ "/methodInvokeTime";
						ZooKeeperSession.getInstance().createOrUpdateNode(path, methodInvokeTime);
						return obj;
					}
				});

	}
	
	private Long getLastInvokeTime(ClientConnect clientConnect) {
		String path = "/services/" + clientConnect.getIp() + ":" + clientConnect.getPort()
		+ "/methodInvokeTime";
		String lastMethodInvokeTime = ZooKeeperSession.getInstance().getNodeData(path);
		if (lastMethodInvokeTime != null) {
			String[] lastMethodInvokeTimeSplited = lastMethodInvokeTime.split(",");
			long lastBeginTime = Long.parseLong(lastMethodInvokeTimeSplited[0]);
			long lastEndTime = Long.parseLong(lastMethodInvokeTimeSplited[1]);
			long now = new Date().getTime();
			if (now - lastEndTime < outTime) {
				return lastEndTime - lastBeginTime;
			}
		}
		return 0L;
	}

}
