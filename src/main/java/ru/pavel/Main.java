package ru.pavel;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    static Logger logger = Logger.getLogger("server");
    static Map<String, String> mapStatic = new HashMap<>();
    static final String KEY = "ZAEOu9nfCO4c35CbvCbZECY5Pl9MFOd0LpzPK5be";
    static String URL_NASA = "https://api.nasa.gov/planetary/apod?api_key=DEMO_KEY";
    static final String IMAGE_PATH_TEXT = "C:/java/test-tasks/nasa-api/images_nasa_text";

    public static void main(String[] args) throws IOException {
        new File("images_nasa_text").mkdir();
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
                    InputStream in = getUrl(URL_NASA);
                    String nasa = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    nasaGet = parseJson(nasa);
                    SaveFile.image(nasaGet, getUrl(nasaGet.hdurl));
                    SaveFile.text(nasaGet);
                    in.close();
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
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
            NasaGet nasaGet = new NasaGet();
            String[] strings = nasa.split("\",\"");
            nasaGet.setCopyright(subText(strings[0]));
            nasaGet.setDate(subText(strings[1]));
            nasaGet.setExplanation(subText(strings[2]));
            nasaGet.setHdurl(subText(strings[3]));
            nasaGet.setMedia_type(strings[4]);
            nasaGet.setService_version(strings[5]);
            nasaGet.setTitle(subText(strings[6]));
            nasaGet.setUrl(strings[7]);
            return nasaGet;
        }

        private String subText(String text) {
            return text.substring(text.indexOf(":\"") + 2);
        }

        private InputStream getUrl(String url) throws URISyntaxException, IOException {
            return new URI(url).toURL().openStream();
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
            String dirImage = "C:/java/test-tasks/nasa-api/images_nasa_text";
            File file = new File(dirImage);
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
            String dir = "C:/java/test-tasks/nasa-api/src/main/resources/static";
            return Stream.of(new File(dir).listFiles()).filter(File::isFile)
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