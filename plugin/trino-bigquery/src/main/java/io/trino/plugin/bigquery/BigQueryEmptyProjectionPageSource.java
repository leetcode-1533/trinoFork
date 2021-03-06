/*
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
package io.trino.plugin.bigquery;

import com.google.common.collect.ImmutableList;
import io.trino.spi.Page;
import io.trino.spi.PageBuilder;
import io.trino.spi.connector.ConnectorPageSource;

public class BigQueryEmptyProjectionPageSource
        implements ConnectorPageSource
{
    private final long numberOfRows;
    private boolean finished;

    public BigQueryEmptyProjectionPageSource(long numberOfRows)
    {
        this.numberOfRows = numberOfRows;
        this.finished = false;
    }

    @Override
    public long getCompletedBytes()
    {
        return 0;
    }

    @Override
    public long getReadTimeNanos()
    {
        return 0;
    }

    @Override
    public boolean isFinished()
    {
        return finished;
    }

    @Override
    public Page getNextPage()
    {
        PageBuilder pageBuilder = new PageBuilder(ImmutableList.of());
        for (long i = 0; i < numberOfRows; i++) {
            pageBuilder.declarePosition();
        }
        finished = true;
        return pageBuilder.build();
    }

    @Override
    public long getMemoryUsage()
    {
        return 0;
    }

    @Override
    public void close()
    {
        // nothing to do
    }
}
