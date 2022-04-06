/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.common.esb.util;

import com.tencent.bk.job.common.esb.config.EsbConfig;
import com.tencent.bk.job.common.esb.model.EsbAppScopeDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.ApplicationContextRegister;

/**
 * ESB 业务与资源范围转换工具类
 */
public class EsbDTOAppScopeMappingHelper {
    /**
     * appId转换为bk_biz_id、bk_scope_type、bk_scope_id
     *
     * @param appId          Job业务ID
     * @param esbAppScopeDTO 资源范围-ESB DTO
     */
    public static void fillEsbAppScopeDTOByAppId(Long appId, EsbAppScopeDTO esbAppScopeDTO) {
        if (appId == null) {
            return;
        }
        AppScopeMappingService appScopeMappingService =
            ApplicationContextRegister.getBean(AppScopeMappingService.class);
        ResourceScope resourceScope = appScopeMappingService.getScopeByAppId(appId);
        esbAppScopeDTO.setScopeType(resourceScope.getType().getValue());
        esbAppScopeDTO.setScopeId(resourceScope.getId());
        EsbConfig esbConfig = ApplicationContextRegister.getBean(EsbConfig.class);
        // 如果不兼容bk_biz_id，那么使用bk_scope_type+bk_scope_id参数校验方式
        if (esbConfig.isBkBizIdEnabled()) {
            esbAppScopeDTO.setBizId(Long.valueOf(resourceScope.getId()));
        }
    }
}