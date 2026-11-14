package com.siam.system.modular.package_goods.model.dto.internal;

import com.siam.system.modular.package_goods.entity.internal.GuessYouLike;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 猜你喜欢管理参数
 */
@Data
@ApiModel(value = "猜你喜欢管理参数")
public class GuessYouLikeParam {

    @ApiModelProperty(notes = "关联记录id集合(批量移除用)")
    private List<Integer> ids;

    @ApiModelProperty(notes = "商品id集合(批量添加用)")
    private List<Integer> goodsIds;

    @ApiModelProperty(notes = "排序数据集合(id + sortNumber)")
    private List<GuessYouLike> items;
}
