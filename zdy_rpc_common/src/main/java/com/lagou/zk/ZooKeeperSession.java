package com.lagou.zk;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

/**
 * ZooKeeperSession
 * 
 * @author Administrator
 *
 */
public class ZooKeeperSession {

	private static CuratorFramework client;
	private static Map<String, Set> serviceCache = new ConcurrentHashMap<String, Set>();

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

	public void loadData(String serviceName) {
		List<String> addressList = getServiceAddr(serviceName);
		if (addressList != null && !addressList.isEmpty()) {
			Set<String> set = new HashSet<String>();
			for (String address : addressList) {
				set.add(address);
			}
			serviceCache.put(serviceName, set);
		}
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
						Set<String> address = serviceCache.get(serviceName);
						if (address == null) {
							address = new HashSet<String>();
						}
						address.add(nodeName);
						serviceCache.put(serviceName, address);
					}
					break;
				case NODE_REMOVED:
					System.out.println("Node removed " + nodeName);
					if (pathSplited.length == 4) {
						Set<String> addresses = serviceCache.get(serviceName);
						if (addresses != null) {
							addresses.remove(nodeName);
							serviceCache.put(serviceName, addresses);
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
		ZooKeeperSession.getInstance().addListener("service1");
		ZooKeeperSession.getInstance().register("service1", "127.0.0.1");
	    ZooKeeperSession.getInstance().loadData("service1");
		Thread.sleep(2000);
		for (Map.Entry<String, Set> entry : serviceCache.entrySet()) {
			String mapKey = entry.getKey();
			Set mapValue = entry.getValue();
			System.out.println(mapKey + ":-----------" + mapValue);
		}

		ZooKeeperSession.getInstance().register("service1", "127.0.0.2");
		Thread.sleep(2000);
		for (Map.Entry<String, Set> entry : serviceCache.entrySet()) {
			String mapKey = entry.getKey();
			Set mapValue = entry.getValue();
			System.out.println(mapKey + ":============" + mapValue);
		}
		System.out.println(ZooKeeperSession.getInstance().getServiceAddr("service1"));
		ZooKeeperSession.getInstance().deRegister("service1", "127.0.0.2");
		Thread.sleep(2000);
		for (Map.Entry<String, Set> entry : serviceCache.entrySet()) {
			String mapKey = entry.getKey();
			Set mapValue = entry.getValue();
			System.out.println(mapKey + ":============" + mapValue);
		}
		System.out.println(ZooKeeperSession.getInstance().getServiceAddr("service1"));
	}
}
