package com.lagou.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.utils.ZKPaths;

import com.alibaba.fastjson.JSON;
import com.lagou.zk.ZooKeeperSession;


public class JDBCHelper {
	private static JDBCHelper instance = null;
	
	private static String DATASOURCE_NODE = "/datasource";
	/**
	 * 获取单例
	 * @return 单例
	 */
	public static JDBCHelper getInstance() {
		if(instance == null) {
			synchronized(JDBCHelper.class) {
				if(instance == null) {
					instance = new JDBCHelper();
				}
			}
		}
		return instance;
	}
	
	// 数据库连接池
	private LinkedList<Connection> datasource = new LinkedList<Connection>();
	
	private void initDataSource() {
		
		/*Map<String,String> map = new HashMap<String,String>();
		map.put("driver", "com.mysql.jdbc.Driver");
		map.put("url", "jdbc:mysql://10.1.20.4:3306/saic_pay_statistics?useUnicode=true&characterEncoding=UTF-8&zeroDateTimeBehavior=convertToNull&allowMultiQueries=true&useSSL=false");
		map.put("user", "pay_statistics");
		map.put("password", "9d630e780c3d57731fdecb423f43a4cc");
		ZooKeeperSession.getInstance().createOrUpdateNode(DATASOURCE_NODE, JSON.toJSONString(map));*/
		
		String dataSource = ZooKeeperSession.getInstance().getNodeData(DATASOURCE_NODE);
		if(dataSource==null) {
			throw new RuntimeException("获取数据库配置信息失败");
		}
		
		Map dataSourceMap = JSON.parseObject(dataSource, Map.class);
		String driver = String.valueOf(dataSourceMap.get("driver"));
		String url = String.valueOf(dataSourceMap.get("url"));
		String user = String.valueOf(dataSourceMap.get("user"));
		String password = String.valueOf(dataSourceMap.get("password"));
		try {
			Class.forName(driver);
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
		}
		
		int datasourceSize = 10;
		// 然后创建指定数量的数据库连接，并放入数据库连接池中
		for(int i = 0; i < datasourceSize; i++) {
			try {
				Connection conn = DriverManager.getConnection(url, user, password);
				datasource.push(conn);  
			} catch (Exception e) {
				e.printStackTrace(); 
			}
		}
	}
	
	private JDBCHelper() {
		initDataSource();
		 ZooKeeperSession.getInstance().addListener(DATASOURCE_NODE, new TreeCacheListener() {
				@Override
				public void childEvent(CuratorFramework client, TreeCacheEvent event) throws Exception {
					String nodeName = ZKPaths.getNodeFromPath(event.getData().getPath());
					switch (event.getType()) {
					case NODE_ADDED:
						break;
					case NODE_REMOVED:
						System.out.println("datasource Node removed " + nodeName);
						break;
					case NODE_UPDATED:
						System.out.println("datasource Node updated " + nodeName);
						datasource.clear();
						initDataSource();
						break;
					}
				}
			});
	}
	
	/**
	 * 第四步，提供获取数据库连接的方法
	 * 有可能，你去获取的时候，这个时候，连接都被用光了，你暂时获取不到数据库连接
	 * 所以我们要自己编码实现一个简单的等待机制，去等待获取到数据库连接
	 * 
	 */
	public synchronized Connection getConnection() {
		while(datasource.size() == 0) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}  
		}
		return datasource.poll();
	}
	
	/**
	 * 第五步：开发增删改查的方法
	 * 1、执行增删改SQL语句的方法
	 * 2、执行查询SQL语句的方法
	 * 3、批量执行SQL语句的方法
	 */
	
	/**
	 * 执行增删改SQL语句
	 * @param sql 
	 * @param params
	 * @return 影响的行数
	 */
	public int executeUpdate(String sql, Object[] params) {
		int rtn = 0;
		Connection conn = null;
		PreparedStatement pstmt = null;
		
		try {
			conn = getConnection();
			conn.setAutoCommit(false);  
			
			pstmt = conn.prepareStatement(sql);
			
			if(params != null && params.length > 0) {
				for(int i = 0; i < params.length; i++) {
					pstmt.setObject(i + 1, params[i]);  
				}
			}
			
			rtn = pstmt.executeUpdate();
			
			conn.commit();
		} catch (Exception e) {
			e.printStackTrace();  
		} finally {
			if(conn != null) {
				datasource.push(conn);  
			}
		}
		
		return rtn;
	}
	
	/**
	 * 执行查询SQL语句
	 * @param sql
	 * @param params
	 * @param callback
	 */
	public void executeQuery(String sql, Object[] params, 
			QueryCallback callback) {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		try {
			conn = getConnection();
			pstmt = conn.prepareStatement(sql);
			
			if(params != null && params.length > 0) {
				for(int i = 0; i < params.length; i++) {
					pstmt.setObject(i + 1, params[i]);   
				}
			}
			
			rs = pstmt.executeQuery();
			
			callback.process(rs);  
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(conn != null) {
				datasource.push(conn);  
			}
		}
	}
	
	/**
	 * 批量执行SQL语句
	 * 
	 * 批量执行SQL语句，是JDBC中的一个高级功能
	 * 默认情况下，每次执行一条SQL语句，就会通过网络连接，向MySQL发送一次请求
	 * 
	 * 但是，如果在短时间内要执行多条结构完全一模一样的SQL，只是参数不同
	 * 虽然使用PreparedStatement这种方式，可以只编译一次SQL，提高性能，但是，还是对于每次SQL
	 * 都要向MySQL发送一次网络请求
	 * 
	 * 可以通过批量执行SQL语句的功能优化这个性能
	 * 一次性通过PreparedStatement发送多条SQL语句，比如100条、1000条，甚至上万条
	 * 执行的时候，也仅仅编译一次就可以
	 * 这种批量执行SQL语句的方式，可以大大提升性能
	 * 
	 * @param sql
	 * @param paramsList
	 * @return 每条SQL语句影响的行数
	 */
	public int[] executeBatch(String sql, List<Object[]> paramsList) {
		int[] rtn = null;
		Connection conn = null;
		PreparedStatement pstmt = null;
		
		try {
			conn = getConnection();
			
			// 第一步：使用Connection对象，取消自动提交
			conn.setAutoCommit(false);  
			
			pstmt = conn.prepareStatement(sql);
			
			// 第二步：使用PreparedStatement.addBatch()方法加入批量的SQL参数
			if(paramsList != null && paramsList.size() > 0) {
				for(Object[] params : paramsList) {
					for(int i = 0; i < params.length; i++) {
						pstmt.setObject(i + 1, params[i]);  
					}
					pstmt.addBatch();
				}
			}
			
			// 第三步：使用PreparedStatement.executeBatch()方法，执行批量的SQL语句
			rtn = pstmt.executeBatch();
			
			// 最后一步：使用Connection对象，提交批量的SQL语句
			conn.commit();
		} catch (Exception e) {
			e.printStackTrace();  
		} finally {
			if(conn != null) {
				datasource.push(conn);  
			}
		}
		
		return rtn;
	}
	
	/**
	 * 静态内部类：查询回调接口
	 * @author Administrator
	 *
	 */
	public static interface QueryCallback {
		
		/**
		 * 处理查询结果
		 * @param rs 
		 * @throws Exception
		 */
		void process(ResultSet rs) throws Exception;
		
	}
	
}
