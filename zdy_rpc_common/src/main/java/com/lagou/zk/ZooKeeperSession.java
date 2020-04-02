package com.lagou.zk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

import com.alibaba.fastjson.JSON;
import com.lagou.client.ClientConnect;
import com.lagou.service.UserService;

/**
 * ZooKeeperSession
 * 
 * @author Administrator
 *
 */
public class ZooKeeperSession {

	private static CuratorFramework client;
	public static Map<String, List<ClientConnect>> clientConnectCache = new ConcurrentHashMap<String, List<ClientConnect>>();
	public ZooKeeperSession() {

		RetryPolicy exponentialBackoffRetry = new ExponentialBackoffRetry(1000, 3);
		client = CuratorFrameworkFactory.builder().connectString("127.0.0.1:2181").sessionTimeoutMs(50000)
				.connectionTimeoutMs(30000).retryPolicy(exponentialBackoffRetry).namespace("rpc") // 独立的命名空间 /base
				.build();
		client.start();
	}

	public void register(String serviceName, String addr) {
		String path = "/services/" + serviceName + "/" + addr;
		HashMap<String, Object> meta = new HashMap<String, Object>();
		meta.put("addr", addr);
		meta.put("serviceName", serviceName);
		String data = JSON.toJSONString(meta); // 检测是否存在该路径。

		try {
			Stat stat = client.checkExists().forPath(path);
			// 如果不存在这个路径，stat为null，创建新的节点路径。
			if (stat == null) {
				String s = client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path,
						data.getBytes());
			}
		} catch (Exception e) {
			if (e.getMessage().indexOf("NodeExists") < 0) {
				System.out.println("节点已存在" + serviceName);
			}
		}
	}

	public void deRegister(String serviceName, String addr) {
		String path = "/services/" + serviceName + "/" + addr;
		try {
			this.client.delete().forPath(path);
		} catch (Exception e) {
			e.getStackTrace();
		}

	}

	public List<String> getServiceAddr(String serviceName) {
		String path = "/services/" + serviceName;
		try {
			return client.getChildren().forPath(path);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	public void updateNodeData(String path,String content) {
		 try {
			client.setData().forPath(path,content.getBytes());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getNodeData(String path) {
		 try {
			byte[] bytes = client.getData().forPath(path);
			return new String(bytes);
		} catch (Exception e) {
			if (e.getMessage().indexOf("NoNode") < 0) {
				System.out.println("节点不存在" + path);
			}
		}
		return null;
	}
	
	
	public void createOrUpdateNode(String path, String content) {
		try {
			Stat stat = client.checkExists().forPath(path);
			// 如果不存在这个路径，stat为null，创建新的节点路径。
			if (stat == null) {
				client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path,
						content.getBytes());
			}else {
				client.setData().forPath(path,content.getBytes());
			}
		} catch (Exception e) {
			if (e.getMessage().indexOf("NodeExists") < 0) {
				System.out.println("节点已存在" + path);
			}
		}
	}
	
	public void loadData(String serviceName) {
		List<String> addressList = getServiceAddr(serviceName);
		if (addressList != null && !addressList.isEmpty()) {
			List<ClientConnect> clientConnectList = new ArrayList<ClientConnect>();
			for (String address : addressList) {
				ClientConnect clientConnect = new ClientConnect();
				String[] addressSplited = address.split(":");
				clientConnect.initClient(addressSplited[0], Integer.parseInt(addressSplited[1]));
				clientConnectList.add(clientConnect);
			} 
			clientConnectCache.put(serviceName, clientConnectList);
		}else {
			System.out.println("没有找到提供者");
		}
			
			
	}
	
	public void addListener(String path,TreeCacheListener treeCacheListener) {
		TreeCache treeCache = new TreeCache(client, path);
		treeCache.getListenable().addListener(treeCacheListener);
	}

	public void addListener(String serviceName) {
		String path = "/services/" + serviceName;
		TreeCache treeCache = new TreeCache(client, path);
		treeCache.getListenable().addListener(new TreeCacheListener() {
			@Override
			public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
				String nodeName = ZKPaths.getNodeFromPath(event.getData().getPath());
				String[] pathSplited = event.getData().getPath().split("/");
				switch (event.getType()) {
				case NODE_ADDED:
					System.out.println("Node add " + nodeName);
					if (pathSplited.length == 4) {
						List<ClientConnect> clientConnectList = clientConnectCache.get(serviceName);
						if(clientConnectList==null) {
							clientConnectList = new ArrayList<ClientConnect>();
						}
						
						String[] addressSplited = nodeName.split(":");
						boolean isExsit = false;
						for(ClientConnect connect:clientConnectList) {
							String ipAndPort = connect.getIp()+":"+connect.getPort();
							if(ipAndPort.equals(nodeName)) {
								isExsit = true;
							}
						}
						if(!isExsit) {
							ClientConnect clientConnect = new ClientConnect();
							clientConnect.initClient(addressSplited[0], Integer.parseInt(addressSplited[1]));
							clientConnectList.add(clientConnect);
							clientConnectCache.put(serviceName, clientConnectList);
						}
					}
					break;
				case NODE_REMOVED:
					System.out.println("Node removed " + nodeName);
					if (pathSplited.length == 4) {
						List<ClientConnect> clientConnectList = clientConnectCache.get(serviceName);
						if(clientConnectList!=null&&!clientConnectList.isEmpty()) {
							List<ClientConnect> newClientConnectList =new ArrayList<ClientConnect>();
							for(ClientConnect clientConnect:clientConnectList) {
								String ipAndPort = clientConnect.getIp()+":"+clientConnect.getPort();
								if(ipAndPort.equals(nodeName)) {
									clientConnect.close();
								}else {
									newClientConnectList.add(clientConnect);
								}
							}
							clientConnectCache.put(serviceName, newClientConnectList);
						}
					}
					break;
				case NODE_UPDATED:
					System.out.println("Node updated " + nodeName);
					break;

				}
			}
		});
		try {
			treeCache.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 封装单例的静态内部类
	 * 
	 * @author Administrator
	 *
	 */
	private static class Singleton {

		private static ZooKeeperSession instance;

		static {
			instance = new ZooKeeperSession();
		}

		public static ZooKeeperSession getInstance() {
			return instance;
		}

	}

	/**
	 * 获取单例
	 * 
	 * @return
	 */
	public static ZooKeeperSession getInstance() {
		return Singleton.getInstance();
	}

	/**
	 * 初始化单例的便捷方法
	 */
	public static void init() {
		getInstance();
	}

	public static void main(String[] args) throws InterruptedException {
		ZooKeeperSession.getInstance().deRegister(UserService.class.getName(), "127.0.0.1:8088");
		//ZooKeeperSession.getInstance().register(UserService.class.getName(), "127.0.0.1:8088");
	}
}
