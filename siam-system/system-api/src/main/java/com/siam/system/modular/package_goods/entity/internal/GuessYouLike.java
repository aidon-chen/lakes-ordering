package com.siam.system.modular.package_goods.entity.internal;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 猜你喜欢-商家手动挑选关联表
 * 仅存商品关联与排序，删除关联不影响商品本体（商品与关联可共存）
 */
@Data
@TableName("tb_guess_you_like")
@ApiModel(value = "猜你喜欢商品关联表")
public class GuessYouLike {

    @TableId(type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(notes = "店铺id(NULL=平台级挑选)")
    private Integer shopId;

    @ApiModelProperty(notes = "商城商品id(tb_points_mall_goods.id)")
    private Integer goodsId;

    @ApiModelProperty(notes = "排序号(升序展示)")
    private Integer sortNumber;

    @ApiModelProperty(notes = "创建时间")
    private Date createTime;

    @ApiModelProperty(notes = "修改时间")
    private Date updateTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getShopId() {
        return shopId;
    }

    public void setShopId(Integer shopId) {
        this.shopId = shopId;
    }

    public Integer getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(Integer goodsId) {
        this.goodsId = goodsId;
    }

    public Integer getSortNumber() {
        return sortNumber;
    }

    public void setSortNumber(Integer sortNumber) {
        this.sortNumber = sortNumber;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
