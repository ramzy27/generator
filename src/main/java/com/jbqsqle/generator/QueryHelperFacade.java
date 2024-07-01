package com.jbqsqle.generator;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class QueryHelperFacade {


    private final FilterModelCalculator filterModelCalculator;
    private final GroupByCalculator groupByCalculator;
    private final TablePrefixTransformer tablePrefixTransformer;
    private final ColumnPrefixHandler columnPrefixHandler;

    public QueryHelperFacade(
            FilterModelCalculator filterModelCalculator,
            GroupByCalculator groupByCalculator,
            TablePrefixTransformer tablePrefixTransformer,
            ColumnPrefixHandler columnPrefixHandler) {
        this.filterModelCalculator = filterModelCalculator;
        this.groupByCalculator = groupByCalculator;
        this.tablePrefixTransformer = tablePrefixTransformer;
        this.columnPrefixHandler = columnPrefixHandler;
    }



    public FilterModelCalculator getFilterModelCalculator() {
        return filterModelCalculator;
    }

    public GroupByCalculator getGroupByCalculator() {
        return groupByCalculator;
    }

    public TablePrefixTransformer getTablePrefixTransformer() {
        return tablePrefixTransformer;
    }

    public ColumnPrefixHandler getColumnPrefixHandler() {
        return columnPrefixHandler;
    }


    @Component
    class FilterModelCalculator {
        Map<String, Map<String, ColumnFilter>> calculateFilterModel(SqlServerPaginationRequest sqlRequest) {
            Map<String, Map<String, ColumnFilter>> groupedFiltersModel = new HashMap<>();

            if (sqlRequest.getFilterModel() != null) {
                groupedFiltersModel = sqlRequest.getFilterModel().entrySet().stream()
                        .map(entry -> {
                            String[] keys = entry.getKey().split("_");
                            return new AbstractMap.SimpleEntry<>(keys[0], new AbstractMap.SimpleEntry<>(keys[1], entry.getValue()));
                        })
                        .collect(Collectors.groupingBy(Map.Entry::getKey,
                                Collectors.toMap(entry -> entry.getValue().getKey(), entry -> entry.getValue().getValue())));
            }

            return groupedFiltersModel;
        }
    }

    @Component
    class GroupByCalculator {
        Set<ColumnVO> calculateGroupBy(SqlServerPaginationRequest sqlRequest, Set<ColumnVO> resultsFields) {
            Set<ColumnVO> resultRowGroups = getColumnsByTablePrefix(sqlRequest.getRowGroupCols());
            resultsFields.addAll(resultRowGroups);
            return resultRowGroups;
        }

        private Set<ColumnVO> getColumnsByTablePrefix(List<ColumnVO> valueCols) {
            return valueCols.stream()
                    .filter(field -> field.field().startsWith(WarehouseQueryBuilderV53.RESULT_TABLE_PREFIX))
                    .map(field -> {
                        String columnName = field.field();
                        if (columnName.startsWith("Result_Exposure")) {
                            columnName = columnName.substring(WarehouseQueryBuilderV53.RESULT_TABLE_PREFIX.length());
                        } else if (columnName.startsWith(WarehouseQueryBuilderV53.RESULT_TABLE_PREFIX)) {
                            String riskMeasureName = columnName.substring(WarehouseQueryBuilderV53.RESULT_TABLE_PREFIX.length());
                            try {
                                RiskMeasureName.valueOf(riskMeasureName);
                                columnName = "Result." + riskMeasureName;
                            } catch (IllegalArgumentException ex) {
                                // Not a valid RiskMeasure enum name, keep the original column name
                            }
                        }
                        return new ColumnVO(columnName.replace("_", "."), columnName.replace(".", "_"), null);
                    })
                    .collect(Collectors.toSet());
        }
    }

    @Component
    class TablePrefixTransformer {
        List<ColumnVO> transformTablePrefix(List<ColumnVO> valueCols) {
            return valueCols.stream()
                    .map(field -> {
                        String columnName = field.field();
                        if (columnName.startsWith(WarehouseQueryBuilderV53.RESULT_TABLE_PREFIX) &&
                                Stream.concat(Arrays.stream(RiskMeasureName.values()), Arrays.stream(RiskLabel.values()))
                                        .anyMatch(riskMeasureName -> riskMeasureName.name().equals(columnName.substring(WarehouseQueryBuilderV53.RESULT_TABLE_PREFIX.length())))) {
                            columnName = columnName.substring(WarehouseQueryBuilderV53.RESULT_TABLE_PREFIX.length());
                            return new ColumnVO(columnName, columnName.replace("_", "."), field.aggFunc());
                        } else {
                            return field;
                        }
                    })
                    .collect(Collectors.toList());
        }
    }

    @Component
    class ColumnPrefixHandler {
        Set<ColumnVO> getColumnsByTablePrefix(List<ColumnVO> valueCols) {
            return valueCols.stream()
                    .filter(field -> field.field().startsWith(WarehouseQueryBuilderV53.RESULT_TABLE_PREFIX))
                    .map(field -> {
                        String columnName = field.field();
                        if (columnName.startsWith("Result_Exposure")) {
                            columnName = columnName.substring(WarehouseQueryBuilderV53.RESULT_TABLE_PREFIX.length());
                        } else if (columnName.startsWith(WarehouseQueryBuilderV53.RESULT_TABLE_PREFIX)) {

                                    riskMeasureName = columnName.substring(WarehouseQueryBuilderV53.RESULT_TABLE_PREFIX.length());
                            try {
                                RiskMeasureName.valueOf(riskMeasureName);
                                columnName = "Result." + riskMeasureName;
                            } catch (IllegalArgumentException ex) {
                                // Not a valid RiskMeasure enum name, keep the original column name
                            }
                        }
                        return new ColumnVO(columnName.replace("_", "."), columnName.replace(".", "_"), null);
                    })
                    .collect(Collectors.toSet());
        }
    }
}
