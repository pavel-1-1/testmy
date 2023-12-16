package ru.pavel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    static String URL_A = "https://api.nasa.gov/planetary/apod?api_key=DEMO_KEY";
    static String KEY = "ZAEOu9nfCO4c35CbvCbZECY5Pl9MFOd0LpzPK5be";
    static String pathSave = "C:/corporatex/testmy/images_nasa_text";
    static String pathStatic = "C:/corporatex/testmy/src/main/resources/static";

    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
        mapStatic = StaticOut.staticFile();
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 80), 0);
        server.createContext("/", new GetHandler());
        server.setExecutor(Executors.newFixedThreadPool(1));
        server.start();
        logger.info("Server started");
        logger.info(mapStatic.toString());
    }

    static class GetHandler implements HttpHandler {
        HttpClient httpClient;

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            logger.info(exchange.getRequestURI().getPath().substring(1) + "--------------------------------");
            String urlReq = exchange.getRequestURI().getPath().substring(1);

            if (urlReq.startsWith("IMAGE")) {
                byte[] bytes = StaticOut.getImage(urlReq.substring(5));
                outInClient(bytes, exchange);
                return;
            }
            if (urlReq.equals("")) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(URL_A.replaceAll("DEMO_KEY", KEY)))
                        .GET()
                        .build();
                HttpResponse<String> response;
                NasaGet nasaGet = null;
                try {
                    response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    nasaGet = new ObjectMapper().readValue(response.body(), NasaGet.class);
                } catch (InterruptedException e) {
                    logger.info(e.toString());
                }
                SaveFile.save(nasaGet);
                byte[] bytes = StaticOut.getStatic("nasa.html", mapStatic);
                outInClient(bytes, exchange);
            } else if (mapStatic.containsKey(urlReq)) {
                byte[] bytes = StaticOut.getStatic(urlReq, mapStatic);
                outInClient(bytes, exchange);
            } else {
                outInClient(null, exchange);
            }
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
        static void save(NasaGet nasaGet) {
            String pathFile = nasaGet.title + nasaGet.hdurl.substring(nasaGet.hdurl.lastIndexOf("."));
            System.out.println(pathFile);
//            File file = new File(pathSave, pathFile);
//            try (InputStream in = new URL(nasaGet.hdurl).openStream()) {
//                if (file.createNewFile()) {
//                    Files.copy(in, Paths.get(file.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
//                }
//            } catch (IOException ignored) {
//            }
        }

        static String create(NasaGet nasaGet) {
            String pathFile = nasaGet.title + nasaGet.hdurl.substring(nasaGet.hdurl.lastIndexOf("."));
            File file = new File(pathFile);
            try (InputStream in = new URL(nasaGet.hdurl).openStream()) {
                if (file.createNewFile()) {
                    Files.copy(in, Paths.get(file.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException ignored) {
            }
            return file.getAbsolutePath();
        }

        //       static byte[] replace(NasaGet nasaGet) {
//            String title = nasaGet.title;
//            String image = create(nasaGet);
//            path = image;
//            String desc = nasaGet.explanation;
//            return html.replace("TITLE", title).replace("DESCRIPT", desc).replace("JPG", image)
//                    .getBytes();
        //      }
    }

    static class StaticOut {
        static byte[] getStatic(String path, Map<String, String> map) {
            try (InputStream in = new FileInputStream(map.get(path))) {
                return in.readAllBytes();
            } catch (IOException e) {
            }
            return null;
        }

        static byte[] getImage(String nameImage) throws IOException {
            File file = new File(pathSave);
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
            return Stream.of(new File(pathStatic).listFiles()).filter(File::isFile)
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