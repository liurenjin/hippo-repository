/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.lock.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.repository.lock.db.DbHelper.close;
import static org.onehippo.repository.lock.db.DbLockManager.REFRESH_LOCK_STATEMENT;

/**
 * Refreshes all locks that are in possession by <strong>this</strong> cluster node and have less than 20 seconds to live. Note the 20
 * seconds is a heuristic number: The {@link DbLockRefresher} runs about every 5 seconds. If some hiccup or other Threads
 * causes some delay, 20 seconds should still be more than enough.
 */
public class DbLockRefresher implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(DbLockRefresher.class);

    private final DataSource dataSource;
    private final String clusterNodeId;

    public DbLockRefresher(final DataSource dataSource, final String clusterNodeId) {
        this.dataSource = dataSource;
        this.clusterNodeId = clusterNodeId;
    }

    @Override
    public void run() {
        Connection connection = null;
        boolean originalAutoCommit = false;
        try {
            connection = dataSource.getConnection();
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(true);
            final PreparedStatement refreshStatement = connection.prepareStatement(REFRESH_LOCK_STATEMENT);
            refreshStatement.setString(1, clusterNodeId);
            // select all rows that have less than 20 seconds to live
            refreshStatement.setLong(2, System.currentTimeMillis() + 20000);
            int updated = refreshStatement.executeUpdate();
            log.info("Refreshed {} locks", updated);
            refreshStatement.close();
        } catch (SQLException e) {
            if (log.isDebugEnabled()) {
                log.info("Exception in {} happened. Possibly another cluster node did already reset some lock rows:", this.getClass().getName(), e);
            } else {
                log.info("Exception in {} happened.  Possibly another cluster node did already reset some lock rows: {}", this.getClass().getName(), e.toString());
            }
        } finally {
            close(connection, originalAutoCommit);
        }
    }
}
