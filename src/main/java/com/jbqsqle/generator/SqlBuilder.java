package com.jbqsqle.generator;

public interface SqlBuilder {
    SqlBuilder select(SqlPart part);

    SqlBuilder from(SqlPart part);

    SqlBuilder where(SqlPart part);

    SqlBuilder groupBy(SqlPart part);

    SqlBuilder having(SqlPart part);

    String build();
}
