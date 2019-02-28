/*
 * Copyright 2017-2019 CodingApi .
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.codingapi.txlcn.tc.core;


import com.codingapi.txlcn.tc.aspect.Invocation;
import com.codingapi.txlcn.tc.aspect.TransactionInfo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.transaction.Status;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 分布式事务远程调用控制对象
 * ！！！不推荐经典应用程序使用，使用不当有可能造成事务流程出错 ！！！
 * <p>
 * Created by lorne on 2017/6/5.
 */
@Data
@Slf4j
public class DTXLocalContext {

    private final static ThreadLocal<DTXLocalContext> currentLocal = new InheritableThreadLocal<>();

    /**
     * 事务类型
     */
    private String transactionType;

    /**
     * 事务组
     */
    private String groupId;

    /**
     * 事务单元
     */
    private String unitId;

    /**
     * 是否是事务发起方
     */
    private boolean originalBranch;

    /**
     * 事务信息
     */
    private TransactionInfo transactionInfo;

    /**
     * 业务线相关方法
     */
    private List<Invocation> invocations = new ArrayList<>(6);

    /**
     * 业务相关资源
     */
    private Object resource;


    ////////////////////////// volatile ///////////////////////////////

    /**
     * 系统分布式事务状态
     */
    private int sysTransactionState = Status.STATUS_UNKNOWN;

    /**
     * 是否代理资源
     */
    private boolean proxy;

    //////// private     ///////////////////////
    /**
     * 临时值
     */
    private boolean proxyTmp;


    private boolean isProxyTmp() {
        return proxyTmp;
    }

    private void setProxyTmp(boolean proxyTmp) {
        this.proxyTmp = proxyTmp;
    }
    ///////   end      /////////////////////////


    /**
     * 获取当前线程变量。不推荐用此方法，会产生NullPointerException
     *
     * @return 当前线程变量
     */
    public static DTXLocalContext cur() {
        return currentLocal.get();
    }

    /**
     * 获取或新建一个线程变量。
     *
     * @return 当前线程变量
     */
    public static DTXLocalContext getOrNew() {
        if (currentLocal.get() == null) {
            currentLocal.set(new DTXLocalContext());
        }
        return currentLocal.get();
    }

    /**
     * 设置代理资源
     */
    public static void makeProxyConnection() {
        if (currentLocal.get() != null) {
            cur().proxyTmp = cur().proxy;
            cur().proxy = true;
        }
    }

    /**
     * 设置不代理资源
     */
    public static void makeUnProxyConnection() {
        if (currentLocal.get() != null) {
            cur().proxyTmp = cur().proxy;
            cur().proxy = false;
        }
    }

    /**
     * 撤销到上一步的资源代理状态
     */
    public static void undoProxyStatus() {
        if (currentLocal.get() != null) {
            cur().proxy = cur().proxyTmp;
        }
    }

    /**
     * 清理线程变量
     */
    public static void makeNeverAppeared() {
        if (currentLocal.get() != null) {
            log.debug("clean thread local[{}]: {}", DTXLocalContext.class.getSimpleName(), cur());
            currentLocal.remove();
        }
    }

    /**
     * 事务状态
     *
     * @return 1 commit 0 rollback
     */
    public static int transactionState(int userDtxState) {
        DTXLocalContext dtxLocalContext = Objects.requireNonNull(currentLocal.get(), "DTX can't be null.");
        return userDtxState == 1 ? dtxLocalContext.sysTransactionState : userDtxState;
    }
}
