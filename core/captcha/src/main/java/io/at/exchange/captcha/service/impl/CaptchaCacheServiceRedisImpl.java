package io.at.exchange.captcha.service.impl;

import io.at.exchange.captcha.service.CaptchaCacheService;
import io.at.base.cache.GlobalCache;

/**
 * Redis缓存
 *
 * @author zhangzp
 */
public class CaptchaCacheServiceRedisImpl implements CaptchaCacheService {
    @Override
    public void set(String key, String value, long expiresInSeconds) {
        GlobalCache.set(key, value, (int) expiresInSeconds);
    }

    @Override
    public boolean exists(String key) {
        return GlobalCache.containsKey(key);
    }

    @Override
    public void delete(String key) {
        GlobalCache.remove(key);
    }

    @Override
    public String get(String key) {
        return (String) GlobalCache.get(key);
    }
}
