/*
 *Copyright © 2018 anji-plus
 *安吉加加信息技术有限公司
 *http://www.anji-plus.com
 *All rights reserved.
 */
package io.at.exchange.captcha.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.dd.tools.log.Logger;
import io.at.exchange.captcha.model.common.RepCodeEnum;
import io.at.exchange.captcha.model.common.ResponseModel;
import io.at.exchange.captcha.model.vo.CaptchaVO;
import io.at.exchange.captcha.util.AESUtil;
import io.at.exchange.captcha.util.ImageUtils;
import io.at.exchange.captcha.util.RandomUtils;
import io.at.exchange.captcha.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Random;

/**
 * 滑动验证码
 *
 * @author raodeming
 * @date 2019/12/25
 */
public class BlockPuzzleCaptchaServiceImpl extends AbstractCaptchaservice {
    /**
     * 右下角水印文字，默认 zbg.com
     */
    private String waterMark;
    /**
     * 水印文字字体，默认宋体
     */
    private String waterMarkFont;
    /**
     * 校验滑动拼图允许误差偏移量(默认5像素)
     */
    private String slipOffset;
    /**
     * aes.key(16位，和前端加密保持一致)
     */
    private String aseKey;

    public BlockPuzzleCaptchaServiceImpl() {
        super();
        this.waterMark = getConfig("captcha.water.mark", "zbg.com");
        this.waterMarkFont = getConfig("captcha.water.font", "宋体");
        this.slipOffset = getConfig("captcha.slip.offset", "5");
        this.aseKey = getConfig("captcha.aes.key", "BGxdEUOZkXka4HSj");
    }

    @Override
    public ResponseModel get(CaptchaVO captchaVO) {

        //原生图片
        BufferedImage originalImage = ImageUtils.getOriginal();
        //设置水印
        Graphics backgroundGraphics = originalImage.getGraphics();
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        Font watermark = new Font(waterMarkFont, Font.BOLD, HAN_ZI_SIZE / 2);
        backgroundGraphics.setFont(watermark);
        backgroundGraphics.setColor(Color.white);
        backgroundGraphics.drawString(waterMark, width - ((HAN_ZI_SIZE / 2) * (waterMark.length())) - 5, height - (HAN_ZI_SIZE / 2) + 7);

        //抠图图片
        BufferedImage jigsawImage = ImageUtils.getSlidingBlock();
        CaptchaVO captcha = pictureTemplatesCut(originalImage, jigsawImage);
        if (captcha == null
                || StringUtils.isBlank(captcha.getJigsawImageBase64())
                || StringUtils.isBlank(captcha.getOriginalImageBase64())) {
            return ResponseModel.errorMsg(RepCodeEnum.API_CAPTCHA_ERROR);
        }
        return ResponseModel.successData(captcha);
    }

    @Override
    public ResponseModel check(CaptchaVO captchaVO) {
        //取坐标信息
        String codeKey = String.format(REDIS_CAPTCHA_KEY, captchaVO.getToken());
        if (!captchaCacheService.exists(codeKey)) {
            return ResponseModel.errorMsg(RepCodeEnum.API_CAPTCHA_INVALID);
        }
        String s = captchaCacheService.get(codeKey);
        //验证码只用一次，即刻失效
        captchaCacheService.delete(codeKey);
        Point point = null;
        Point point1 = null;
        String pointJson = null;
        try {
            point = JSONObject.parseObject(s, Point.class);
            //aes解密
            pointJson = decrypt(captchaVO.getPointJson(), aseKey);
            point1 = JSONObject.parseObject(pointJson, Point.class);
        } catch (Exception e) {
            Logger.error("验证码坐标解析失败", e);
            return ResponseModel.errorMsg(e.getMessage());
        }
        if (point.x - Integer.parseInt(slipOffset) > point1.x
                || point1.x > point.x + Integer.parseInt(slipOffset)
                || point.y != point1.y) {
            return ResponseModel.errorMsg(RepCodeEnum.API_CAPTCHA_COORDINATE_ERROR);
        }
        //校验成功，将信息存入redis
        String secondKey = String.format(REDIS_SECOND_CAPTCHA_KEY, captchaVO.getToken());
        captchaCacheService.set(secondKey, pointJson, EXPIRESIN_THREE);
        captchaVO.setResult(true);
        return ResponseModel.successData(captchaVO);
    }

    @Override
    public ResponseModel verification(CaptchaVO captchaVO) {
        return null;
    }

