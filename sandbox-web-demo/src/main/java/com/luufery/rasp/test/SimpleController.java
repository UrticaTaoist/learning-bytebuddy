package com.luufery.rasp.test;

import com.luufery.rasp.test.stat.Utils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

@RestController
@RequestMapping("simple")
public final class SimpleController {


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
//        ZipInputStream
//        public ZipEntry getNextEntry() throws IOException {
        //()Ljava/util/zip/ZipEntry;
//        ZipFile
//        public InputStream getInputStream(ZipEntry entry)
//        (Ljava/util/zip/ZipEntry;)Ljava/io/InputStream;

//        org.apache.catalina.core.StandardService
        //stopInternal
//        void stopInternal()
        //();


        String s3 = s1 + s2;
        return s3 + ">>";
    }

    @GetMapping("util")
    public Object util(){
        return Utils.utils01();
    }


    @GetMapping("file")
    public Object file() throws IOException {
//        Files files = new Files();
        Files.newOutputStream(null,null);
        return"df";
    }
}
