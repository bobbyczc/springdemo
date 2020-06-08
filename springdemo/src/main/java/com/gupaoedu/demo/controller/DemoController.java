package com.gupaoedu.demo.controller;

import com.gupaoedu.mvcframework.annotation.GPController;
import com.gupaoedu.mvcframework.annotation.GPRequestMapping;
import com.gupaoedu.mvcframework.annotation.GPRequestParam;

/**
 * @author bobstorm
 * @date 2020/6/6 17:29
 */
@GPController
@GPRequestMapping("/demo")
public class DemoController {

    @GPRequestMapping("/name")
    public String test(@GPRequestParam String name){
        return "My name is " + name;
    }
}
