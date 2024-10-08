/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.analytics.table;

import static java.lang.String.join;
import static java.lang.String.valueOf;
import static org.hisp.dhis.analytics.AnalyticsTableType.TRACKED_ENTITY_INSTANCE_ENROLLMENTS;
import static org.hisp.dhis.analytics.table.JdbcEventAnalyticsTableManager.EXPORTABLE_EVENT_STATUSES;
import static org.hisp.dhis.commons.util.TextUtils.removeLastComma;
import static org.hisp.dhis.commons.util.TextUtils.replace;
import static org.hisp.dhis.db.model.DataType.CHARACTER_11;
import static org.hisp.dhis.db.model.DataType.CHARACTER_32;
import static org.hisp.dhis.db.model.DataType.DOUBLE;
import static org.hisp.dhis.db.model.DataType.GEOMETRY;
import static org.hisp.dhis.db.model.DataType.INTEGER;
import static org.hisp.dhis.db.model.DataType.TIMESTAMP;
import static org.hisp.dhis.db.model.DataType.VARCHAR_255;
import static org.hisp.dhis.db.model.DataType.VARCHAR_50;
import static org.hisp.dhis.db.model.constraint.Nullable.NOT_NULL;
import static org.hisp.dhis.db.model.constraint.Nullable.NULL;
import static org.hisp.dhis.util.DateUtils.toLongDate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hisp.dhis.analytics.AnalyticsTableHookService;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.AnalyticsTableUpdateParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.analytics.table.model.AnalyticsTable;
import org.hisp.dhis.analytics.table.model.AnalyticsTableColumn;
import org.hisp.dhis.analytics.table.model.AnalyticsTablePartition;
import org.hisp.dhis.analytics.table.setting.AnalyticsTableSettings;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataapproval.DataApprovalLevelService;
import org.hisp.dhis.db.model.IndexType;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.resourcetable.ResourceTableService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.database.DatabaseInfoProvider;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("org.hisp.dhis.analytics.TeiEnrollmentsAnalyticsTableManager")
public class JdbcTeiEnrollmentsAnalyticsTableManager extends AbstractJdbcTableManager {
  private static final List<AnalyticsTableColumn> FIXED_COLS =
      List.of(
          AnalyticsTableColumn.builder()
              .build()
              .withName("trackedentityinstanceuid")
              .withDataType(CHARACTER_11)
              .withNullable(NOT_NULL)
              .withSelectExpression("tei.uid"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("programuid")
              .withDataType(CHARACTER_11)
              .withNullable(NULL)
              .withSelectExpression("p.uid"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("programinstanceuid")
              .withDataType(CHARACTER_11)
              .withNullable(NULL)
              .withSelectExpression("pi.uid"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("enrollmentdate")
              .withDataType(TIMESTAMP)
              .withSelectExpression("pi.enrollmentdate"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("enddate")
              .withDataType(TIMESTAMP)
              .withSelectExpression("pi.completeddate"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("incidentdate")
              .withDataType(TIMESTAMP)
              .withSelectExpression("pi.occurreddate"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("enrollmentstatus")
              .withDataType(VARCHAR_50)
              .withSelectExpression("pi.status"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("pigeometry")
              .withDataType(GEOMETRY)
              .withSelectExpression("pi.geometry")
              .withIndexType(IndexType.GIST),
          AnalyticsTableColumn.builder()
              .build()
              .withName("pilongitude")
              .withDataType(DOUBLE)
              .withSelectExpression(
                  "case when 'POINT' = GeometryType(pi.geometry) then ST_X(pi.geometry) end"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("pilatitude")
              .withDataType(DOUBLE)
              .withSelectExpression(
                  "case when 'POINT' = GeometryType(pi.geometry) then ST_Y(pi.geometry) end"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("uidlevel1")
              .withDataType(CHARACTER_11)
              .withNullable(NULL)
              .withSelectExpression("ous.uidlevel1"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("uidlevel2")
              .withDataType(CHARACTER_11)
              .withNullable(NULL)
              .withSelectExpression("ous.uidlevel2"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("uidlevel3")
              .withDataType(CHARACTER_11)
              .withNullable(NULL)
              .withSelectExpression("ous.uidlevel3"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("uidlevel4")
              .withDataType(CHARACTER_11)
              .withNullable(NULL)
              .withSelectExpression("ous.uidlevel4"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("ou")
              .withDataType(CHARACTER_11)
              .withNullable(NULL)
              .withSelectExpression("ou.uid"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("ouname")
              .withDataType(VARCHAR_255)
              .withNullable(NULL)
              .withSelectExpression("ou.name"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("oucode")
              .withDataType(CHARACTER_32)
              .withNullable(NULL)
              .withSelectExpression("ou.code"),
          AnalyticsTableColumn.builder()
              .build()
              .withName("oulevel")
              .withDataType(INTEGER)
              .withNullable(NULL)
              .withSelectExpression("ous.level"));

  private final TrackedEntityTypeService trackedEntityTypeService;

  public JdbcTeiEnrollmentsAnalyticsTableManager(
      IdentifiableObjectManager idObjectManager,
      OrganisationUnitService organisationUnitService,
      CategoryService categoryService,
      SystemSettingManager systemSettingManager,
      DataApprovalLevelService dataApprovalLevelService,
      ResourceTableService resourceTableService,
      AnalyticsTableHookService tableHookService,
      PartitionManager partitionManager,
      DatabaseInfoProvider databaseInfoProvider,
      @Qualifier("analyticsJdbcTemplate") JdbcTemplate jdbcTemplate,
      TrackedEntityTypeService trackedEntityTypeService,
      AnalyticsTableSettings analyticsTableSettings,
      PeriodDataProvider periodDataProvider,
      SqlBuilder sqlBuilder) {
    super(
        idObjectManager,
        organisationUnitService,
        categoryService,
        systemSettingManager,
        dataApprovalLevelService,
        resourceTableService,
        tableHookService,
        partitionManager,
        databaseInfoProvider,
        jdbcTemplate,
        analyticsTableSettings,
        periodDataProvider,
        sqlBuilder);
    this.trackedEntityTypeService = trackedEntityTypeService;
  }

  /**
   * Returns the {@link AnalyticsTableType} of analytics table which this manager handles.
   *
   * @return type of analytics table.
   */
  @Override
  public AnalyticsTableType getAnalyticsTableType() {
    return TRACKED_ENTITY_INSTANCE_ENROLLMENTS;
  }

  /**
   * Returns a {@link AnalyticsTable} with a list of yearly {@link AnalyticsTablePartition}.
   *
   * @param params the {@link AnalyticsTableUpdateParams}.
   * @return the analytics table with partitions.
   */
  @Override
  @Transactional
  public List<AnalyticsTable> getAnalyticsTables(AnalyticsTableUpdateParams params) {
    Logged logged = analyticsTableSettings.getTableLogged();
    return trackedEntityTypeService.getAllTrackedEntityType().stream()
        .map(tet -> new AnalyticsTable(getAnalyticsTableType(), getColumns(), logged, tet))
        .collect(Collectors.toList());
  }

  private List<AnalyticsTableColumn> getColumns() {
    List<AnalyticsTableColumn> columns = new ArrayList<>();
    columns.addAll(FIXED_COLS);
    columns.add(getOrganisationUnitNameHierarchyColumn());

    return columns;
  }

  @Override
  protected List<String> getPartitionChecks(Integer year, Date endDate) {
    return List.of();
  }

  /**
   * Populates the given analytics table.
   *
   * @param params the {@link AnalyticsTableUpdateParams}.
   * @param partition the {@link AnalyticsTablePartition} to populate.
   */
  @Override
  public void populateTable(AnalyticsTableUpdateParams params, AnalyticsTablePartition partition) {
    String tableName = partition.getName();

    List<AnalyticsTableColumn> columns = partition.getMasterTable().getAnalyticsTableColumns();

    StringBuilder sql = new StringBuilder("insert into " + tableName + " (");

    for (AnalyticsTableColumn col : columns) {
      sql.append(quote(col.getName()) + ",");
    }

    removeLastComma(sql).append(") select ");

    for (AnalyticsTableColumn col : columns) {
      sql.append(col.getSelectExpression() + ",");
    }

    removeLastComma(sql)
        .append(
            replace(
                """
                \sfrom enrollment pi \
                inner join trackedentity tei on pi.trackedentityid = tei.trackedentityid \
                and tei.deleted = false \
                and tei.trackedentitytypeid =${teiId} \
                and tei.lastupdated < '${startTime}' \
                left join program p on p.programid = pi.programid \
                left join organisationunit ou on pi.organisationunitid = ou.organisationunitid \
                left join analytics_rs_orgunitstructure ous on ous.organisationunitid = ou.organisationunitid \
                where exists ( select 1 from event psi where psi.deleted = false \
                and psi.enrollmentid = pi.enrollmentid \
                and psi.status in (${statuses})) \
                and pi.occurreddate is not null \
                and pi.deleted = false\s""",
                Map.of(
                    "teiId", valueOf(partition.getMasterTable().getTrackedEntityType().getId()),
                    "startTime", toLongDate(params.getStartTime()),
                    "statuses", join(",", EXPORTABLE_EVENT_STATUSES))));

    invokeTimeAndLog(sql.toString(), "Populating table: '{}'", tableName);
  }
}
