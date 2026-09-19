package com.siam.system.modular.package_service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.siam.system.modular.package_service.entity.WineStorageRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 取酒记录 Mapper · C-16（仅商家端取酒动作写入）
 */
@Mapper
public interface WineStorageRecordMapper extends BaseMapper<WineStorageRecord> {
}
