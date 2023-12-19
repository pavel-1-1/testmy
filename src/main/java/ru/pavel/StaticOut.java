package ru.pavel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class StaticOut {
    static byte[] replace(NasaJson nasaJson, String html) {
        String title = nasaJson.getTitle();
        String image = nasaJson.getTitle().replace(":", "") + nasaJson.getHdurl().substring(nasaJson.getHdurl().lastIndexOf("."));
        String desc = nasaJson.getExplanation();
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

    static byte[] getImage(String nameImage, String pathContent) throws IOException {
        File file = new File(pathContent);
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
}
