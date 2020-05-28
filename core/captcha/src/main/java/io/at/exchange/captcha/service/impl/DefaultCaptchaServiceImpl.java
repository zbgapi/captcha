/*
 *Copyright © 2018 anji-plus
 *安吉加加信息技术有限公司
 *http://www.anji-plus.com
 *All rights reserved.
 */
package io.at.exchange.captcha.service.impl;


import io.at.base.utils.TypeUtil;
import io.at.exchange.captcha.model.common.RepCodeEnum;
import io.at.exchange.captcha.model.common.ResponseModel;
import io.at.exchange.captcha.model.vo.CaptchaVO;
import io.at.exchange.captcha.service.CaptchaCacheService;
import io.at.exchange.captcha.service.CaptchaService;
import io.at.exchange.captcha.util.AESUtil;
import io.at.exchange.captcha.util.ImageUtils;
import io.at.exchange.captcha.util.StringUtils;
import com.dd.tools.TProperties;
import com.dd.tools.log.Logger;
import io.at.base.config.SignCacheConfig;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author raodeming
 * @date 2019/12/25
 */
public class DefaultCaptchaServiceImpl implements CaptchaService {
    /**
     * 滑动验证码原生图片路径
     */
    private String captchaOriginalPathJigsaw;
    /**
     * 选文字验证码原生图片路径
     */
    private String captchaOriginalPathClick;
    /**
     * aes.key(16位，和前端加密保持一致)
     */
    private String aseKey;
    /**
     * 缓存Key
     */
    protected static String REDIS_SECOND_CAPTCHA_KEY = "RUNNING:CAPTCHA:second-%s";

    protected CaptchaCacheService captchaCacheService;

    private Map<String, CaptchaService> instances = new HashMap<>();

    public DefaultCaptchaServiceImpl() {
        this.aseKey = TypeUtil.s(TProperties.getString("config", "captcha.aes.key"), "BGxdEUOZkXka4HSj");
        initCache();

        instances.put("blockPuzzleCaptchaService", new BlockPuzzleCaptchaServiceImpl());
        instances.put("clickWordCaptchaService", new ClickWordCaptchaServiceImpl());
        Logger.debug("supported-captchaTypes-service:" + instances.keySet().toString());

        //初始化底图
        this.captchaOriginalPathJigsaw = TProperties.getString("config", "captcha.captcha-original-path.jigsaw");
        this.captchaOriginalPathClick = TProperties.getString("config", "captcha.captcha-original-path.pic-click");
        ImageUtils.cacheImage(captchaOriginalPathJigsaw, captchaOriginalPathClick);
        Logger.info("--->>>初始化验证码底图<<<---");
    }

    public void initCache() {
        if (SignCacheConfig.isRedis()) {
            this.captchaCacheService = new CaptchaCacheServiceRedisImpl();
        } else {
            this.captchaCacheService = new CaptchaCacheServiceMemImpl();
        }
    }

    private CaptchaService getService(String captchaType) {
        return instances.get(captchaType.concat("CaptchaService"));
    }

    @Override
    public ResponseModel get(CaptchaVO captchaVO) {
        if (captchaVO == null) {
            return RepCodeEnum.NULL_ERROR.parseError("captchaVO");
        }
        if (StringUtils.isEmpty(captchaVO.getCaptchaType())) {
            return RepCodeEnum.NULL_ERROR.parseError("类型");
        }
        if ("blockPuzzle".equals(captchaVO.getCaptchaType())) {
            captchaVO.setCaptchaOriginalPath(captchaOriginalPathJigsaw);
        } else {
            captchaVO.setCaptchaOriginalPath(captchaOriginalPathClick);
        }
        return getService(captchaVO.getCaptchaType()).get(captchaVO);
    }

    @Override
    public ResponseModel check(CaptchaVO captchaVO) {
        if (captchaVO == null) {
            return RepCodeEnum.NULL_ERROR.parseError("captchaVO");
        }
        if (StringUtils.isEmpty(captchaVO.getCaptchaType())) {
            return RepCodeEnum.NULL_ERROR.parseError("类型");
        }
        if (StringUtils.isEmpty(captchaVO.getToken())) {
            return RepCodeEnum.NULL_ERROR.parseError("token");
        }
        return getService(captchaVO.getCaptchaType()).check(captchaVO);
    }

    @Override
    public ResponseModel verification(CaptchaVO captchaVO) {
        if (captchaVO == null) {
            return RepCodeEnum.NULL_ERROR.parseError("captchaVO");
        }
        if (StringUtils.isEmpty(captchaVO.getCaptchaVerification())) {
            return RepCodeEnum.NULL_ERROR.parseError("captchaVerification");
        }
        try {
            //aes解密
            String s = AESUtil.aesDecrypt(captchaVO.getCaptchaVerification(), aseKey);
            String token = s.split("---")[0];
            String pointJson = s.split("---")[1];
            //取坐标信息
            String codeKey = String.format(REDIS_SECOND_CAPTCHA_KEY, token);
            if (!captchaCacheService.exists(codeKey)) {
                return ResponseModel.errorMsg(RepCodeEnum.API_CAPTCHA_INVALID);
            }
            String redisData = captchaCacheService.get(codeKey);
            //二次校验取值后，即刻失效
            captchaCacheService.delete(codeKey);
            if (!pointJson.equals(redisData)) {
                return ResponseModel.errorMsg(RepCodeEnum.API_CAPTCHA_COORDINATE_ERROR);
            }
        } catch (Exception e) {
            Logger.error("验证码坐标解析失败", e);
            return ResponseModel.errorMsg(e.getMessage());
        }
        return ResponseModel.success();
    }

}
