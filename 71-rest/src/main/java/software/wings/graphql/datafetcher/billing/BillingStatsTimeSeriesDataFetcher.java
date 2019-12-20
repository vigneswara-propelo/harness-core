package software.wings.graphql.datafetcher.billing;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcherWithAggregationList;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;
import software.wings.graphql.schema.type.aggregation.QLBillingDataPoint;
import software.wings.graphql.schema.type.aggregation.QLBillingStackedTimeSeriesData;
import software.wings.graphql.schema.type.aggregation.QLBillingStackedTimeSeriesData.QLBillingStackedTimeSeriesDataBuilder;
import software.wings.graphql.schema.type.aggregation.QLBillingStackedTimeSeriesDataPoint;
import software.wings.graphql.schema.type.aggregation.QLBillingStackedTimeSeriesDataPoint.QLBillingStackedTimeSeriesDataPointBuilder;
import software.wings.graphql.schema.type.aggregation.QLBillingTimeDataPoint;
import software.wings.graphql.schema.type.aggregation.QLBillingTimeDataPoint.QLBillingTimeDataPointBuilder;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLReference;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMEntityGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMGroupBy;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@Slf4j
public class BillingStatsTimeSeriesDataFetcher
    extends AbstractStatsDataFetcherWithAggregationList<QLCCMAggregationFunction, QLBillingDataFilter, QLCCMGroupBy,
        QLBillingSortCriteria> {
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject QLBillingStatsHelper statsHelper;
  @Inject BillingDataQueryBuilder billingDataQueryBuilder;

  private static final long ONE_DAY_MILLIS = 86400000;

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, List<QLCCMAggregationFunction> aggregateFunction,
      List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy, List<QLBillingSortCriteria> sortCriteria) {
    if (!timeScaleDBService.isValid()) {
      throw new InvalidRequestException("Cannot process request in BillingStatsTimeSeriesDataFetcher");
    }
    try {
      return getData(accountId, filters, aggregateFunction, groupBy, sortCriteria);
    } catch (Exception e) {
      throw new InvalidRequestException("Error while fetching billing data {}", e);
    }
  }

  @Override
  protected QLData postFetch(String accountId, List<QLCCMGroupBy> groupByList, QLData qlData) {
    return null;
  }

  protected QLData getData(@NotNull String accountId, List<QLBillingDataFilter> filters,
      List<QLCCMAggregationFunction> aggregateFunction, List<QLCCMGroupBy> groupByList,
      List<QLBillingSortCriteria> sortCriteria) {
    BillingDataQueryMetadata queryData;
    ResultSet resultSet = null;
    List<QLCCMEntityGroupBy> groupByEntityList = billingDataQueryBuilder.getGroupByEntity(groupByList);
    if (filters == null) {
      filters = new ArrayList<>();
    }

    queryData =
        billingDataQueryBuilder.formQuery(accountId, filters, aggregateFunction, groupByEntityList, sortCriteria);
    logger.info("BillingStatsTimeSeriesDataFetcher query: {}", queryData.getQuery());
    logger.info(queryData.getQuery());

    try (Connection connection = timeScaleDBService.getDBConnection();
         Statement statement = connection.createStatement()) {
      resultSet = statement.executeQuery(queryData.getQuery());

      return generateStackedTimeSeriesData(queryData, resultSet, getMinStartTimeFromFilters(filters));
    } catch (SQLException e) {
      logger.error("BillingStatsTimeSeriesDataFetcher Error exception {}", e);
    } finally {
      DBUtils.close(resultSet);
    }
    return null;
  }

  protected QLBillingStackedTimeSeriesData generateStackedTimeSeriesData(
      BillingDataQueryMetadata queryData, ResultSet resultSet, long startTimeFromFilters) throws SQLException {
    Map<Long, List<QLBillingTimeDataPoint>> qlTimeDataPointMap = new LinkedHashMap<>();
    Map<Long, List<QLBillingTimeDataPoint>> qlTimeCpuPointMap = new LinkedHashMap<>();
    Map<Long, List<QLBillingTimeDataPoint>> qlTimeMemoryPointMap = new LinkedHashMap<>();
    Map<Long, List<QLBillingTimeDataPoint>> qlTimeMemoryUtilsPointMap = new LinkedHashMap<>();
    Map<Long, List<QLBillingTimeDataPoint>> qlTimeCpuUtilsPointMap = new LinkedHashMap<>();

    checkAndAddPrecedingZeroValuedData(queryData, resultSet, startTimeFromFilters, qlTimeDataPointMap);

    do {
      QLBillingTimeDataPointBuilder dataPointBuilder = QLBillingTimeDataPoint.builder();
      // For First Level Idle Cost Drill Down
      QLBillingTimeDataPointBuilder cpuPointBuilder = QLBillingTimeDataPoint.builder();
      QLBillingTimeDataPointBuilder memoryPointBuilder = QLBillingTimeDataPoint.builder();
      // For Leaf level Idle cost Drill Down
      QLBillingTimeDataPointBuilder memoryUtilsPointBuilder = QLBillingTimeDataPoint.builder();
      QLBillingTimeDataPointBuilder cpuUtilsPointBuilder = QLBillingTimeDataPoint.builder();

      for (BillingDataMetaDataFields field : queryData.getFieldNames()) {
        switch (field.getDataType()) {
          case DOUBLE:
            switch (field) {
              case CPUIDLECOST:
                cpuPointBuilder.value(roundingDoubleFieldValue(field, resultSet));
                break;
              case MEMORYIDLECOST:
                memoryPointBuilder.value(roundingDoubleFieldValue(field, resultSet));
                break;
              case MAXCPUUTILIZATION:
                cpuUtilsPointBuilder.max(roundingDoubleFieldValue(field, resultSet));
                break;
              case AVGCPUUTILIZATION:
                cpuUtilsPointBuilder.avg(roundingDoubleFieldValue(field, resultSet));
                break;
              case MAXMEMORYUTILIZATION:
                memoryUtilsPointBuilder.max(roundingDoubleFieldValue(field, resultSet));
                break;
              case AVGMEMORYUTILIZATION:
                memoryUtilsPointBuilder.avg(roundingDoubleFieldValue(field, resultSet));
                break;
              default:
                dataPointBuilder.value(roundingDoubleFieldValue(field, resultSet));
            }
            break;
          case STRING:
            final String entityId = resultSet.getString(field.getFieldName());
            cpuPointBuilder.key(buildQLReference(field, entityId));
            memoryPointBuilder.key(buildQLReference(field, entityId));
            dataPointBuilder.key(buildQLReference(field, entityId));
            cpuUtilsPointBuilder.key(buildQLReference(field, entityId));
            memoryUtilsPointBuilder.key(buildQLReference(field, entityId));

            break;
          case TIMESTAMP:
            long time = resultSet.getTimestamp(field.getFieldName(), utils.getDefaultCalendar()).getTime();
            cpuPointBuilder.time(time);
            memoryPointBuilder.time(time);
            dataPointBuilder.time(time);
            cpuUtilsPointBuilder.time(time);
            memoryUtilsPointBuilder.time(time);
            break;
          default:
            throw new InvalidRequestException("UnsupportedType " + field.getDataType());
        }
      }

      checkDataPointIsValidAndInsert(dataPointBuilder.build(), qlTimeDataPointMap);
      checkDataPointIsValidAndInsert(cpuPointBuilder.build(), qlTimeCpuPointMap);
      checkDataPointIsValidAndInsert(memoryPointBuilder.build(), qlTimeMemoryPointMap);
      checkDataPointIsValidAndInsert(cpuUtilsPointBuilder.build(), qlTimeCpuUtilsPointMap);
      checkDataPointIsValidAndInsert(memoryUtilsPointBuilder.build(), qlTimeMemoryUtilsPointMap);
    } while (resultSet != null && resultSet.next());

    QLBillingStackedTimeSeriesDataBuilder timeSeriesDataBuilder = QLBillingStackedTimeSeriesData.builder();

    return timeSeriesDataBuilder.data(prepareStackedTimeSeriesData(queryData, qlTimeDataPointMap))
        .cpuIdleCost(prepareStackedTimeSeriesData(queryData, qlTimeCpuPointMap))
        .memoryIdleCost(prepareStackedTimeSeriesData(queryData, qlTimeMemoryPointMap))
        .cpuUtilMetrics(prepareStackedTimeSeriesData(queryData, qlTimeCpuUtilsPointMap))
        .memoryUtilMetrics(prepareStackedTimeSeriesData(queryData, qlTimeMemoryUtilsPointMap))
        .build();
  }

  private void checkDataPointIsValidAndInsert(
      QLBillingTimeDataPoint dataPoint, Map<Long, List<QLBillingTimeDataPoint>> qlTimeDataPointMap) {
    if (dataPoint.getAvg() != null || dataPoint.getMax() != null || dataPoint.getValue() != null) {
      qlTimeDataPointMap.computeIfAbsent(dataPoint.getTime(), k -> new ArrayList<>());
      qlTimeDataPointMap.get(dataPoint.getTime()).add(dataPoint);
    }
  }

  private long getMinStartTimeFromFilters(List<QLBillingDataFilter> filters) {
    long minStartTime = Long.MAX_VALUE;
    for (QLBillingDataFilter filter : filters) {
      if (filter.getStartTime() != null) {
        minStartTime = Math.min(filter.getStartTime().getValue().longValue(), minStartTime);
      }
    }
    return minStartTime;
  }

  private boolean checkStartTimeFilterIsValid(long startTime) {
    return startTime != Long.MAX_VALUE;
  }

  private double roundingDoubleFieldValue(BillingDataMetaDataFields field, ResultSet resultSet) throws SQLException {
    return Math.round(resultSet.getDouble(field.getFieldName()) * 100D) / 100D;
  }

  private void checkAndAddPrecedingZeroValuedData(BillingDataQueryMetadata queryData, ResultSet resultSet,
      long startTimeFromFilters, Map<Long, List<QLBillingTimeDataPoint>> qlTimeDataPointMap) throws SQLException {
    if (resultSet != null && resultSet.next()) {
      String id = "";
      for (BillingDataMetaDataFields field : queryData.getFieldNames()) {
        if (field.getFieldName().contains("ID")) {
          id = resultSet.getString(field.getFieldName());
        }
      }
      if (checkStartTimeFilterIsValid(startTimeFromFilters)) {
        long timeOfFirstEntry =
            resultSet.getTimestamp(BillingDataMetaDataFields.STARTTIME.getFieldName(), utils.getDefaultCalendar())
                .getTime();
        AddPrecedingZeroValuedData(queryData, qlTimeDataPointMap, id, timeOfFirstEntry, startTimeFromFilters);
      }
    }
  }

  private void AddPrecedingZeroValuedData(BillingDataQueryMetadata queryData,
      Map<Long, List<QLBillingTimeDataPoint>> qlTimeDataPointMap, String id, long timeOfFirstEntry,
      long startTimeFromFilters) {
    int missingDays = (int) ((timeOfFirstEntry - startTimeFromFilters) / ONE_DAY_MILLIS);
    long startTime = timeOfFirstEntry - missingDays * ONE_DAY_MILLIS;
    while (timeOfFirstEntry > startTime) {
      QLBillingTimeDataPointBuilder dataPointBuilder = QLBillingTimeDataPoint.builder();
      for (BillingDataMetaDataFields field : queryData.getFieldNames()) {
        switch (field.getDataType()) {
          case DOUBLE:
            dataPointBuilder.value(0);
            break;
          case STRING:
            final String entityId = id;
            dataPointBuilder.key(buildQLReference(field, entityId));
            break;
          case TIMESTAMP:
            dataPointBuilder.time(startTime);
            break;
          default:
            throw new InvalidRequestException("UnsupportedType " + field.getDataType());
        }
      }
      QLBillingTimeDataPoint dataPoint = dataPointBuilder.build();
      qlTimeDataPointMap.computeIfAbsent(dataPoint.getTime(), k -> new ArrayList<>());
      qlTimeDataPointMap.get(dataPoint.getTime()).add(dataPoint);
      startTime += ONE_DAY_MILLIS;
    }
  }

  private QLReference buildQLReference(BillingDataMetaDataFields field, String key) {
    return QLReference.builder().type(field.getFieldName()).id(key).name(statsHelper.getEntityName(field, key)).build();
  }

  private List<QLBillingStackedTimeSeriesDataPoint> prepareStackedTimeSeriesData(
      BillingDataQueryMetadata queryData, Map<Long, List<QLBillingTimeDataPoint>> qlTimeDataPointMap) {
    List<QLBillingStackedTimeSeriesDataPoint> timeSeriesDataPoints = new ArrayList<>();

    qlTimeDataPointMap.keySet().forEach(time -> {
      List<QLBillingTimeDataPoint> timeDataPoints = qlTimeDataPointMap.get(time);
      QLBillingStackedTimeSeriesDataPointBuilder builder = QLBillingStackedTimeSeriesDataPoint.builder();
      List<QLBillingDataPoint> dataPoints =
          timeDataPoints.stream().map(QLBillingTimeDataPoint::getQLBillingDataPoint).collect(Collectors.toList());
      if (queryData.getGroupByFields() != null) {
        dataPoints = filterQLDataPoints(dataPoints, queryData.getFilters(), queryData.getGroupByFields().get(0));
      }
      builder.values(dataPoints).time(time);
      timeSeriesDataPoints.add(builder.build());
    });
    return timeSeriesDataPoints;
  }

  private List<QLBillingDataPoint> filterQLDataPoints(
      List<QLBillingDataPoint> dataPoints, List<QLBillingDataFilter> filters, BillingDataMetaDataFields groupBy) {
    if (groupBy != null) {
      Map<BillingDataMetaDataFields, String[]> filterValueMap = getFilterDeploymentMetaDataField(filters);
      String[] values = filterValueMap.get(groupBy);
      if (values != null) {
        final Set valueSet = Sets.newHashSet(values);
        dataPoints.removeIf(dataPoint -> !valueSet.contains(dataPoint.getKey().getId()));
      }
    }
    return dataPoints;
  }

  private Map<BillingDataMetaDataFields, String[]> getFilterDeploymentMetaDataField(List<QLBillingDataFilter> filters) {
    Map<BillingDataMetaDataFields, String[]> filterMap = new EnumMap<>(BillingDataMetaDataFields.class);
    for (QLBillingDataFilter filter : filters) {
      if (filter.getApplication() != null) {
        filterMap.put(BillingDataMetaDataFields.APPID, filter.getApplication().getValues());
      }
    }
    return filterMap;
  }

  @Override
  public String getEntityType() {
    return null;
  }
}
