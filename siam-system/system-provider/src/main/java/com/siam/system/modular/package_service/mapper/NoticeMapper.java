package com.siam.system.modular.package_service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.siam.system.modular.package_service.entity.Notice;
import org.apache.ibatis.annotations.Mapper;

/**
 * 公告 Mapper · C-14（公开接口，不带 member 前缀）
 */
@Mapper
public interface NoticeMapper extends BaseMapper<Notice> {
}
