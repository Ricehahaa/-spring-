package com.zhouyu;

import com.spring.ZhouyuApplicationContext;
import com.zhouyu.service.UserService;

public class Test {
        public static void main(String[] args) {
            ZhouyuApplicationContext zhouyuApplicationContext = new ZhouyuApplicationContext(AppConfig.class);

            UserService userService1 = (UserService) zhouyuApplicationContext.getBean("userService");
//            Object userService2 = zhouyuApplicationContext.getBean("userService");
//            Object userService3 = zhouyuApplicationContext.getBean("userService");
//            System.out.println(userService1);
//            System.out.println(userService2);
//            System.out.println(userService3);
            userService1.test();

        }
    }
