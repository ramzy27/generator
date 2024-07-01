package com.jbqsqle.generator;

public class SelectPart implements SqlPart {
    private final String column;

    public SelectPart(String column) {
        this.column = column;
    }

    @Override
    public String toSql() {
        return column;
    }
}
