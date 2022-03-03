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

package com.tencent.bk.job.upgrader.task;

import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.util.jwt.BasicJwtManager;
import com.tencent.bk.job.common.util.jwt.JwtManager;
import com.tencent.bk.job.upgrader.anotation.ExecuteTimeEnum;
import com.tencent.bk.job.upgrader.anotation.RequireTaskParam;
import com.tencent.bk.job.upgrader.anotation.UpgradeTask;
import com.tencent.bk.job.upgrader.anotation.UpgradeTaskInputParam;
import com.tencent.bk.job.upgrader.client.EsbCmdbClient;
import com.tencent.bk.job.upgrader.client.JobClient;
import com.tencent.bk.job.upgrader.model.AppInfo;
import com.tencent.bk.job.upgrader.model.cmdb.BizSetAttr;
import com.tencent.bk.job.upgrader.model.cmdb.BizSetFilter;
import com.tencent.bk.job.upgrader.model.cmdb.BizSetInfo;
import com.tencent.bk.job.upgrader.model.cmdb.BizSetScope;
import com.tencent.bk.job.upgrader.model.cmdb.CreateBizSetReq;
import com.tencent.bk.job.upgrader.model.cmdb.Rule;
import com.tencent.bk.job.upgrader.task.param.JobManageServerAddress;
import com.tencent.bk.job.upgrader.task.param.ParamNameConsts;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 业务集、全业务迁移任务
 */
@Slf4j
@RequireTaskParam(value = {
    @UpgradeTaskInputParam(value = JobManageServerAddress.class)
})
@UpgradeTask(
    dataStartVersion = "3.0.0.0",
    targetVersion = "3.5.0.0",
    targetExecuteTime = ExecuteTimeEnum.AFTER_UPDATE_JOB)
public class BizSetMigrationTask extends BaseUpgradeTask {

    private JobClient jobManageClient;

    private EsbCmdbClient esbCmdbClient;
    private List<AppInfo> specialAppInfoList;

    private String getJobHostUrlByAddress(String address) {
        if (!address.startsWith("http://") && !address.startsWith("https://")) {
            address = "http://" + address;
        }
        return address;
    }

    public BizSetMigrationTask(Properties properties) {
        super(properties);
    }

    @Override
    public void init() {
        String appCode = (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_APP_CODE);
        String appSecret = (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_APP_SECRET);
        String esbBaseUrl = (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_ESB_SERVICE_URL);
        String securityPublicKeyBase64 =
            (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_JOB_SECURITY_PUBLIC_KEY_BASE64);
        String securityPrivateKeyBase64 =
            (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_JOB_SECURITY_PRIVATE_KEY_BASE64);
        JwtManager jwtManager;
        try {
            jwtManager = new BasicJwtManager(securityPrivateKeyBase64, securityPublicKeyBase64);
        } catch (IOException | GeneralSecurityException e) {
            String msg = "Fail to generate jwt auth token";
            log.error(msg, e);
            throw new InternalException(msg, e, ErrorCode.INTERNAL_ERROR);
        }
        // 迁移过程最大预估时间：3h
        String jobAuthToken = jwtManager.generateToken(3 * 60 * 60 * 1000);
        jobManageClient = new JobClient(
            getJobHostUrlByAddress((String) properties.get(ParamNameConsts.INPUT_PARAM_JOB_MANAGE_SERVER_ADDRESS)),
            jobAuthToken
        );
        esbCmdbClient = new EsbCmdbClient(esbBaseUrl, appCode, appSecret, "zh-cn");
        // 从job-manage拉取业务集/全业务信息
        this.specialAppInfoList = getAllSpecialAppInfoFromManage();
    }

    private List<AppInfo> getAllSpecialAppInfoFromManage() {
        try {
            return jobManageClient.listSpecialApps();
        } catch (Exception e) {
            log.error("Fail to get special apps from job-manage, please confirm job-manage version>=3.5.0.0");
            throw e;
        }
    }

