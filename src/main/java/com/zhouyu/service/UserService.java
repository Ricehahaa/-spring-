package com.zhouyu.service;

import com.spring.*;

@Component("userService")
public class UserService implements BeanNameAware, InitializingBean {

    @AutoWired
    private OrderService orderService;

    //bean的名字
    private String beanName;

    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void test(){
        System.out.println(this.orderService);
        System.out.println(beanName);
    }

    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println(beanName + " 初始化方法");
    }
}
