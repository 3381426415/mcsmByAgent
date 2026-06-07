package com.gitcode.mcsm_backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gitcode.mcsm_backend.Entity.AdminLog;
import com.gitcode.mcsm_backend.mapper.AdminLogMapper;
import org.springframework.stereotype.Service;

/**
 * 管理员日志服务 - 记录和查询管理员操作日志
 */
@Service
public class AdminLogService extends ServiceImpl<AdminLogMapper, AdminLog> {

    /**
     * 保存日志
     */
    public void saveLog(AdminLog log) {
        this.save(log);
    }

    /**
     * 分页查询日志
     */
    public Page<AdminLog> getLogPage(int page, int size, String module, String operatorName) {
        Page<AdminLog> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<AdminLog> wrapper = new LambdaQueryWrapper<>();

        if (module != null && !module.isEmpty()) {
            wrapper.eq(AdminLog::getModule, module);
        }
        if (operatorName != null && !operatorName.isEmpty()) {
            wrapper.like(AdminLog::getOperatorName, operatorName);
        }

        wrapper.orderByDesc(AdminLog::getCreateTime);
        return this.page(pageParam, wrapper);
    }
}