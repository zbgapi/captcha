/*
 *Copyright © 2018 anji-plus
 *安吉加加信息技术有限公司
 *http://www.anji-plus.com
 *All rights reserved.
 */
package io.at.exchange.captcha.util;

import com.dd.tools.TFile;
import com.dd.tools.log.Logger;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


public class ImageUtils {
    private static Map<String, String> originalCacheMap = new ConcurrentHashMap<>();  //滑块底图
    private static Map<String, String> slidingBlockCacheMap = new ConcurrentHashMap<>(); //滑块
    private static Map<String, String> picClickCacheMap = new ConcurrentHashMap<>(); //点选文字


    public static void cacheImage(String captchaOriginalPathJigsaw, String captchaOriginalPathClick) {
        //滑动拼图
        if (StringUtils.isBlank(captchaOriginalPathJigsaw)) {
            originalCacheMap.putAll(getResourcesImagesFile("images/jigsaw/original", ".*.png"));
            slidingBlockCacheMap.putAll(getResourcesImagesFile("images/jigsaw/slidingBlock", ".*.png"));
        } else {
            originalCacheMap.putAll(getImagesFile(captchaOriginalPathJigsaw + File.separator + "original"));
            slidingBlockCacheMap.putAll(getImagesFile(captchaOriginalPathJigsaw + File.separator + "slidingBlock"));
        }
        //点选文字
        if (StringUtils.isBlank(captchaOriginalPathClick)) {
            picClickCacheMap.putAll(getResourcesImagesFile("images/pic-click", ".*.png"));
        } else {
            picClickCacheMap.putAll(getImagesFile(captchaOriginalPathClick));
        }
    }

    public static BufferedImage getOriginal() {
        int randomNum = RandomUtils.getRandomInt(1, originalCacheMap.size() + 1);
        String s = originalCacheMap.get("bg".concat(String.valueOf(randomNum)).concat(".png"));
        return getBase64StrToImage(s);
    }

    public static BufferedImage getSlidingBlock() {
        int randomNum = RandomUtils.getRandomInt(1, slidingBlockCacheMap.size() + 1);
        String s = slidingBlockCacheMap.get(String.valueOf(randomNum).concat(".png"));
        return getBase64StrToImage(s);
    }

    public static BufferedImage getPicClick() {
        int randomNum = RandomUtils.getRandomInt(1, picClickCacheMap.size() + 1);
        String s = picClickCacheMap.get("bg".concat(String.valueOf(randomNum)).concat(".png"));
        return getBase64StrToImage(s);
    }

    /**
     * 图片转base64 字符串
     *
     * @param templateImage
     * @return
     */
    public static String getImageToBase64Str(BufferedImage templateImage) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(templateImage, "png", baos);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] bytes = baos.toByteArray();
        BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encodeBuffer(bytes).trim();
    }

    /**
     * base64 字符串转图片
     *
     * @param base64String
     * @return
     */
    public static BufferedImage getBase64StrToImage(String base64String) {
        try {
            BASE64Decoder base64Decoder = new BASE64Decoder();
            byte[] bytes = base64Decoder.decodeBuffer(base64String);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            return ImageIO.read(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static Map<String, String> getResourcesImagesFile(String directory, String pattern) {
        Map<String, String> imgMap = new HashMap<>();

        URL url = ImageUtils.class.getClassLoader().getResource(directory);
        if (url == null) {
            return imgMap;
        }
        try {
            if ("jar".equals(url.getProtocol())) {
                JarFile jar = ((JarURLConnection) url.openConnection()).getJarFile();
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().contains(directory) && entry.getName().matches(pattern)) {
                        byte[] bytes = TFile.loadResource(entry.getName());
                        String string = Base64.getEncoder().encodeToString(bytes);
                        String filename = entry.getName().substring(entry.getName().lastIndexOf(File.separator) + 1);
                        imgMap.put(filename, string);
                    }
                }
            } else {
                File configFile = new File(url.getFile());

                List<File> resources = TFile.scanFile(configFile, pattern);
                for (File resource : resources) {
                    byte[] bytes = TFile.loadFile(resource);
                    String string = Base64.getEncoder().encodeToString(bytes);
                    String filename = resource.getName();
                    imgMap.put(filename, string);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return imgMap;
    }

    private static Map<String, String> getImagesFile(String path) {
        Map<String, String> imgMap = new HashMap<>();
        File file = new File(path);
        File[] files = file.listFiles();
        Arrays.stream(files).forEach(item -> {
            try {
                byte[] bytes = TFile.loadFile(item);
                String string = Base64.getEncoder().encodeToString(bytes);
                imgMap.put(item.getName(), string);
            } catch (Exception e) {
                Logger.error("加载滑块验证码图片异常", e);
            }
        });
        return imgMap;
    }
}
