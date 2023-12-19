package ru.pavel;

import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    static Map<String, String> mapStatic = new HashMap<>();
    static String IMAGE_PATH_TEXT = "images_nasa_text";
    static String pathStatic = "static";

    static String URL_NASA;
    static String KEY;

    public static void main(String[] args) throws IOException {
        if (args.length != 2) return;
        URL_NASA = args[0];
        KEY = args[1];
        mapStatic = staticFile();
        URL_NASA = URL_NASA.replace("DEMO_KEY", KEY);
        File file = new File(IMAGE_PATH_TEXT);
        if (!file.exists()) file.mkdir();
        File file1 = new File(pathStatic);
        for (File listFile : file1.listFiles()) {
            System.out.println(listFile.getAbsolutePath());
        }

        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 80), 0);
        server.createContext("/", new MyHandler(IMAGE_PATH_TEXT, mapStatic, URL_NASA));
        server.setExecutor(Executors.newFixedThreadPool(1));
        server.start();
    }

    static Map<String, String> staticFile() {
        File file = new File(pathStatic);
        return Stream.of(new File(file.getAbsolutePath()).listFiles()).filter(File::isFile)
                .collect(Collectors.toMap(File::getName, File::getAbsolutePath));
    }
}
