package com.jbqsqle.generator;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class GenericQueryBuilder extends AbstractSqlBuilder {
    private List<String> unnestColumns = new ArrayList<>();
    private String pivotEnrichment;
    private final QueryHelperFacade queryHelperFacade;

    public GenericQueryBuilder(QueryHelperFacade queryHelperFacade) {
        this.queryHelperFacade = queryHelperFacade;
    }


    public GenericQueryBuilder unnest(List<String> columns) {
        unnestColumns.addAll(columns);
        return this;
    }

    public GenericQueryBuilder pivot(String pivotEnrichment) {
        this.pivotEnrichment = pivotEnrichment;
        return this;
    }

    public String generateSql(SqlServerPaginationRequest sqlRequest, String resultsTableName, List<String> unnestColumns, String pivotEnrichment) {
        // Setup the query with provided unnest columns and pivot enrichment
        this.unnest(unnestColumns);
        this.pivot(pivotEnrichment);

        select(new SelectPart("Result.*"))
                .from(new FromPart(resultsTableName));
            Set<ColumnVO> allGroups = queryHelperFacade.getGroupByCalculator().calculateGroupBy(sqlRequest, resultsFields);
            Map<String, Map<String, ColumnFilter>> groupedFiltersModel = queryHelperFacade.getFilterModelCalculator().calculateFilterModel(sqlRequest);

            select(new SelectPart("Result.*"))
                    .from(new FromPart(resultsTableName));

            groupedFiltersModel.forEach((key, value) -> value.forEach((k, v) -> {
                where(new WherePart(k + " = '" + v.getValue() + "'"));
            }));

            allGroups.forEach(group -> groupBy(new GroupByPart(group.field())));


        return build();
    }

    @Override
    public String build() {
        String sql = super.build();
        if (!unnestColumns.isEmpty()) {
            for (String column : unnestColumns) {
                sql = sql.replace(column, "UNNEST(" + column + ")");
            }
        }
        if (pivotEnrichment != null && !pivotEnrichment.isEmpty()) {
            sql += " " + pivotEnrichment;
        }
        return sql;
    }
}

