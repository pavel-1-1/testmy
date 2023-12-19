package ru.pavel;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MyHandler implements HttpHandler {
    private final String IMAGE_PATH_TEXT;
    private final Map<String, String> mapStatic;
    private final String URL_NASA;

    private NasaJson nasaJson;

    public MyHandler(String IMAGE_PATH_TEXT, Map<String, String> mapStatic, String URL_NASA) {
        this.IMAGE_PATH_TEXT = IMAGE_PATH_TEXT;
        this.mapStatic = mapStatic;
        this.URL_NASA = URL_NASA;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String urlReq = exchange.getRequestURI().getPath().substring(1);
        if (urlReq.startsWith("IMAGE")) {
            byte[] bytes = StaticOut.getImage(urlReq.substring(5), IMAGE_PATH_TEXT);
            outInClient(bytes, exchange);
        } else if (urlReq.isEmpty()) {
            if (!getImageHttp()) {
                outInClient(null, exchange);
                return;
            }
            byte[] bytes = StaticOut.getStatic("nasa.html", mapStatic);
            outInClient(StaticOut.replace(nasaJson, new String(bytes, StandardCharsets.UTF_8)), exchange);
        } else if (mapStatic.containsKey(urlReq)) {
            byte[] bytes = StaticOut.getStatic(urlReq, mapStatic);
            outInClient(bytes, exchange);
        } else {
            outInClient(null, exchange);
        }
    }

    private boolean getImageHttp() throws IOException {
        String json = getHttpJson(URL_NASA);
        if (json == null) return false;
        nasaJson = parseJson(json);
        if (GetUrl(nasaJson.getHdurl(), nasaJson)) {
        } else if (!GetUrl(nasaJson.getUrl(), nasaJson)) {
            System.out.println("no record");
            return false;
        }
        return true;
    }

    private NasaJson parseJson(String nasa) {
        Map<String, String> splitJson = new HashMap<>() {{
            put("copyright", null);
            put("date", null);
            put("explanation", null);
            put("hdurl", null);
            put("media_type", null);
            put("service_version", null);
            put("title", null);
            put("url", null);
        }};
        NasaJson nasaJson = new NasaJson();
        String[] strings = nasa.replace("{\"", "").replace("\"}", "")
                .split("\",\"");
        putMapSplit(splitJson, strings);
        nasaJson.setCopyright(splitJson.get("copyright"));
        nasaJson.setDate(splitJson.get("date"));
        nasaJson.setExplanation(splitJson.get("explanation"));
        nasaJson.setHdurl(splitJson.get("hdurl"));
        nasaJson.setMedia_type(splitJson.get("media_type"));
        nasaJson.setService_version(splitJson.get("service_version"));
        nasaJson.setTitle(splitJson.get("title"));
        nasaJson.setUrl(splitJson.get("url"));
        System.out.println(splitJson);
        return nasaJson;
    }

    private void putMapSplit(Map<String, String> map, String[] arr) {
        Arrays.stream(arr).forEach(text -> {
            String key = text.substring(0, text.indexOf("\":")).replace("\n", "");
            System.out.println(key);
            map.put(key, text.substring(text.indexOf(":\"") + 2).replace("\n", ""));
        });
    }

    private String getHttpJson(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        if (connection.getResponseCode() != 200) {
            return null;
        }
        byte[] bytes = new BufferedInputStream(connection.getInputStream()).readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private boolean GetUrl(String url, NasaJson nasaJson) {
        try {
            HttpURLConnection response = (HttpURLConnection) URI.create(url)
                    .toURL().openConnection();
            int code = response.getResponseCode();
            if (code != 200) return false;
            BufferedInputStream in = new BufferedInputStream(response.getInputStream());
            File file = new File(IMAGE_PATH_TEXT, nasaJson.getTitle().replace(":", "") + url.substring(url.lastIndexOf(".")));
            Files.copy(in, Path.of(file.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
            text(nasaJson);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private void text(NasaJson nasaJson) throws IOException {
        File file = new File(IMAGE_PATH_TEXT, nasaJson.getTitle().replace(":", "") + ".txt");
        FileOutputStream out = new FileOutputStream(file);
        out.write(nasaJson.getExplanation().getBytes(StandardCharsets.UTF_8));
        out.flush();
        out.close();
    }

    private void outInClient(byte[] bytes, HttpExchange exchange) throws IOException {
        OutputStream out = exchange.getResponseBody();
        if (bytes == null) {
            out.close();
            return;
        }
        exchange.sendResponseHeaders(200, bytes.length);
        out.write(bytes);
        out.flush();
        out.close();
    }
}
