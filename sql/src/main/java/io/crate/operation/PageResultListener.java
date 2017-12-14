/*
 * Licensed to Crate under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.  Crate licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial
 * agreement.
 */

package io.crate.operation;

/**
 * An interface to signal more paging data is needed.
 * The PageResultListener has to take action which ensures that
 * {@link io.crate.jobs.PageBucketReceiver#setBucket(int, io.crate.data.Bucket, boolean, PageResultListener)}
 * or
 * {@link io.crate.jobs.PageBucketReceiver#failure(int, Throwable)}
 * is called when more data is needed.
 */
public interface PageResultListener {

    /**
     * Indicates that more data is needed. The implementation has to trigger an action to request
     * more data if {@code needMore} is true.
     * Used together with {@link io.crate.jobs.PageBucketReceiver}.
     * @param needMore True if more data is needed and should be requested, false otherwise.
     */
    void needMore(boolean needMore);
}
