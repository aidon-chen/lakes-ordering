package com.siam.package_common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Date;

/**
 * OSS服务器工具类
 **/
@Slf4j
@Component
public class OSSUtils {

    /** 证件类文件名随机串用（文件名带随机成分，避免"毫秒时间戳可爆破"） */
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private AliyunOss aliyunOSS;

    /**
     * 上传图片--前端文件上传交互
     *
     * @param file 文件对象
     * @param path 模块名称
     * @param id 表唯一标识主键id，如：用户id、商品id；类型应该定义成Object，因为mongodb数据表主键id为字符串类型
     * @return
     **/
    public String uploadImage(MultipartFile file, String path, Object id){
        log.debug("name: " + file.getName());
        log.debug("originalFilename: " + file.getOriginalFilename());
        String objectName = null;
        try {
            InputStream inputStream = file.getInputStream();

            // 文件名处理
            String fileName = file.getOriginalFilename();
            fileName = "siam_" + new Date().getTime() + fileName.substring(fileName.lastIndexOf("."));

            // 根据模块名称、用户id作为文件夹命名
            objectName = "data/images/" + path + "/" + id + "/" + fileName;

            aliyunOSS.uploadFile(inputStream, objectName);
        } catch (IOException e) {
            //e.printStackTrace();
            throw new RuntimeException("图片上传失败");
        }
        return objectName;
    }

    /**
     * 上传图片--逻辑处理时上传文件
     *
     * @return
     **/
    public void uploadImage(InputStream inputStream, String savePath){
        try {
            aliyunOSS.uploadFile(inputStream, savePath);
        } catch (IOException e) {
            //e.printStackTrace();
            throw new RuntimeException("图片上传失败");
        }
    }

    /**
     * 上传**证件类**图片到私有桶（会员身份认证材料专用）。
     * <p>
     * 与 {@link #uploadImage(MultipartFile, String, Object)} 的两点差异：
     * <ol>
     *   <li>落在独立私有桶（`aliyun.oss.privateBucketName`），桶级禁止匿名读（plan §8 要求 3）；</li>
     *   <li>文件名额外带 **8 位随机串**，并带 `private/` 路径段。原因：原实现文件名只有
     *       `siam_<毫秒时间戳>`、id 又是自增小整数，公共读下等于可爆破；虽然私有桶已经解决
     *       未授权访问，但随机串是零成本的纵深防御，同时便于按 `data/images/private/`
     *       这个前缀把存量证件图整体识别出来做迁移。</li>
     * </ol>
     *
     * @param file 文件对象
     * @param path 模块名称
     * @param id   表唯一标识主键id
     * @return 对象名（相对路径，库中直接存这个值）
     **/
    public String uploadPrivateImage(MultipartFile file, String path, Object id) {
        log.debug("name: " + file.getName());
        log.debug("originalFilename: " + file.getOriginalFilename());
        String objectName = null;
        try {
            InputStream inputStream = file.getInputStream();

            // 文件名处理：siam_<毫秒>_<8位随机>.<ext>
            String fileName = file.getOriginalFilename();
            String ext = fileName.substring(fileName.lastIndexOf("."));
            fileName = "siam_" + new Date().getTime() + "_" + randomToken() + ext;

            // data/images/private/<模块>/<id>/<文件名>
            objectName = "data/images/private/" + path + "/" + id + "/" + fileName;

            aliyunOSS.uploadToPrivateBucket(inputStream, objectName);
        } catch (IOException e) {
            log.error("证件类图片上传失败：{}", e.getMessage());
            throw new RuntimeException("图片上传失败");
        }
        return objectName;
    }

    /**
     * 删除私有桶中的证件图（「审核即弃」清理用，幂等）
     **/
    public boolean deletePrivateImage(String objectName) {
        return aliyunOSS.deleteFromPrivateBucket(objectName);
    }

    /**
     * 删除**默认桶**中的对象。仅供历史遗留清理使用：
     * 私有桶上线前产生的证件图落在默认桶，清理时要按路径前缀分流到正确的桶，否则会永远删不掉。
     **/
    public boolean deleteImage(String objectName) {
        return aliyunOSS.deleteObject(objectName);
    }

    /**
     * 生成证件图的**短时效签名 URL**（云端=绝对签名地址；本地=相对路径，由前端拼本机前缀）
     **/
    public String generatePrivateUrl(String objectName) {
        return aliyunOSS.generatePrivatePresignedUrl(objectName, null);
    }

    /**
     * 私有桶是否已配置（未配置时证件图退回默认桶，属降级配置，调用方可据此告警）
     **/
    public boolean isPrivateBucketEnabled() {
        return aliyunOSS.isPrivateBucketEnabled();
    }

    /**
     * 8 位十六进制随机串（SecureRandom）
     **/
    private String randomToken() {
        byte[] bytes = new byte[4];
        RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(8);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    /**
     * 判断文件是否存在
     * @param remoteFileName
     * @return
     */
    public Boolean doesObjectExist(String remoteFileName) {
        return aliyunOSS.doesObjectExist(remoteFileName);
    }
}