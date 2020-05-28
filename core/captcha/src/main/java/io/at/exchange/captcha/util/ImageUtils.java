/*
 *Copyright © 2018 anji-plus
 *安吉加加信息技术有限公司
 *http://www.anji-plus.com
 *All rights reserved.
 */
package io.at.exchange.captcha.util;

import com.dd.tools.TFile;
import com.dd.tools.log.Logger;

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


/**
 * @author zhangzhipeng
 */
public class ImageUtils {
    private static Map<String, String> originalCacheMap = new ConcurrentHashMap<>();  //滑块底图
    private static Map<String, String> slidingBlockCacheMap = new ConcurrentHashMap<>(); //滑块
    private static Map<String, String> picClickCacheMap = new ConcurrentHashMap<>(); //点选文字


    public static void cacheImage(String captchaOriginalPathJigsaw, String captchaOriginalPathClick) {
        if (originalCacheMap.isEmpty() || slidingBlockCacheMap.isEmpty()) {
            //滑动拼图
            if (StringUtils.isBlank(captchaOriginalPathJigsaw)) {
                originalCacheMap.putAll(getResourcesImagesFile("images/jigsaw/original"));
                slidingBlockCacheMap.putAll(getResourcesImagesFile("images/jigsaw/slidingBlock"));
            } else {
                originalCacheMap.putAll(getImagesFile(captchaOriginalPathJigsaw + File.separator + "original"));
                slidingBlockCacheMap.putAll(getImagesFile(captchaOriginalPathJigsaw + File.separator + "slidingBlock"));
            }
        }

        if (picClickCacheMap.isEmpty()) {
            //点选文字
            if (StringUtils.isBlank(captchaOriginalPathClick)) {
                picClickCacheMap.putAll(getResourcesImagesFile("images/pic-click"));
            } else {
                picClickCacheMap.putAll(getImagesFile(captchaOriginalPathClick));
            }
        }
    }

    public static BufferedImage getOriginal() {
        return getBase64StrToImage(RandomUtils.getRandom(originalCacheMap.values()));
    }

    public static BufferedImage getSlidingBlock() {
        return getBase64StrToImage(RandomUtils.getRandom(slidingBlockCacheMap.values()));
    }

    public static BufferedImage getPicClick() {
        return getBase64StrToImage(RandomUtils.getRandom(picClickCacheMap.values()));
    }

    /**
     * 图片转base64 字符串
     */
    public static String getImageToBase64Str(BufferedImage templateImage) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(templateImage, "png", baos);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return AESUtil.base64Encode(baos.toByteArray());
    }

    /**
     * base64 字符串转图片
     */
    public static BufferedImage getBase64StrToImage(String base64String) {
        try {
            byte[] bytes = AESUtil.base64Decode(base64String);
            if (bytes == null) {
                return null;
            }
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            return ImageIO.read(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static Map<String, String> getResourcesImagesFile(String directory) {
        try {
            // 测试环境测试的时候，jar和项目下同时配置了图片时，没有优先获取项目下的图片
            // 暂时没有找出原因，这里先暂时判断下项目下是否有配置图片，然后优先采用项目配置
            URL url = ImageUtils.class.getClassLoader().getResource("");
            if (url != null) {
                String path = url.getPath() + File.separator + directory;
                if (TFile.fileExists(path)) {
                    return getImagesFile(path);
                }
            }

            url = ImageUtils.class.getClassLoader().getResource(directory);
            if (url == null) {
                return new HashMap<>(0);
            }

            if ("jar".equals(url.getProtocol())) {
                Map<String, String> imgMap = new HashMap<>(16);
                JarFile jar = ((JarURLConnection) url.openConnection()).getJarFile();
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().contains(directory) && !entry.isDirectory()) {
                        byte[] bytes = TFile.loadResource(entry.getName());
                        String string = Base64.getEncoder().encodeToString(bytes);
                        String filename = entry.getName().substring(entry.getName().lastIndexOf(File.separator) + 1);
                        imgMap.put(filename, string);
                    }
                }

                return imgMap;
            } else {
                return getImagesFile(url.getFile());
            }
        } catch (Exception e) {
            Logger.error("获取滑块图片异常", e);
        }

        return new HashMap<>(0);
    }

    private static Map<String, String> getImagesFile(String path) {
        Map<String, String> imgMap = new HashMap<>(16);
        File file = new File(path);
        File[] files = file.listFiles();
        if (files == null) {
            return imgMap;
        }

        for (File item : files) {
            try {
                byte[] bytes = TFile.loadFile(item);
                String string = Base64.getEncoder().encodeToString(bytes);
                imgMap.put(item.getName(), string);
            } catch (Exception e) {
                Logger.error("加载滑块验证码图片异常", e);
            }
        }


        return imgMap;
    }
}
