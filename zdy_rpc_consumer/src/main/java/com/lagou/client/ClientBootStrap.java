package com.lagou.client;

import com.lagou.service.UserService;
import com.lagou.zk.ZooKeeperSession;

public class ClientBootStrap {

    public static  final String providerName="UserService#sayHello#";

    public static void main(String[] args) throws InterruptedException {
    	ZooKeeperSession.getInstance().init();
    	ZooKeeperSession.getInstance().loadData(UserService.class.getName());
    	ZooKeeperSession.getInstance().addListener(UserService.class.getName());
        RpcConsumer rpcConsumer = new RpcConsumer();
        UserService proxy = (UserService) rpcConsumer.createProxy(UserService.class, providerName);

        while (true){
            Thread.sleep(2000);
            System.out.println(proxy.sayHello("are you ok?"));
        }


    }




}
