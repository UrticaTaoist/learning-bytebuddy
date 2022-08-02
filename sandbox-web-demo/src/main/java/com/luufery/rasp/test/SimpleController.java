package com.luufery.rasp.test;

import com.luufery.rasp.test.stat.Utils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;

@RestController
@RequestMapping("simple")
public class SimpleController {


    @GetMapping
    public Object test() {
        String s1 = "sdf";
        String s2 = "fjd";

        for (Method declaredMethod : SimpleController.class.getDeclaredMethods()) {
            System.out.println(declaredMethod.getName());
        }
        return inner(s1 + "ddd", "uid" + s2);
//        throw new RuntimeException("啊?");
    }

    private Object inner(String s1, String s2) {

        String s3 = s1 + s2;
        return s3 + ">>";
    }

    @GetMapping("util")
    public Object util(){
        return Utils.utils01();
    }

}
