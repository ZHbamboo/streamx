/*
 * Copyright 2019 The StreamX Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.core.service.impl;

import com.streamxhub.streamx.console.base.domain.Constant;
import com.streamxhub.streamx.console.base.domain.RestRequest;
import com.streamxhub.streamx.console.base.exception.InternalException;
import com.streamxhub.streamx.console.base.mybatis.pager.MybatisPager;
import com.streamxhub.streamx.console.base.util.CommonUtils;
import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.entity.FlinkEnv;
import com.streamxhub.streamx.console.core.entity.SavePoint;
import com.streamxhub.streamx.console.core.enums.CheckPointType;
import com.streamxhub.streamx.console.core.mapper.SavePointMapper;
import com.streamxhub.streamx.console.core.service.FlinkEnvService;
import com.streamxhub.streamx.console.core.service.SavePointService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author benjobs
 */
@Slf4j
@Service
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true, rollbackFor = Exception.class)
public class SavePointServiceImpl extends ServiceImpl<SavePointMapper, SavePoint> implements SavePointService {

    @Autowired
    private FlinkEnvService flinkEnvService;

    @Override
    public void obsolete(Long appId) {
        this.baseMapper.obsolete(appId);
    }

    @Override
    public boolean save(SavePoint entity) {
        this.expire(entity);
        this.obsolete(entity.getAppId());
        return super.save(entity);
    }

    private void expire(SavePoint entity) {
        FlinkEnv flinkEnv = flinkEnvService.getByAppId(entity.getAppId());
        assert flinkEnv != null;
        int cpThreshold = Integer.parseInt(
            flinkEnv.convertFlinkYamlAsMap()
                .getOrDefault("state.checkpoints.num-retained", "5")
        );

        if (CheckPointType.CHECKPOINT.equals(CheckPointType.of(entity.getType()))) {
            cpThreshold = cpThreshold - 1;
        }

        if (cpThreshold == 0) {
            this.baseMapper.expireAll(entity.getAppId());
        } else {
            LambdaQueryWrapper<SavePoint> queryWrapper = new QueryWrapper<SavePoint>().lambda();
            queryWrapper.select(SavePoint::getTriggerTime)
                .eq(SavePoint::getAppId, entity.getAppId())
                .eq(SavePoint::getType, CheckPointType.CHECKPOINT.get())
                .orderByDesc(SavePoint::getTriggerTime);

            Page<SavePoint> savePointPage = this.baseMapper.selectPage(new Page<>(1, cpThreshold + 1), queryWrapper);
            if (!savePointPage.getRecords().isEmpty() && savePointPage.getRecords().size() > cpThreshold) {
                SavePoint savePoint = savePointPage.getRecords().get(cpThreshold - 1);
                this.baseMapper.expire(entity.getAppId(), savePoint.getTriggerTime());
            }
        }
    }

    @Override
    public SavePoint getLatest(Long id) {
        return this.baseMapper.getLatest(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(Long id, Application application) throws InternalException {
        SavePoint savePoint = getById(id);
        try {
            if (CommonUtils.notEmpty(savePoint.getPath())) {
                application.getFsOperator().delete(savePoint.getPath());
            }
            removeById(id);
            return true;
        } catch (Exception e) {
            throw new InternalException(e.getMessage());
        }
    }

    @Override
    public IPage<SavePoint> page(SavePoint savePoint, RestRequest request) {
        Page<SavePoint> page = new MybatisPager<SavePoint>().getPage(request, "trigger_time", Constant.ORDER_DESC);
        return this.baseMapper.page(page, savePoint.getAppId());
    }

    @Override
    public void removeApp(Application application) {
        Long appId = application.getId();
        baseMapper.removeApp(application.getId());
        try {
            application.getFsOperator().delete(application.getWorkspace().APP_SAVEPOINTS().concat("/").concat(appId.toString()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
