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

package com.tencent.bk.job.execute.model.esb.v2.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.esb.model.EsbReq;
import com.tencent.bk.job.common.esb.model.job.EsbFileSourceDTO;
import com.tencent.bk.job.common.esb.model.job.EsbServerDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 分发文件请求报文
 */
@Getter
@Setter
public class EsbPlatformPushFileRequest extends EsbReq {
    /**
     * 源业务ID
     */
    @JsonProperty("source_bk_biz_id")
    private Long sourceAppId;

    /**
     * 目标业务ID
     */
    @JsonProperty("target_bk_biz_id")
    private Long targetAppId;

    /**
     * 文件分发任务名称
     */
    @JsonProperty("task_name")
    private String name;

    /**
     * 源文件
     */
    @JsonProperty("file_source")
    private List<EsbFileSourceDTO> fileSources;

    /**
     * 目标路径
     */
    @JsonProperty("file_target_path")
    private String targetPath;

    /**
     * 目标服务器账户
     */
    private String account;

    @JsonProperty("target_server")
    private EsbServerDTO targetServer;

    /**
     * 任务执行完成之后回调URL
     */
    @JsonProperty("bk_callback_url")
    private String callbackUrl;

    /**
     * 下载限速，单位MB
     */
    @JsonProperty("download_speed_limit")
    private Integer downloadSpeedLimit;

    /**
     * 上传限速，单位MB
     */
    @JsonProperty("upload_speed_limit")
    private Integer uploadSpeedLimit;

    /**
     * 超时时间，单位秒
     */
    @JsonProperty("timeout")
    private Integer timeout;

    /**
     * 文件重名处理，1-直接覆盖，2-追加源IP
     */
    @JsonProperty("duplicate_handler")
    private Integer duplicateHandler;
}

