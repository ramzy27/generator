package com.jbqsqle.generator;

public class WherePart implements SqlPart {
    private final String condition;

    public WherePart(String condition) {
        this.condition = condition;
    }

    @Override
    public String toSql() {
        return condition;
    }
}