    /**
     * 为业务集构造选择业务的过滤条件
     *
     * @param appInfo 业务集信息
     * @return 过滤条件
     */
    private BizSetFilter buildAppSetFilter(AppInfo appInfo) {
        BizSetFilter filter = new BizSetFilter();
        filter.setCondition(BizSetFilter.CONDITION_OR);

        List<Rule> rules = new ArrayList<>();
        // 指定所有子业务ID
        Rule subAppIdsRule = new Rule();
        subAppIdsRule.setField("bk_biz_id");
        subAppIdsRule.setOperator(Rule.OPERATOR_IN);
        subAppIdsRule.setValue(appInfo.getSubAppIds());
        rules.add(subAppIdsRule);
        // 指定业务所属部门ID
        Rule operateDeptIdRule = new Rule();
        operateDeptIdRule.setField("bk_operate_dept_id");
        operateDeptIdRule.setOperator(Rule.OPERATOR_EQUAL);
        operateDeptIdRule.setValue(appInfo.getOperateDeptId());
        rules.add(operateDeptIdRule);

        filter.setRules(rules);
        return filter;
    }

    /**
     * 根据Job中现存业务集/全业务信息向CMDB创建业务集/全业务
     *
     * @param appInfo 业务集/全业务信息
     */
    private boolean createCMDBResourceForApp(AppInfo appInfo) {
        CreateBizSetReq createBizSetReq = new CreateBizSetReq();
        String desc = "Auto created by bk-job migration";
        String supplierAccount = (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_CMDB_DEFAULT_SUPPLIER_ACCOUNT);
        BizSetAttr attr = BizSetAttr.builder()
            .id(appInfo.getId())
            .name(appInfo.getName())
            .desc(desc)
            .maintainer(appInfo.getMaintainers())
            .timeZone(appInfo.getTimeZone())
            .language(appInfo.getLanguage())
            .supplierAccount(supplierAccount)
            .build();
        createBizSetReq.setAttr(attr);
        BizSetScope scope = new BizSetScope();
        if (appInfo.getAppType() == AppTypeEnum.APP_SET.getValue()) {
            scope.setMatchAll(false);
            scope.setFilter(buildAppSetFilter(appInfo));
        } else if (appInfo.getAppType() == AppTypeEnum.ALL_APP.getValue()) {
            // 匹配所有业务
            scope.setMatchAll(true);
            scope.setFilter(null);
        } else {
            log.warn("Not support app type:{}", appInfo.getAppType());
            return false;
        }
        createBizSetReq.setScope(scope);
        try {
            List<BizSetInfo> bizSetList = esbCmdbClient.searchBizSetById(attr.getId());
            if (CollectionUtils.isEmpty(bizSetList)) {
                Long bizSetId = esbCmdbClient.createBizSet(createBizSetReq);
                log.info("bizSet {} created", bizSetId);
                return true;
            } else {
                log.warn("bizSet {} already exists, ignore", attr.getId());
                return false;
            }
        } catch (Exception e) {
            FormattingTuple msg = MessageFormatter.format("Fail to create bizSet {}", createBizSetReq);
            log.error(msg.getMessage(), e);
            return false;
        }
    }

    /**
     * 给业务集/全业务运维人员授权
     *
     * @param appInfo 业务集/全业务信息
     */
    private void authAppMaintainers(AppInfo appInfo) {
        // TODO
    }

    @Override
    public int execute(String[] args) {
        log.info(getName() + " for version " + getTargetVersion() + " begin to run...");
        for (AppInfo appInfo : specialAppInfoList) {
            // 1.调用CMDB接口创建业务集/全业务
            if (createCMDBResourceForApp(appInfo)) {
                // 2.调用IAM接口授权
                authAppMaintainers(appInfo);
            }
        }
        return 0;
    }
}
