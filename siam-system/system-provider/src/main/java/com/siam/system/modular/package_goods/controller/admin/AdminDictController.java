package com.siam.system.modular.package_goods.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.siam.package_common.annoation.AdminPermission;
import com.siam.package_common.entity.BasicResult;
import com.siam.package_common.exception.StoneCustomerException;
import com.siam.system.modular.package_goods.entity.DictItem;
import com.siam.system.modular.package_goods.entity.DictType;
import com.siam.system.modular.package_goods.service.DictItemService;
import com.siam.system.modular.package_goods.service.DictTypeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端 A-02 业务字典配置
 * 鉴权：写操作使用 @AdminPermission，超管账号直接放行（AdminPermissionAop）
 */
@RestController
@RequestMapping(value = "/rest/admin/dict")
@Transactional(rollbackFor = Exception.class)
@Api(tags = "后台业务字典模块相关接口", description = "AdminDictController")
public class AdminDictController {

    @Autowired
    private DictTypeService dictTypeService;

    @Autowired
    private DictItemService dictItemService;

    /* ==================== 字典类型 ==================== */

    @ApiOperation(value = "字典类型列表")
    @PostMapping(value = "/type/list")
    public BasicResult typeList(@RequestBody DictType dictType) {
        Page<DictType> page = dictTypeService.getListByPage(dictType);
        return BasicResult.success(page);
    }

    @AdminPermission
    @ApiOperation(value = "新增字典类型")
    @PostMapping(value = "/type/insert")
    public BasicResult typeInsert(@RequestBody DictType dictType) {
        if (dictTypeService.selectByCode(dictType.getCode()) != null) {
            throw new StoneCustomerException("该类型编码已存在");
        }
        dictTypeService.insert(dictType);
        return BasicResult.success();
    }

    @AdminPermission
    @ApiOperation(value = "修改字典类型")
    @PutMapping(value = "/type/update")
    public BasicResult typeUpdate(@RequestBody DictType dictType) {
        //编码唯一校验：排除自身 id
        DictType sameCode = dictTypeService.selectByCode(dictType.getCode());
        if (sameCode != null && !sameCode.getId().equals(dictType.getId())) {
            throw new StoneCustomerException("该类型编码已存在");
        }
        dictTypeService.update(dictType);
        return BasicResult.success();
    }

    @AdminPermission
    @ApiOperation(value = "删除字典类型")
    @DeleteMapping(value = "/type/delete")
    public BasicResult typeDelete(@RequestBody DictType dictType) {
        //该类型下仍有字典项时拒绝删除
        DictType exist = dictTypeService.selectById(dictType.getId());
        if (exist != null) {
            Integer count = dictItemService.countByTypeCode(exist.getCode());
            if (count != null && count > 0) {
                throw new StoneCustomerException("该类型下存在字典项，请先删除字典项");
            }
        }
        dictTypeService.delete(dictType.getId());
        return BasicResult.success();
    }

    /* ==================== 字典项 ==================== */

    @ApiOperation(value = "字典项列表")
    @PostMapping(value = "/item/list")
    public BasicResult itemList(@RequestBody DictItem dictItem) {
        Page<DictItem> page = dictItemService.getListByPage(dictItem);
        return BasicResult.success(page);
    }

    @AdminPermission
    @ApiOperation(value = "新增字典项")
    @PostMapping(value = "/item/insert")
    public BasicResult itemInsert(@RequestBody DictItem dictItem) {
        if (dictItemService.selectByTypeCodeAndValue(dictItem.getTypeCode(), dictItem.getValue()) != null) {
            throw new StoneCustomerException("该类型下的键值已存在");
        }
        dictItemService.insert(dictItem);
        return BasicResult.success();
    }

    @AdminPermission
    @ApiOperation(value = "修改字典项")
    @PutMapping(value = "/item/update")
    public BasicResult itemUpdate(@RequestBody DictItem dictItem) {
        //键值唯一校验：同 typeCode 下 value 唯一，排除自身 id
        DictItem sameValue = dictItemService.selectByTypeCodeAndValue(dictItem.getTypeCode(), dictItem.getValue());
        if (sameValue != null && !sameValue.getId().equals(dictItem.getId())) {
            throw new StoneCustomerException("该类型下的键值已存在");
        }
        dictItemService.update(dictItem);
        return BasicResult.success();
    }

    @AdminPermission
    @ApiOperation(value = "删除字典项")
    @DeleteMapping(value = "/item/delete")
    public BasicResult deleteItem(@RequestBody DictItem dictItem) {
        dictItemService.delete(dictItem.getId());
        return BasicResult.success();
    }
}