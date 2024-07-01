package com.jbqsqle.generator;



import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Slf4j
public class WarehouseQueryBuilderV53 {
    public static final String RESULT_TABLE_PREFIX = "Result_";

    private final BigQuerySqlQueryConfiguration bigQuerySqlQueryConfiguration;
    private final QueryHelperFacade queryHelperFacade;
    private final GenericQueryBuilder genericQueryBuilder;

    public WarehouseQueryBuilderV53(
            BigQuerySqlQueryConfiguration bigQuerySqlQueryConfiguration,
            QueryHelperFacade queryHelperFacade,
            GenericQueryBuilder genericQueryBuilder) {
        this.bigQuerySqlQueryConfiguration = bigQuerySqlQueryConfiguration;
        this.queryHelperFacade = queryHelperFacade;
        this.genericQueryBuilder = genericQueryBuilder;
    }

    public String generateSql(SqlServerPaginationRequest sqlRequest, String resultsTableName) {
        boolean pivotingEnabled = !Strings.EMPTY.equals(sqlRequest.getPivotColumn());
        Set<ColumnVO> resultsFields = queryHelperFacade.getColumnPrefixHandler().getColumnsByTablePrefix(sqlRequest.getValueCols());

        prepareMeasuresPivoting(sqlRequest, resultsFields, pivotingEnabled);

        String pivotEnrichment = "";
        if (pivotingEnabled) {
            List<String> pivotColumnValues = extractPivotColumnValues(resultsFields);
            pivotEnrichment = "PIVOT (SUM(Value) FOR " + sqlRequest.getPivotColumn() + " IN (" +
                    pivotColumnValues.stream().map(v -> "'" + v + "'").collect(Collectors.joining(", ")) + "))";
        }

        // Prepare unnest columns
        List<String> unnestColumns = Arrays.asList("Exposure");

        // Generate the SQL using GenericQueryBuilder
        String resultQuery = genericQueryBuilder.generateSql(sqlRequest, resultsTableName, unnestColumns, pivotEnrichment);
        log.info("Generated SQL: {}", resultQuery);
        return resultQuery;
    }

    private void prepareMeasuresPivoting(SqlServerPaginationRequest sqlRequest, Set<ColumnVO> resultsFields, boolean pivotingEnabled) {
        if (pivotingEnabled) {
            resultsFields.removeIf(columnVO -> Stream.concat(Arrays.stream(RiskMeasureName.values()), Arrays.stream(RiskLabel.values()))
                    .anyMatch(riskMeasureName -> columnVO.field().contains(riskMeasureName.name())));

            resultsFields.add(new ColumnVO("Value", null));
            resultsFields.add(new ColumnVO("Measure", null));
        }
        sqlRequest.setValueCols(transformTablePrefix(sqlRequest.getValueCols()));
    }

    private List<String> transformTablePrefix(List<ColumnVO> valueCols) {
        return valueCols.stream()
                .map(field -> {
                    String columnName = field.field();
                    if (columnName.startsWith(RESULT_TABLE_PREFIX) &&
                            Stream.concat(Arrays.stream(RiskMeasureName.values()), Arrays.stream(RiskLabel.values()))
                                    .anyMatch(riskMeasureName -> riskMeasureName.name().equals(columnName.substring(RESULT_TABLE_PREFIX.length())))) {
                        columnName = columnName.substring(RESULT_TABLE_PREFIX.length());
                        return new ColumnVO(columnName, columnName.replace("_", "."), field.aggFunc());
                    } else {
                        return field;
                    }
                })
                .collect(Collectors.toList());
    }

    private List<String> extractPivotColumnValues(Set<ColumnVO> resultsFields) {
        return resultsFields.stream()
                .map(columnVO -> columnVO.field().substring(RESULT_TABLE_PREFIX.length()))
                .filter(s -> Arrays.stream(RiskMeasureName.values()).anyMatch(measureName -> measureName.name().equals(s)))
                .collect(Collectors.toList());
    }
}
