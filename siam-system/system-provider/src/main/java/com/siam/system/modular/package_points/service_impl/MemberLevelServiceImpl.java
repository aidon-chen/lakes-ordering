package com.siam.system.modular.package_points.service_impl;

import com.siam.system.modular.package_points.entity.MemberLevelSetting;
import com.siam.system.modular.package_points.mapper.MemberLevelSettingMapper;
import com.siam.system.modular.package_points.mapper.MemberPointsLogMapper;
import com.siam.system.modular.package_points.service.MemberLevelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MemberLevelServiceImpl implements MemberLevelService {

    @Autowired
    private MemberLevelSettingMapper memberLevelSettingMapper;

    @Autowired
    private MemberPointsLogMapper memberPointsLogMapper;

    @Override
    public Map<String, Object> getLevelInfo(Integer memberId) {
        Integer totalPoints = memberPointsLogMapper.sumPointsByMemberId(memberId);
        if (totalPoints == null) {
            totalPoints = 0;
        }
        List<MemberLevelSetting> levels = memberLevelSettingMapper.selectList(null);
        levels.sort((a, b) -> a.getThreshold().compareTo(b.getThreshold()));

        MemberLevelSetting current = null;
        MemberLevelSetting next = null;
        for (MemberLevelSetting item : levels) {
            if (item.getThreshold() <= totalPoints) {
                current = item;
            } else {
                next = item;
                break; // levels 已按阈值升序，第一个不满足的即下一档
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalPoints", totalPoints);
        result.put("level", current == null ? 1 : current.getLevel());
        result.put("levelName", current == null ? "青铜" : current.getName());
        result.put("nextLevelName", next == null ? null : next.getName());
        result.put("nextThreshold", next == null ? null : next.getThreshold());
        result.put("gap", next == null ? 0 : next.getThreshold() - totalPoints);
        result.put("levels", levels);
        return result;
    }
}
