/*
 *Copyright © 2018 anji-plus
 *安吉加加信息技术有限公司
 *http://www.anji-plus.com
 *All rights reserved.
 */
package com.anji.captcha.util;

import java.util.*;

/**
 * 随机数相关工具类
 *
 * @author zhangzp
 */
public class RandomUtils {

    /**
     * 生成UUID
     *
     * @return 去出 "-" 的uuid字符串
     */
    public static String getUUID() {
        String uuid = UUID.randomUUID().toString();
        uuid = uuid.replace("-", "");
        return uuid;
    }

    /**
     * 获取字符串中一个随机字符
     *
     * @return 随机一个字符
     */
    public static String getRandom(String str) {
        return str.charAt(new Random().nextInt(str.length())) + "";
    }

    /**
     * 从数组中随机获取一个元素
     *
     * @param array 数组
     * @param <T>   数组元素类型
     * @return 随机一个元素
     */
    public static <T> T getRandom(T[] array) {
        if (array.length == 0) {
            return null;
        } else {
            int index = new Random().nextInt(array.length);
            return array[index];
        }
    }

    /**
     * 从结婚中随机获取一个元素
     *
     * @param collection 集合
     * @param <T>        集合元素类型
     * @return 随机一个元素
     */
    public static <T> T getRandom(Collection<T> collection) {
        if (collection.size() == 0) {
            return null;
        } else {
            int index = new Random().nextInt(collection.size());
            List<T> list = collection instanceof List ? (List<T>) collection : new ArrayList<>(collection);
            return list.get(index);
        }
    }

    /**
     * 随机范围内数字
     *
     * @param startNum 开始数字，包括
     * @param endNum   结束数字，不包括
     * @return 随机整数
     */
    public static Integer getRandomInt(int startNum, int endNum) {
        return new Random().nextInt(endNum - startNum) + startNum;
    }

    /**
     * 获取随机字符串
     *
     * @param length 长度
     * @return 随机字符串
     */
    public static String getRandomString(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(62);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }
}
