package com.jbqsqle.generator;

public class GroupByPart implements SqlPart {
    private final String column;

    public GroupByPart(String column) {
        this.column = column;
    }

    @Override
    public String toSql() {
        return column;
    }
}
