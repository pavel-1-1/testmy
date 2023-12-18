package ru.pavel;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    static Logger logger = Logger.getLogger("server");
    static Map<String, String> mapStatic = new HashMap<>();
    static String KEY;
    static String URL_NASA;
    static String pathStatic = "static";
    static final String IMAGE_PATH_TEXT = "images_nasa_text";

    static final HttpClient client = HttpClient.newHttpClient();

    public static void main(String[] args) throws IOException {
        if (args.length != 2) return;
        URL_NASA = args[0];
        KEY = args[1];
        System.out.println(URL_NASA + "--------" + KEY);
        File file = new File(IMAGE_PATH_TEXT);
        if (!file.exists()) file.mkdir();
        File file1 = new File(pathStatic);
        for (File listFile : file1.listFiles()) {
            System.out.println(listFile.getAbsolutePath());
        }
        mapStatic = StaticOut.staticFile();
        URL_NASA = URL_NASA.replace("DEMO_KEY", KEY);
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 80), 0);
        server.createContext("/", new GetHandler());
        server.setExecutor(Executors.newFixedThreadPool(1));
        server.start();
        logger.info("Server started");
        logger.info(mapStatic.toString());
    }

    static class GetHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            logger.info(exchange.getRequestURI().getPath().substring(1) + "--------------------------------");
            String urlReq = exchange.getRequestURI().getPath().substring(1);

            if (urlReq.startsWith("IMAGE")) {
                byte[] bytes = StaticOut.getImage(urlReq.substring(5));
                outInClient(bytes, exchange);
                return;
            }
            if (urlReq.isEmpty()) {
                NasaGet nasaGet;
                try {
                    InputStream in = getHttp(URL_NASA).getInputStream();
                    String nasa = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    nasaGet = parseJson(nasa);

                    HttpURLConnection connection = getHttp(nasaGet.getHdurl());
                    if (connection.getResponseCode() == 200) {
                        SaveFile.image(nasaGet, connection.getInputStream());
                    } else {
                        connection = getHttp(nasaGet.getUrl());
                        if (connection.getResponseCode() != 200) return;
                        SaveFile.image(nasaGet, connection.getInputStream());
                    }
                    SaveFile.text(nasaGet);
                    in.close();
                } catch (URISyntaxException e) {
                    throw new RuntimeException("error request");
                }
                byte[] bytes = StaticOut.getStatic("nasa.html", mapStatic);
                outInClient(StaticOut.replace(nasaGet, new String(bytes, StandardCharsets.UTF_8)), exchange);
            } else if (mapStatic.containsKey(urlReq)) {
                byte[] bytes = StaticOut.getStatic(urlReq, mapStatic);
                outInClient(bytes, exchange);
            } else {
                outInClient(null, exchange);
            }
        }

        private NasaGet parseJson(String nasa) {
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
            NasaGet nasaGet = new NasaGet();
            String[] strings = nasa.replace("{\"", "").replace("\"}", "")
                    .split("\",\"");
            putMapSplit(splitJson, strings);
            nasaGet.setCopyright(splitJson.get("copyright"));
            nasaGet.setDate(splitJson.get("date"));
            nasaGet.setExplanation(splitJson.get("explanation"));
            nasaGet.setHdurl(splitJson.get("hdurl"));
            nasaGet.setMedia_type(splitJson.get("media_type"));
            nasaGet.setService_version(splitJson.get("service_version"));
            nasaGet.setTitle(splitJson.get("title"));
            nasaGet.setUrl(splitJson.get("url"));
            System.out.println(splitJson);
            return nasaGet;
        }

        private void putMapSplit(Map<String, String> map, String[] arr) {
            Arrays.stream(arr).forEach(text -> {
                String key = text.substring(0, text.indexOf("\":"));
                System.out.println(key);
                map.put(key, text.substring(text.indexOf(":\"") + 2).replace("\n", ""));
            });
        }

        private HttpURLConnection getHttp(String url) throws URISyntaxException, IOException {
            return (HttpURLConnection) new URI(url).toURL().openConnection();
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

    static class SaveFile {
        static void image(NasaGet nasaGet, InputStream in) throws IOException {
            File file = new File(IMAGE_PATH_TEXT, nasaGet.title + nasaGet.hdurl.substring(nasaGet.hdurl.lastIndexOf(".")));
            Files.copy(in, Path.of(file.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
        }

        static void text(NasaGet nasaGet) throws IOException {
            File file = new File(IMAGE_PATH_TEXT, nasaGet.title + ".txt");
            FileOutputStream out = new FileOutputStream(file);
            out.write(nasaGet.explanation.getBytes(StandardCharsets.UTF_8));
            out.flush();
            out.close();
        }
    }

    static class StaticOut {
        static byte[] replace(NasaGet nasaGet, String html) {
            String title = nasaGet.title;
            String image = nasaGet.title + nasaGet.hdurl.substring(nasaGet.hdurl.lastIndexOf("."));
            String desc = nasaGet.explanation;
            return html.replace("TITLE", title).replace("DESCRIPT", desc).replace("JPG", "IMAGE" + image)
                    .getBytes();
        }

        static byte[] getStatic(String path, Map<String, String> map) {
            try (InputStream in = new FileInputStream(map.get(path))) {
                return in.readAllBytes();
            } catch (IOException e) {
            }
            return null;
        }

        static byte[] getImage(String nameImage) throws IOException {
            File file = new File(IMAGE_PATH_TEXT);
            for (File listFile : file.listFiles()) {
                if (listFile.getName().equals(nameImage)) {
                    try (FileInputStream in = new FileInputStream(listFile.getAbsolutePath())) {
                        return in.readAllBytes();
                    } finally {
                    }

                }
            }
            return null;
        }

        static Map<String, String> staticFile() {
            File file = new File(pathStatic);
            return Stream.of(new File(file.getAbsolutePath()).listFiles()).filter(File::isFile)
                    .collect(Collectors.toMap(File::getName, File::getAbsolutePath));
        }
    }

    static class NasaGet {
        private String copyright;
        private String date;
        private String explanation;
        private String hdurl;
        private String media_type;
        private String service_version;
        private String title;
        private String url;

        public String getCopyright() {
            return copyright;
        }

        public void setCopyright(String copyright) {
            this.copyright = copyright;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getExplanation() {
            return explanation;
        }

        public void setExplanation(String explanation) {
            this.explanation = explanation;
        }

        public String getHdurl() {
            return hdurl;
        }

        public void setHdurl(String hdurl) {
            this.hdurl = hdurl;
        }

        public String getMedia_type() {
            return media_type;
        }

        public void setMedia_type(String media_type) {
            this.media_type = media_type;
        }

        public String getService_version() {
            return service_version;
        }

        public void setService_version(String service_version) {
            this.service_version = service_version;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}