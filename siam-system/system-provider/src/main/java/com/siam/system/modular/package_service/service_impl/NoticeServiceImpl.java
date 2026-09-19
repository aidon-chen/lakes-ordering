package com.siam.system.modular.package_service.service_impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.siam.package_common.exception.StoneCustomerException;
import com.siam.system.modular.package_goods.entity.Shop;
import com.siam.system.modular.package_goods.mapper.ShopMapper;
import com.siam.system.modular.package_service.entity.Notice;
import com.siam.system.modular.package_service.mapper.NoticeMapper;
import com.siam.system.modular.package_service.model.vo.NoticeVo;
import com.siam.system.modular.package_service.service.NoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 公告服务实现 · C-14
 */
@Service
public class NoticeServiceImpl implements NoticeService {

    /** 列表摘要最大长度（画板摘要为两行以内，40 字安全） */
    private static final int SUMMARY_LIMIT = 40;

    @Autowired
    private NoticeMapper noticeMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Override
    public List<NoticeVo> list(Integer shopId) {
        QueryWrapper<Notice> qw = new QueryWrapper<>();
        qw.eq("status", 1);
        if (shopId != null) {
            // 门店专属公告 + 全店通用公告（shop_id IS NULL）一起返回
            qw.and(w -> w.isNull("shop_id").or().eq("shop_id", shopId));
        }
        // 置顶优先，其余按发布时间倒序（用字符串列名，避开 Boolean isTop 的 Lombok getter 命名歧义）
        qw.orderByDesc("is_top").orderByDesc("publish_time");

        List<Notice> list = noticeMapper.selectList(qw);
        Map<Integer, String> shopNames = shopNameMap(list);
        return list.stream()
                .map(n -> toVo(n, false, shopNames))
                .collect(Collectors.toList());
    }

    @Override
    public NoticeVo detail(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null || !Objects.equals(notice.getStatus(), 1)) {
            throw new StoneCustomerException("公告不存在或已下线");
        }
        Map<Integer, String> shopNames = shopNameMap(Collections.singletonList(notice));
        return toVo(notice, true, shopNames);
    }

    /* ==================== 私有 ==================== */

    private Map<Integer, String> shopNameMap(List<Notice> notices) {
        Set<Integer> shopIds = notices.stream()
                .map(Notice::getShopId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (shopIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return shopMapper.selectBatchIds(shopIds).stream()
                .collect(Collectors.toMap(Shop::getId, Shop::getName, (a, b) -> a));
    }

    private NoticeVo toVo(Notice notice, boolean withContent, Map<Integer, String> shopNames) {
        NoticeVo vo = new NoticeVo();
        vo.setId(notice.getId());
        vo.setTitle(notice.getTitle());
        if (withContent) {
            vo.setContent(notice.getContent());
        }
        vo.setSummary(summary(notice.getContent()));
        vo.setIsTop(notice.getIsTop() != null && notice.getIsTop());
        if (notice.getPublishTime() != null) {
            vo.setPublishDate(new SimpleDateFormat("yyyy-MM-dd").format(notice.getPublishTime()));
        }
        if (notice.getShopId() != null) {
            vo.setShopName(shopNames.get(notice.getShopId()));
        }
        return vo;
    }

    /** 富文本 → 纯文本摘要（剥标签 + 解常用实体 + 压空白 + 截断） */
    private String summary(String content) {
        String text = stripHtml(content);
        if (text.length() <= SUMMARY_LIMIT) {
            return text;
        }
        return text.substring(0, SUMMARY_LIMIT) + "…";
    }

    private String stripHtml(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        String s = html.replaceAll("<[^>]+>", " ");
        s = s.replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
        return s.replaceAll("\\s+", " ").trim();
    }
}
