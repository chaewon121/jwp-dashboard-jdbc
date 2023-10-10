package org.springframework.jdbc.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class JdbcTemplate {

    private static final Logger log = LoggerFactory.getLogger(JdbcTemplate.class);

    private final DataSource dataSource;
    private final StatementExecutor statementExecutor;

    public JdbcTemplate(final DataSource dataSource) {
        this(dataSource, new StatementExecutor());
    }

    public JdbcTemplate(final DataSource dataSource,
                        final StatementExecutor statementExecutor) {
        this.dataSource = dataSource;
        this.statementExecutor = statementExecutor;
    }

    public void update(final String sql,
                       final Object... parameters) {
        query(sql, PreparedStatement::executeUpdate, parameters);
    }

    public <T> Optional<T> queryForObject(final String sql,
                                          final ResultSetMapper<T> rowMapper,
                                          final Object... parameters) {
        final List<T> results = queryForList(sql, rowMapper, parameters);
        if (results.size() > 1) {
            throw new DataAccessException("2개 이상의 결과를 반환할 수 없습니다.");
        }
        if (results.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(results.iterator().next());
    }

    public <T> List<T> queryForList(final String sql,
                                    final ResultSetMapper<T> rowMapper,
                                    final Object... parameters) {
        return query(sql, statement -> statementExecutor.execute(statement, rowMapper), parameters);
    }

    private <T> T query(final String sql,
                        final PreparedStatementCallback<T> preparedStatementCallback,
                        final Object... parameters) {
        final Connection connection = DataSourceUtils.getConnection(dataSource);
        try (final PreparedStatement preparedStatement = createStatement(connection, sql, parameters)) {
            return preparedStatementCallback.execute(preparedStatement);
        } catch (final SQLException e) {
            log.error(e.getMessage(), e);
            throw new DataAccessException(e);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private PreparedStatement createStatement(final Connection connection,
                                              final String sql,
                                              final Object[] parameters) throws SQLException {
        final PreparedStatement preparedStatement = connection.prepareStatement(sql);
        for (int i = 0; i < parameters.length; i++) {
            preparedStatement.setObject(i + 1, parameters[i]);
        }
        return preparedStatement;
    }
}

