package com.jbqsqle.generator;

public class FromPart implements SqlPart {
    private final String table;

    public FromPart(String table) {
        this.table = table;
    }

    @Override
    public String toSql() {
        return table;
    }
}
