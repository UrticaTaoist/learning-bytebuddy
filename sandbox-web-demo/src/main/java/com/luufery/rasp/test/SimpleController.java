package com.luufery.rasp.test;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("simple")
public class SimpleController {

    @GetMapping
    public Object test() {
        String s1 = "sdf";
        String s2 = "fjd";
        return inner(s1 + "ddd", "uid" + s2);
    }

    private Object inner(String s1, String s2) {

        String s3 = s1 + s2;
        return s3 + ">>";
    }

}
