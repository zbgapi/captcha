/*
 *Copyright © 2018 anji-plus
 *安吉加加信息技术有限公司
 *http://www.anji-plus.com
 *All rights reserved.
 */
package io.at.exchange.captcha.controller;

import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import com.anji.captcha.service.impl.DefaultCaptchaServiceImpl;
import com.dd.http.server.module.annontationRouter.annotation.Body;
import com.dd.http.server.module.annontationRouter.annotation.Router;

/**
 * @author raodeming
 * @date 2019/12/25
 */
@Router(path = "/captcha")
public class CaptchaController {

    private CaptchaService captchaService = new DefaultCaptchaServiceImpl();

    @Router(path = "/get", method = "POST")
    public ResponseModel get(@Body CaptchaVO captchaVO) {
        return captchaService.get(captchaVO);
    }

    @Router(path = "/check", method = "POST")
    public ResponseModel check(@Body CaptchaVO captchaVO) {
        return captchaService.check(captchaVO);
    }

    @Router(value = "/verify", method = "POST")
    public ResponseModel verify(@Body CaptchaVO captchaVO) {
        return captchaService.verification(captchaVO);
    }


}
