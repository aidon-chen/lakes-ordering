package com.siam.system.modular.package_goods.controller.admin.internal;

import com.siam.package_common.constant.BasicResultCode;
import com.siam.package_common.entity.BasicData;
import com.siam.package_common.entity.BasicResult;
import com.siam.system.modular.package_goods.model.dto.internal.GuessYouLikeParam;
import com.siam.system.modular.package_goods.service.internal.GuessYouLikeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 猜你喜欢管理接口（商家端后台）
 * 关联表仅存商品挑选与排序；移除关联不影响商品本体
 *
 * 鉴权：本页属商家端日常管理，不再要求超管身份
 *（@AdminPermission 只放过 username==admin，商家账号如 admin-lakes 会被拒；
 * 参见 AdminAdvertisementController 同类处理）。
 * 安全性由 /rest/admin/** 的全局登录拦截保证：未登录仍返回 code 2。
 */
@Slf4j
@RestController
@RequestMapping(value = "/rest/admin/pointsMall/guessYouLike")
@Transactional(rollbackFor = Exception.class)
@Api(tags = "后台猜你喜欢管理相关接口", description = "AdminGuessYouLikeController")
public class AdminGuessYouLikeController {

    @Autowired
    private GuessYouLikeService guessYouLikeService;

    @ApiOperation(value = "猜你喜欢商品列表(含已下架，附真实销量/偏移量/展示销量)")
    @PostMapping(value = "/list")
    public BasicResult list(@RequestBody @Validated(value = {}) GuessYouLikeParam param){
        List<Map<String, Object>> list = guessYouLikeService.listManaged();
        return BasicResult.success(list);
    }

    @ApiOperation(value = "批量添加商品到猜你喜欢(跳过已存在，追加到列表末尾)")
    @PostMapping(value = "/add")
    public BasicResult add(@RequestBody @Validated(value = {}) GuessYouLikeParam param){
        BasicData basicResult = new BasicData();

        int inserted = guessYouLikeService.addGoods(param.getGoodsIds());
        basicResult.setData(inserted);
        basicResult.setSuccess(true);
        basicResult.setCode(BasicResultCode.SUCCESS);
        basicResult.setMessage("添加成功");
        return basicResult;
    }

    @ApiOperation(value = "批量移除猜你喜欢商品(仅删关联行，商品本体不受影响)")
    @PostMapping(value = "/remove")
    public BasicResult remove(@RequestBody @Validated(value = {}) GuessYouLikeParam param){
        BasicResult basicResult = new BasicResult();

        guessYouLikeService.removeByIds(param.getIds());
        basicResult.setSuccess(true);
        basicResult.setCode(BasicResultCode.SUCCESS);
        basicResult.setMessage("移除成功");
        return basicResult;
    }

    @ApiOperation(value = "批量更新猜你喜欢排序号")
    @PostMapping(value = "/updateSort")
    public BasicResult updateSort(@RequestBody @Validated(value = {}) GuessYouLikeParam param){
        BasicResult basicResult = new BasicResult();

        guessYouLikeService.updateSort(param.getItems());
        basicResult.setSuccess(true);
        basicResult.setCode(BasicResultCode.SUCCESS);
        basicResult.setMessage("排序成功");
        return basicResult;
    }
}
