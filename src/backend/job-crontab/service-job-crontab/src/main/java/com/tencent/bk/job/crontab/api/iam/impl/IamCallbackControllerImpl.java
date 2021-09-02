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

package com.tencent.bk.job.crontab.api.iam.impl;

import com.tencent.bk.job.common.iam.util.IamRespUtil;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.crontab.api.iam.IamCallbackController;
import com.tencent.bk.job.crontab.model.dto.CronJobInfoDTO;
import com.tencent.bk.job.crontab.service.CronJobService;
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO;
import com.tencent.bk.sdk.iam.dto.callback.request.IamSearchCondition;
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.ListAttributeResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.ListAttributeValueResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceByPolicyResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.SearchInstanceResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 23/3/2020 11:20
 */
@Slf4j
@RestController
public class IamCallbackControllerImpl implements IamCallbackController {

    private final CronJobService cronJobService;

    @Autowired
    public IamCallbackControllerImpl(CronJobService cronJobService) {
        this.cronJobService = cronJobService;
    }

    private InstanceInfoDTO convert(CronJobInfoDTO cronJobInfo) {
        InstanceInfoDTO instanceInfo = new InstanceInfoDTO();
        instanceInfo.setId(String.valueOf(cronJobInfo.getId()));
        instanceInfo.setDisplayName(cronJobInfo.getName());
        return instanceInfo;
    }

    private SearchInstanceResponseDTO searchInstanceResp(
        CallbackRequestDTO callbackRequest
    ) {
        log.info(
            "{}|{}|{}|{}",
            callbackRequest.getMethod().getMethod(),
            callbackRequest.getType(),
            callbackRequest.getFilter(),
            callbackRequest.getPage()
        );
        IamSearchCondition searchCondition = IamSearchCondition.fromReq(callbackRequest);
        CronJobInfoDTO condition = new CronJobInfoDTO();
        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        PageData<CronJobInfoDTO> cronJobInfoPageData;
        condition.setAppId(searchCondition.getAppIdList().get(0));
        condition.setName(callbackRequest.getFilter().getKeyword());

        baseSearchCondition.setStart(searchCondition.getStart().intValue());
        baseSearchCondition.setLength(searchCondition.getLength().intValue());

        cronJobInfoPageData =
            cronJobService.listPageCronJobInfos(condition, baseSearchCondition);

        return IamRespUtil.getSearchInstanceRespFromPageData(cronJobInfoPageData, this::convert);
    }

    private ListInstanceResponseDTO listInstanceResp(
        CallbackRequestDTO callbackRequest
    ) {
        log.info(
            "{}|{}|{}|{}",
            callbackRequest.getMethod().getMethod(),
            callbackRequest.getType(),
            callbackRequest.getFilter(),
            callbackRequest.getPage()
        );
        IamSearchCondition searchCondition = IamSearchCondition.fromReq(callbackRequest);
        CronJobInfoDTO condition = new CronJobInfoDTO();
        condition.setAppId(searchCondition.getAppIdList().get(0));
        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(searchCondition.getStart().intValue());
        baseSearchCondition.setLength(searchCondition.getLength().intValue());

        PageData<CronJobInfoDTO> cronJobInfoPageData =
            cronJobService.listPageCronJobInfos(condition, baseSearchCondition);

        return IamRespUtil.getListInstanceRespFromPageData(cronJobInfoPageData, this::convert);
    }

    @Override
    public CallbackBaseResponseDTO cronJobCallback(CallbackRequestDTO callbackRequest) {
        log.debug("Receive iam callback|{}", callbackRequest);
        CallbackBaseResponseDTO response;
        switch (callbackRequest.getMethod()) {
            case LIST_INSTANCE:
                response = listInstanceResp(callbackRequest);
                break;
            case FETCH_INSTANCE_INFO:
                log.debug("Fetch instance info request!|{}|{}|{}", callbackRequest.getType(),
                    callbackRequest.getFilter(), callbackRequest.getPage());

                IamSearchCondition searchCondition = IamSearchCondition.fromReq(callbackRequest);
                List<Object> instanceAttributeInfoList = new ArrayList<>();
                for (String instanceId : searchCondition.getIdList()) {
                    try {
                        long id = Long.parseLong(instanceId);
                        InstanceInfoDTO instanceInfo = new InstanceInfoDTO();
                        instanceInfo.setId(instanceId);
                        instanceInfo.setDisplayName(cronJobService.getCronJobNameById(id));
                        instanceAttributeInfoList.add(instanceInfo);
                    } catch (NumberFormatException e) {
                        log.error("Parse object id failed!|{}", instanceId, e);
                    }
                }

                FetchInstanceInfoResponseDTO fetchInstanceInfoResponse = new FetchInstanceInfoResponseDTO();
                fetchInstanceInfoResponse.setCode(0L);
                fetchInstanceInfoResponse.setData(instanceAttributeInfoList);

                response = fetchInstanceInfoResponse;
                break;
            case LIST_ATTRIBUTE:
                log.debug("List attribute request!|{}|{}|{}", callbackRequest.getType(), callbackRequest.getFilter(),
                    callbackRequest.getPage());
                response = new ListAttributeResponseDTO();
                response.setCode(0L);
                break;
            case LIST_ATTRIBUTE_VALUE:
                log.debug("List attribute value request!|{}|{}|{}", callbackRequest.getType(),
                    callbackRequest.getFilter(), callbackRequest.getPage());
                response = new ListAttributeValueResponseDTO();
                response.setCode(0L);
                break;
            case LIST_INSTANCE_BY_POLICY:
                log.debug("List instance by policy request!|{}|{}|{}", callbackRequest.getType(),
                    callbackRequest.getFilter(), callbackRequest.getPage());
                response = new ListInstanceByPolicyResponseDTO();
                response.setCode(0L);
                break;
            case SEARCH_INSTANCE:
                response = searchInstanceResp(callbackRequest);
                break;
            default:
                log.error("Unknown callback method!|{}|{}|{}|{}", callbackRequest.getMethod(),
                    callbackRequest.getType(), callbackRequest.getFilter(), callbackRequest.getPage());
                response = new CallbackBaseResponseDTO();
        }
        return response;
    }
}
