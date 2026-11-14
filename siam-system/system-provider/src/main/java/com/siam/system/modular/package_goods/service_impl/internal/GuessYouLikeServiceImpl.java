package com.siam.system.modular.package_goods.service_impl.internal;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.siam.package_common.exception.StoneCustomerException;
import com.siam.system.modular.package_goods.entity.internal.GuessYouLike;
import com.siam.system.modular.package_goods.entity.internal.PointsMallGoods;
import com.siam.system.modular.package_goods.mapper.internal.GuessYouLikeMapper;
import com.siam.system.modular.package_goods.service.internal.GuessYouLikeService;
import com.siam.system.modular.package_goods.service.internal.PointsMallGoodsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GuessYouLikeServiceImpl implements GuessYouLikeService {

    @Autowired
    private GuessYouLikeMapper guessYouLikeMapper;

    @Autowired
    private PointsMallGoodsService pointsMallGoodsService;

    @Override
    public List<Map<String, Object>> listForDisplay(int limit) {
        List<Map<String, Object>> list = guessYouLikeMapper.selectDisplayGoodsList(limit);
        return list == null ? new ArrayList<>() : list;
    }

    @Override
    public List<Map<String, Object>> listManaged() {
        List<Map<String, Object>> list = guessYouLikeMapper.selectManagedList();
        return list == null ? new ArrayList<>() : list;
    }

    @Override
    public int addGoods(List<Integer> goodsIds) {
        if (goodsIds == null || goodsIds.isEmpty()) {
            throw new StoneCustomerException("请选择要添加的商品");
        }
        // 下一个排序号 = 当前最大排序号 + 1（追加到列表末尾）
        GuessYouLike maxSort = guessYouLikeMapper.selectOne(
                new QueryWrapper<GuessYouLike>().orderByDesc("sort_number").last("limit 1"));
        int nextSort = (maxSort == null || maxSort.getSortNumber() == null) ? 1 : maxSort.getSortNumber() + 1;

        int inserted = 0;
        for (Integer goodsId : goodsIds) {
            PointsMallGoods goods = pointsMallGoodsService.getById(goodsId);
            if (goods == null) {
                throw new StoneCustomerException("商品不存在(_goods_id=" + goodsId + ")");
            }
            // 唯一键 uk_goods_id 兜底：已存在的跳过，保证幂等
            Integer exists = guessYouLikeMapper.selectCount(
                    new QueryWrapper<GuessYouLike>().eq("goods_id", goodsId));
            if (exists != null && exists > 0) {
                log.info("猜你喜欢添加跳过已存在商品 goodsId={}", goodsId);
                continue;
            }
            GuessYouLike record = new GuessYouLike();
            record.setShopId(goods.getShopId());
            record.setGoodsId(goodsId);
            record.setSortNumber(nextSort++);
            record.setCreateTime(new Date());
            record.setUpdateTime(new Date());
            guessYouLikeMapper.insert(record);
            inserted++;
        }
        return inserted;
    }

    @Override
    public void removeByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new StoneCustomerException("请选择要移除的商品");
        }
        guessYouLikeMapper.deleteBatchIds(ids);
    }

    @Override
    public void updateSort(List<GuessYouLike> items) {
        if (items == null || items.isEmpty()) {
            throw new StoneCustomerException("排序数据不能为空");
        }
        for (GuessYouLike item : items) {
            if (item.getId() == null || item.getSortNumber() == null) {
                throw new StoneCustomerException("排序数据不完整(需 id 与 sortNumber)");
            }
            // 仅更新排序号，不触碰其他字段
            GuessYouLike update = new GuessYouLike();
            update.setId(item.getId());
            update.setSortNumber(item.getSortNumber());
            update.setUpdateTime(new Date());
            guessYouLikeMapper.updateById(update);
        }
    }
}
