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

package io.crate.action.sql;

import io.crate.data.RowConsumer;
import io.crate.data.BatchIterator;
import io.crate.data.Row;
import io.crate.exceptions.SQLExceptions;

import javax.annotation.Nullable;

public class RowConsumerToResultReceiver implements RowConsumer {

    private ResultReceiver resultReceiver;
    private int maxRows;
    private long rowCount = 0;
    private BatchIterator<Row> activeIt;

    public RowConsumerToResultReceiver(ResultReceiver resultReceiver, int maxRows) {
        this.resultReceiver = resultReceiver;
        this.maxRows = maxRows;
    }

    @Override
    public void accept(BatchIterator<Row> iterator, @Nullable Throwable failure) {
        if (failure == null) {
            consumeIt(iterator);
        } else {
            if (iterator != null) {
                iterator.close();
            }
            resultReceiver.fail(failure);
        }
    }

    private void consumeIt(BatchIterator<Row> iterator) {
        boolean allLoaded;
        try {
            while (iterator.moveNext()) {
                rowCount++;
                resultReceiver.setNextRow(iterator.currentElement());

                if (maxRows > 0 && rowCount % maxRows == 0) {
                    activeIt = iterator;
                    resultReceiver.batchFinished();
                    return; // resumed via postgres protocol, close is done later
                }
            }
            allLoaded = iterator.allLoaded();
        } catch (Throwable t) {
            iterator.close();
            resultReceiver.fail(t);
            return;
        }
        if (allLoaded) {
            iterator.close();
            resultReceiver.allFinished(false);
        } else {
            iterator.loadNextBatch().whenComplete((r, f) -> {
                if (f == null) {
                    consumeIt(iterator);
                } else {
                    iterator.close();
                    resultReceiver.fail(SQLExceptions.unwrap(f));
                }
            });
        }
    }

    /**
     * If this consumer suspended itself (due to {@code maxRows} being > 0, it will close the BatchIterator
     * and finish the ResultReceiver with interrupted=true.
     */
    public void closeAndFinishIfSuspended() {
        if (activeIt != null) {
            activeIt.close();
            resultReceiver.allFinished(true);
        }
    }

    public boolean suspended() {
        return activeIt != null;
    }

    public void replaceResultReceiver(ResultReceiver resultReceiver, int maxRows) {
        this.resultReceiver = resultReceiver;
        this.maxRows = maxRows;
    }

    public void resume() {
        assert activeIt != null : "resume must only be called if suspended() returned true and activeIt is not null";
        BatchIterator<Row> iterator = this.activeIt;
        this.activeIt = null;
        consumeIt(iterator);
    }
}
