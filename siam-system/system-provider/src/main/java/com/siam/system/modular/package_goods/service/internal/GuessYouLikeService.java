package com.siam.system.modular.package_goods.service.internal;

import com.siam.system.modular.package_goods.entity.internal.GuessYouLike;

import java.util.List;
import java.util.Map;

public interface GuessYouLikeService {

    /**
     * 单个商品销量偏移量上限，防刷榜
     */
    int SALES_OFFSET_MAX = 1000000;

    /**
     * 顾客端展示列表（仅已上架，按 sort_number 升序，限 limit 条）；无配置时返回空列表，由调用方回退热销逻辑
     */
    List<Map<String, Object>> listForDisplay(int limit);

    /**
     * 商家端管理列表（含已下架商品，附真实销量/偏移量/展示销量）
     */
    List<Map<String, Object>> listManaged();

    /**
     * 批量添加商品到猜你喜欢（跳过已存在的商品，追加到列表末尾）
     *
     * @return 实际新增条数
     */
    int addGoods(List<Integer> goodsIds);

    /**
     * 按关联记录 id 批量移除（仅删关联行，商品本体不受影响）
     */
    void removeByIds(List<Integer> ids);

    /**
     * 批量更新排序号
     */
    void updateSort(List<GuessYouLike> items);
}
