package com.siam.package_common.util;

import com.aliyun.oss.OSSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

@Component
@ConfigurationProperties(value = "aliyun.oss")
public class AliyunOss {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private String accessKeyId;

    private String secret;

    private String endpoint;

    private String bucketName;

    /**
     * 证件类私有桶（仅会员身份认证材料使用）。
     * <p>
     * 背景：默认桶 lakes-public（2026-09-24 自原作者桶 siam-hangzhou 迁出）为公共读，
     * 任何人持 URL 即可取图；而身份证/学生证/教师资格证属敏感个人信息，
     * plan §8 要求 3 明确「严禁公共读」。
     * <p>
     * 隔离方式：**只把证件类挪到独立私有桶**，商品图/门店图/头像仍留在默认桶——
     * 因为全库 50+ 列存的是相对路径，整桶私有化会让四端全量裂图。
     * <p>
     * 留空 ⇒ 证件图退回默认桶（仅兼容旧配置，不推荐）。
     */
    private String privateBucketName;

    /**
     * 私有桶专用 endpoint（可选）。
     * <p>
     * 为什么需要独立字段：OSS 的 bucket 命名空间<strong>按地域隔离</strong>——
     * endpoint 与桶地域不一致时（如拿杭州 endpoint 访问成都的桶），
     * SDK 会拼出 {@code <bucket>.oss-cn-hangzhou.aliyuncs.com}，服务端返回 <strong>NoSuchBucket</strong>。
     * <p>
     * 取值规则：留空 ⇒ 跟随 {@link #endpoint}（两个桶同地域时的最简配置，向后兼容）；
     * 非空 ⇒ 私有桶的 putObject / deleteObject / generatePresignedUrl 全部走这个 endpoint。
     * <p>
     * 2026-09-24 用户拍板：私有桶建在西南1（成都）⇒ mvp profile 默认
     * {@code http://oss-cn-chengdu.aliyuncs.com}，可用环境变量 ALIYUN_OSS_PRIVATE_ENDPOINT 覆盖；
     * 同日换桶迁移后默认桶 lakes-public 也在成都，两桶同地域 ⇒ 留空即可。
     */
    private String privateEndpoint;

    /**
     * 私有桶对象的签名 URL 有效期（秒）。默认 300 秒（5 分钟）。
     */
    private Integer privateUrlExpireSeconds;

    /**
     * 本地联调存储根目录（仅 application-local.yml 配置）：
     * 非空 ⇒ 图片写本机磁盘、完全不连 OSS；留空 ⇒ 走 OSS（云端 mvp profile 即为此，行为不变）。
     * 落盘后的相对路径与 OSS 的 objectName 完全同构（都带 data/images/ 前缀），
     * 因此前端只需换一个域名前缀即可两边通用。
     */
    private String localStorageDir;

    private OSSClient ossClient;

