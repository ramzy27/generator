package com.jbqsqle.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class AbstractSqlBuilder implements SqlBuilder {
    protected List<SqlPart> selectParts = new ArrayList<>();
    protected List<SqlPart> fromParts = new ArrayList<>();
    protected List<SqlPart> whereParts = new ArrayList<>();
    protected List<SqlPart> groupByParts = new ArrayList<>();
    protected List<SqlPart> havingParts = new ArrayList<>();

    @Override
    public SqlBuilder select(SqlPart part) {
        selectParts.add(part);
        return this;
    }

    @Override
    public SqlBuilder from(SqlPart part) {
        fromParts.add(part);
        return this;
    }

    @Override
    public SqlBuilder where(SqlPart part) {
        whereParts.add(part);
        return this;
    }

    @Override
    public SqlBuilder groupBy(SqlPart part) {
        groupByParts.add(part);
        return this;
    }

    @Override
    public SqlBuilder having(SqlPart part) {
        havingParts.add(part);
        return this;
    }

    @Override
    public String build() {
        return String.join(" ",
                buildPart(selectParts, "SELECT"),
                buildPart(fromParts, "FROM"),
                buildPart(whereParts, "WHERE"),
                buildPart(groupByParts, "GROUP BY"),
                buildPart(havingParts, "HAVING")
        );
    }

    private String buildPart(List<SqlPart> parts, String clause) {
        return parts.isEmpty() ? "" : clause + " " + parts.stream().map(SqlPart::toSql).collect(Collectors.joining(", "));
    }
}