    /**
     * 根据模板切图
     */
    public CaptchaVO pictureTemplatesCut(BufferedImage originalImage, BufferedImage jigsawImage) {
        try {
            CaptchaVO dataVO = new CaptchaVO();

            int originalWidth = originalImage.getWidth();
            int originalHeight = originalImage.getHeight();
            int jigsawWidth = jigsawImage.getWidth();
            int jigsawHeight = jigsawImage.getHeight();

            //随机生成拼图坐标
            Point point = generateJigsawPoint(originalWidth, originalHeight, jigsawWidth, jigsawHeight);
            int x = (int) point.getX();
            int y = (int) point.getY();

            //生成新的拼图图像
            BufferedImage newJigsawImage = new BufferedImage(jigsawWidth, jigsawHeight, jigsawImage.getType());
            Graphics2D graphics = newJigsawImage.createGraphics();
            graphics.setBackground(Color.white);

            int bold = 5;
            BufferedImage subImage = originalImage.getSubimage(x, 0, jigsawWidth, jigsawHeight);

            // 获取拼图区域
            newJigsawImage = dealCutPictureByTemplate(subImage, jigsawImage, newJigsawImage);

            // 设置“抗锯齿”的属性
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setStroke(new BasicStroke(bold, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
            graphics.drawImage(newJigsawImage, 0, 0, null);
            graphics.dispose();

            ByteArrayOutputStream os = new ByteArrayOutputStream();//新建流。
            ImageIO.write(newJigsawImage, IMAGE_TYPE_PNG, os);//利用ImageIO类提供的write方法，将bi以png图片的数据模式写入流。
            byte[] jigsawImages = os.toByteArray();

            // 源图生成遮罩
            byte[] oriCopyImages = dealOriPictureByTemplate(originalImage, jigsawImage, x, 0);
            dataVO.setOriginalImageBase64(AESUtil.base64Encode(oriCopyImages).replaceAll("\r|\n", ""));
            dataVO.setJigsawImageBase64(AESUtil.base64Encode(jigsawImages).replaceAll("\r|\n", ""));
            dataVO.setToken(RandomUtils.getUUID());

//            base64StrToImage(AESUtil.base64Encode(oriCopyImages), "/Users/zhangzhipeng/Pictures/原图.png");
//            base64StrToImage(AESUtil.base64Encode(jigsawImages), "/Users/zhangzhipeng/Pictures/滑动.png");


            //将坐标信息存入redis中
            String codeKey = String.format(REDIS_CAPTCHA_KEY, dataVO.getToken());
            captchaCacheService.set(codeKey, JSONObject.toJSONString(point), EXPIRESIN_SECONDS);
            return dataVO;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 抠图后原图生成
     */
    private static byte[] dealOriPictureByTemplate(BufferedImage oriImage, BufferedImage templateImage, int x, int y) throws Exception {
        // 源文件备份图像矩阵 支持alpha通道的rgb图像
        BufferedImage oriCopyImage = new BufferedImage(oriImage.getWidth(), oriImage.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        // 源文件图像矩阵
        int[][] oriImageData = getData(oriImage);
        // 模板图像矩阵
        int[][] templateImageData = getData(templateImage);

        //copy 源图做不透明处理
        for (int i = 0; i < oriImageData.length; i++) {
            for (int j = 0; j < oriImageData[0].length; j++) {
                int rgb = oriImage.getRGB(i, j);
                int r = (0xff & rgb);
                int g = (0xff & (rgb >> 8));
                int b = (0xff & (rgb >> 16));
                //无透明处理
                rgb = r + (g << 8) + (b << 16) + (255 << 24);
                oriCopyImage.setRGB(i, j, rgb);
            }
        }

        for (int i = 0; i < templateImageData.length; i++) {
            for (int j = 0; j < templateImageData[0].length - 5; j++) {
                int rgb = templateImage.getRGB(i, j);
                //对源文件备份图像(x+i,y+j)坐标点进行透明处理
                if (rgb != 16777215 && rgb <= 0) {
                    int rgbOri = oriCopyImage.getRGB(x + i, y + j);
                    int r = (0xff & rgbOri);
                    int g = (0xff & (rgbOri >> 8));
                    int b = (0xff & (rgbOri >> 16));
                    rgbOri = r + (g << 8) + (b << 16) + (50 << 24);
                    oriCopyImage.setRGB(x + i, y + j, rgbOri);
                }
            }
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();//新建流。
        ImageIO.write(oriCopyImage, "png", os);//利用ImageIO类提供的write方法，将bi以png图片的数据模式写入流。
        return os.toByteArray();
    }


    /**
     * 根据模板图片抠图
     */
    private static BufferedImage dealCutPictureByTemplate(BufferedImage oriImage, BufferedImage templateImage, BufferedImage targetImage) {
        // 源文件图像矩阵
        int[][] oriImageData = getData(oriImage);
        // 模板图像矩阵
        int[][] templateImageData = getData(templateImage);
        // 模板图像宽度

        for (int i = 0; i < templateImageData.length; i++) {
            // 模板图片高度
            for (int j = 0; j < templateImageData[0].length; j++) {
                // 如果模板图像当前像素点不是白色 copy源文件信息到目标图片中
                int rgb = templateImageData[i][j];
                if (rgb != 16777215 && rgb <= 0) {
                    targetImage.setRGB(i, j, oriImageData[i][j]);
                    int rgbBri = targetImage.getRGB(i, j);
                    if (j > 3 && j < templateImageData[0].length - 3) {
                        int rgbBefore = templateImageData[i][j - 1];
                        int rgbAfter = templateImageData[i][j + 1];
                        if (rgbBefore > 0 || rgbAfter > 0) {
                            int rgb1 = new Color(255, 255, 255, 150).getRGB();
                            targetImage.setRGB(i, j, rgb1);
                        }
                    }
                }


            }
        }
        return targetImage;
    }

    /**
     * 生成图像矩阵
     */
    private static int[][] getData(BufferedImage bimg) {
        int[][] data = new int[bimg.getWidth()][bimg.getHeight()];
        for (int i = 0; i < bimg.getWidth(); i++) {
            for (int j = 0; j < bimg.getHeight(); j++) {
                data[i][j] = bimg.getRGB(i, j);
            }
        }
        return data;
    }

    /**
     * 随机生成拼图坐标
     *
     * @param originalWidth
     * @param originalHeight
     * @param jigsawWidth
     * @param jigsawHeight
     * @return
     */
    private static Point generateJigsawPoint(int originalWidth, int originalHeight, int jigsawWidth, int jigsawHeight) {
        Random random = new Random();
        int widthDifference = originalWidth - jigsawWidth;
        int heightDifference = originalHeight - jigsawHeight;
        int x, y = 0;
        if (widthDifference <= 0) {
            x = 5;
        } else {
            x = random.nextInt(originalWidth - jigsawWidth - 130) + 100;
        }
        if (heightDifference <= 0) {
            y = 5;
        } else {
            y = random.nextInt(originalHeight - jigsawHeight) + 5;
        }
        return new Point(x, y);
    }

}