    /**
     * 上传文件
     *
     * @param inputStream 文件输入流
     * @param objectName 文件全路径
     * @return
     **/
    public void uploadFile(InputStream inputStream, String objectName) throws IOException {
        // 本地联调分支：写本机磁盘，不连 OSS（由 aliyun.oss.localStorageDir 是否配置决定）
        if (isLocalStorageEnabled()) {
            File dest = resolveLocalFile(objectName);
            File parent = dest.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("本地存储目录创建失败：" + parent.getAbsolutePath());
            }
            try (OutputStream os = new FileOutputStream(dest)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    os.write(buffer, 0, len);
                }
            } finally {
                inputStream.close();
            }
            logger.info("本地存储已保存：" + dest.getAbsolutePath());
            return;
        }

        // 创建OSSClient实例
        OSSClient ossClient = getOssClient();

        // 上传内容到指定的存储空间（bucketName）并保存为指定的文件名称（objectName）
        ossClient.putObject(bucketName, objectName, inputStream);

        // 关闭OSSClient
        ossClient.shutdown();

        // 关闭IO流
        inputStream.close();
    }

    /**
     * 创建模拟文件夹
     * PS: OSS上传文件时如果某一级目录不存在，系统会自动创建的
     *
     * @param remotePath 文件夹全路径
     * @return
     **/
    public void createFolder(String remotePath){
        // 创建OSSClient实例
        OSSClient ossClient = getOssClient();

        // OSS 控制台中的文件夹本质上来说是创建了一个大小为 0 并以“/”结尾的对象，用于同类文件的归类操作和批处理
        byte[] bytes = new byte[0];
        ossClient.putObject(bucketName, remotePath, new ByteArrayInputStream(bytes));

        // 关闭OSSClient
        ossClient.shutdown();
    }

    /**
     * 关闭OSSClient
     * @return
     **/
    public void closeOssClient(){
        if(ossClient != null){
            ossClient.shutdown();
        }
    }

    /**
     * 判断文件是否存在
     * @param remoteFileName
     * @return
     */
    public Boolean doesObjectExist(String remoteFileName) {
        // 本地联调分支：查本机磁盘
        if (isLocalStorageEnabled()) {
            return resolveLocalFile(remoteFileName).exists();
        }

        // 创建OSSClient实例
        OSSClient ossClient = getOssClient();

        // 判断文件是否存在
        Boolean result=ossClient.doesObjectExist(bucketName, remoteFileName);

        // 关闭OSSClient
        ossClient.shutdown();

        return result;
    }

    /**
     * 上传证件类文件到**私有桶**。
     * <p>
     * 本地联调（localStorageDir 非空）时同样落本机磁盘，保证本地全流程不依赖 OSS、不需要 AccessKey。
     * 落盘路径与云端 objectName 同构，故签名 URL 生成那一层可以统一处理。
     **/
    public void uploadToPrivateBucket(InputStream inputStream, String objectName) throws IOException {
        if (isLocalStorageEnabled()) {
            // 复用与公共桶相同的落盘逻辑，只是路径前缀不同（data/images/private/...）
            uploadFile(inputStream, objectName);
            return;
        }
        if (!isPrivateBucketEnabled()) {
            throw new IOException("未配置 aliyun.oss.privateBucketName，证件类图片无法上传到私有桶");
        }
        OSSClient client = getPrivateOssClient();
        try {
            client.putObject(privateBucketName.trim(), objectName, inputStream);
        } finally {
            client.shutdown();
            inputStream.close();
        }
    }

    /**
     * 删除默认桶中的对象（历史遗留证件图清理用：私有桶上线前的证件图仍落在默认桶）。
     *
     * @return true=已删除或本就不存在（幂等）；false=删除失败
     **/
    public boolean deleteObject(String objectName) {
        return deleteFromBucket(bucketName, objectName, endpoint);
    }

    /**
     * 删除私有桶中的对象（「审核即弃」清理用）。
     *
     * @return true=已删除或本就不存在（幂等）；false=删除失败，调用方应保留路径以便下次重试
     **/
    public boolean deleteFromPrivateBucket(String objectName) {
        if (!isPrivateBucketEnabled() && !isLocalStorageEnabled()) {
            logger.warn("未配置 aliyun.oss.privateBucketName，跳过删除：{}", objectName);
            return false;
        }
        return deleteFromBucket(privateBucketName, objectName, resolvePrivateEndpoint());
    }

    /**
     * 按指定 endpoint 删除对象。默认桶传 {@link #endpoint}，私有桶传 {@link #resolvePrivateEndpoint()}。
     **/
    private boolean deleteFromBucket(String bucket, String objectName, String effectiveEndpoint) {
        if (objectName == null || objectName.trim().isEmpty()) {
            return true;
        }
        if (isLocalStorageEnabled()) {
            File file = resolveLocalFile(objectName);
            return !file.exists() || file.delete();
        }
        OSSClient client = new OSSClient(effectiveEndpoint, accessKeyId, secret);
        try {
            client.deleteObject(bucket.trim(), objectName.trim());
            return true;
        } catch (Exception e) {
            logger.error("删除 OSS 对象失败：bucket={} object={} 原因：{}", bucket, objectName, e.getMessage());
            return false;
        } finally {
            client.shutdown();
        }
    }

    /**
     * 生成私有桶对象的**短时效签名 URL**（plan §8 要求 3）。
     * <p>
     * 本地联调模式返回**相对路径**（如 `data/images/private/member/17/xxx.png`），
     * 由前端拼上本机端点前缀 `GlobalConfig.ossUrl` 即可——
     * 这样后端不需要知道自己对外的域名/端口，本地与云端两种模式都只需一个返回值。
     *
     * @param objectName    对象名（库中存的相对路径）
     * @param expireSeconds 有效期秒数；null 或 <=0 时取配置默认值
     * @return 云端=绝对签名 URL；本地=相对路径；objectName 为空时返回 null
     **/
    public String generatePrivatePresignedUrl(String objectName, Integer expireSeconds) {
        if (objectName == null || objectName.trim().isEmpty()) {
            return null;
        }
        String name = objectName.trim();
        while (name.startsWith("/")) {
            name = name.substring(1);
        }
        if (isLocalStorageEnabled()) {
            return name;
        }
        if (!isPrivateBucketEnabled()) {
            logger.warn("未配置 aliyun.oss.privateBucketName，无法生成证件图访问地址：{}", objectName);
            return null;
        }
        int seconds = (expireSeconds != null && expireSeconds > 0) ? expireSeconds
                : (privateUrlExpireSeconds != null && privateUrlExpireSeconds > 0 ? privateUrlExpireSeconds : 300);
        Date expiration = new Date(System.currentTimeMillis() + seconds * 1000L);
        OSSClient client = getPrivateOssClient();
        try {
            return client.generatePresignedUrl(privateBucketName.trim(), name, expiration).toString();
        } catch (Exception e) {
            logger.error("生成私有桶签名URL失败：{}，原因：{}", objectName, e.getMessage());
            return null;
        } finally {
            client.shutdown();
        }
    }

    /**
     * 私有桶是否已配置
     **/
    public boolean isPrivateBucketEnabled() {
        return privateBucketName != null && !privateBucketName.trim().isEmpty();
    }

    /**
     * 私有桶实际使用的 endpoint：privateEndpoint 留空则回落到默认桶的 endpoint
     * （两桶同地域的旧配置仍照常工作；不同地域时由 yml/环境变量显式指定）。
     **/
    private String resolvePrivateEndpoint() {
        return (privateEndpoint == null || privateEndpoint.trim().isEmpty()) ? endpoint : privateEndpoint.trim();
    }

    /**
     * 私有桶专用 OSSClient：endpoint 用 {@link #resolvePrivateEndpoint()}，
     * AK/Secret 与默认桶共用同一对。
     * <p>
     * 注意：与 {@link #getOssClient()} 不同，这里<strong>不写回共享字段 ossClient</strong>，
     * 避免两个地域的 client 互相覆盖。
     **/
    public OSSClient getPrivateOssClient() {
        return new OSSClient(resolvePrivateEndpoint(), accessKeyId, secret);
    }

    public String getPrivateEndpoint() {
        return privateEndpoint;
    }

    public void setPrivateEndpoint(String privateEndpoint) {
        this.privateEndpoint = privateEndpoint;
    }

    /**
     * 本地存储是否启用：localStorageDir 配了非空值
     **/
    public boolean isLocalStorageEnabled() {
        return localStorageDir != null && !localStorageDir.trim().isEmpty();
    }

    /**
     * 把对象名解析成本机文件。
     * 相对路径与 OSS 的 objectName 同构（data/images/...），并做目录穿越防护。
     **/
    public File resolveLocalFile(String objectName) {
        String name = objectName == null ? "" : objectName.replace("\\", "/");
        while (name.startsWith("/")) {
            name = name.substring(1);
        }
        if (name.contains("..")) {
            throw new IllegalArgumentException("非法的文件路径：" + objectName);
        }
        return new File(localStorageDir.trim(), name);
    }

    public String getLocalStorageDir() {
        return localStorageDir;
    }

    public void setLocalStorageDir(String localStorageDir) {
        this.localStorageDir = localStorageDir;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getPrivateBucketName() {
        return privateBucketName;
    }

    public void setPrivateBucketName(String privateBucketName) {
        this.privateBucketName = privateBucketName;
    }

    public Integer getPrivateUrlExpireSeconds() {
        return privateUrlExpireSeconds;
    }

    public void setPrivateUrlExpireSeconds(Integer privateUrlExpireSeconds) {
        this.privateUrlExpireSeconds = privateUrlExpireSeconds;
    }

    public OSSClient getOssClient() {
        /*if(ossClient == null){
            ossClient = new OSSClient(endpoint, accessKeyId, secret);
        }*/
        ossClient = new OSSClient(endpoint, accessKeyId, secret);
        return ossClient;
    }

    public void setOssClient(OSSClient ossClient) {
        this.ossClient = ossClient;
    }

    public static void main(String[] args) {
        /*// 检查二级目录是否存在
        boolean found = ossClient.doesObjectExist(bucketName, remotePath);
        if(found == false){
            // 不存在则创建文件夹后再上传
            createFolder(remotePath);
        }*/
    }
}