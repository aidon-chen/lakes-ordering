package com.siam.system.modular.package_goods.controller.member;

import com.siam.package_common.entity.BasicResult;
import com.siam.package_common.exception.StoneCustomerException;
import com.siam.package_common.util.OSSUtils;
import com.siam.system.modular.package_user.auth.cache.MemberSessionManager;
import com.siam.system.modular.package_user.entity.Member;
import com.siam.system.util.TokenUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/rest/member")
@Transactional(rollbackFor = Exception.class)
@Api(tags = "用户上传图片模块相关接口", description = "AdminUploadController")
public class UploadController {

    @Autowired
    private OSSUtils ossUtils;

    @Autowired
    private MemberSessionManager memberSessionManager;

    @ApiOperation(value = "用户上传单张图片")
    @PostMapping(value = "/uploadSingleImage")
    public BasicResult uploadSingleImage(@RequestParam("file") MultipartFile file) {
        Member loginMember = memberSessionManager.getSession(TokenUtil.getToken());

        if(file==null || file.getSize()==0){
            throw new StoneCustomerException(500, "图片不能为空");
        }

        String imageUrl = ossUtils.uploadImage(file, "member", loginMember.getId());
        return BasicResult.success(imageUrl);
    }

    /**
     * 上传**证件类**图片（会员身份认证材料专用）——落独立私有桶，桶级禁止匿名读。
     * <p>
     * 为什么不复用 {@link #uploadSingleImage}：默认桶是对象级公共读，身份证/学生证/教师资格证
     * 属敏感个人信息，plan §8 要求 3 明确「严禁公共读」。而整桶私有化会让全库 50+ 列存相对路径的
     * 图片集体裂图，故只把证件类分流到私有桶。
     * <p>
     * 返回 {path, url}：path 用于提交表单入库；url 用于前端**立即回显**（云端=短时效签名 URL，
     * 本地联调=相对路径）。前端不能再用 ossUrl 硬拼 path，否则私有桶下必然 403。
     */
    @ApiOperation(value = "用户上传证件类图片（私有桶，仅身份认证材料用）")
    @PostMapping(value = "/uploadIdentityImage")
    public BasicResult uploadIdentityImage(@RequestParam("file") MultipartFile file) {
        Member loginMember = memberSessionManager.getSession(TokenUtil.getToken());

        if (file == null || file.getSize() == 0) {
            throw new StoneCustomerException(500, "图片不能为空");
        }

        String objectName = ossUtils.uploadPrivateImage(file, "member", loginMember.getId());
        Map<String, Object> data = new HashMap<>();
        data.put("path", objectName);
        data.put("url", ossUtils.generatePrivateUrl(objectName));
        return BasicResult.success(data);
    }

    @ApiOperation(value = "用户上传多张图片")
    @PostMapping(value = "/uploadMultipleImage")
    public BasicResult uploadMultipleImage(@RequestParam("fileList") List<MultipartFile> fileList) {
        Member loginMember = memberSessionManager.getSession(TokenUtil.getToken());
        String imageUrl = null;
        for (MultipartFile file : fileList) {
            if (file == null || file.getSize() == 0) {
                throw new StoneCustomerException(500, "图片上传失败");
            }
            if (imageUrl == null) {
                imageUrl = ossUtils.uploadImage(file, "member", loginMember.getId());
            } else {
                imageUrl = imageUrl + "," + ossUtils.uploadImage(file, "member", loginMember.getId());
            }
        }
        return BasicResult.success(imageUrl);
    }
}