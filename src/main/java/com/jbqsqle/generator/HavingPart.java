package com.jbqsqle.generator;

public class HavingPart implements SqlPart {
    private final String condition;

    public HavingPart(String condition) {
        this.condition = condition;
    }

    @Override
    public String toSql() {
        return condition;
    }
}
