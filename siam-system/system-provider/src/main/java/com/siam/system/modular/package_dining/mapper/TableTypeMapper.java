package com.siam.system.modular.package_dining.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.siam.system.modular.package_dining.entity.TableType;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/** 注意：与 package_goods.TableTypeMapper 简类名相同，必须显式指定 bean 名避免 Spring 冲突 */
@Mapper
@Repository("diningTableTypeMapper")
public interface TableTypeMapper extends BaseMapper<TableType> {
}
