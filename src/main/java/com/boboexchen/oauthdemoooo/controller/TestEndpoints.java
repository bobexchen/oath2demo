package com.boboexchen.oauthdemoooo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * @ClassName TestEndpoints
 * @Description TODO
 * Author cxb
 * @Date 2019/2/24 22:58
 * @Version 1.0
 **/
@RestController
public class TestEndpoints {
    @GetMapping("/demo/{id}")
    public String getProduct(@PathVariable String id) {
        //for debug
        //Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return "demo id : " + id;
    }

    @GetMapping("/resource/{id}")
    public String getOrder(@PathVariable String id) {
        //for debug
        //Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return "resource id : " + id;
    }
}
