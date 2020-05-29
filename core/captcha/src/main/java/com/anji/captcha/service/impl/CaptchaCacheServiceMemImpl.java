package com.anji.captcha.service.impl;

import com.dd.tools.cache.CachedHashMap;
import com.anji.captcha.service.CaptchaCacheService;

/**
 * 对于分布式部署的应用，我们建议应用自己实现CaptchaCacheService，比如用Redis，参考service/spring-boot代码示例。
 * 如果应用是单点的，也没有使用redis，那默认使用内存。
 * 内存缓存只适合单节点部署的应用，否则验证码生产与验证在节点之间信息不同步，导致失败。
 *
 * @author lide1202@hotmail.com
 * @date 2020-05-12
 */

public class CaptchaCacheServiceMemImpl implements CaptchaCacheService {
    private static final CachedHashMap<String, String> CACHE_MAP = new CachedHashMap<String, String>().autoRemove(true).interval(5).create();

    @Override
    public void set(String key, String value, long expiresInSeconds) {
        CACHE_MAP.put(key, value, expiresInSeconds);
    }

    @Override
    public boolean exists(String key) {
        return CACHE_MAP.containsKey(key);
    }

    @Override
    public void delete(String key) {
        CACHE_MAP.remove(key);
    }

    @Override
    public String get(String key) {
        return CACHE_MAP.get(key);
    }
}
