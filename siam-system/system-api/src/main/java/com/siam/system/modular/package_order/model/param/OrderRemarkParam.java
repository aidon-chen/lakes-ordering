package com.siam.system.modular.package_order.model.param;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.List;

/**
 * 改单备注入参（2026-09-23 新增，配合出品看板「待制作 / 出品中」两列的改备注入口）。
 *
 * <p>继承 {@link OrderParam} ⇒ 复用其 {@code id}（订单主键）与从 {@code Order} 继承来的 {@code remark}（整单备注）。
 */
@Data
@ApiModel(value = "改单备注入参")
public class OrderRemarkParam extends OrderParam {

    /** 单品备注：只传需要改的行；服务端会校验每行是否真属于该订单 */
    private List<DetailRemark> details;

    @Data
    public static class DetailRemark {
        /** tb_order_detail 主键 */
        private Integer id;
        private String remark;
    }
}
