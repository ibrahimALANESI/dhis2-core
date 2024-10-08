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
package org.hisp.dhis.analytics.table.setting;

import static org.hisp.dhis.commons.util.TextUtils.format;
import static org.hisp.dhis.db.model.Logged.LOGGED;
import static org.hisp.dhis.db.model.Logged.UNLOGGED;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_DATABASE;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_TABLE_INDEX_CATEGORY;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_TABLE_INDEX_CATEGORY_OPTION_GROUP_SET;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_TABLE_INDEX_DATA_ELEMENT_GROUP_SET;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_TABLE_INDEX_ORG_UNIT_GROUP_SET;
import static org.hisp.dhis.external.conf.ConfigurationKey.ANALYTICS_TABLE_UNLOGGED;
import static org.hisp.dhis.setting.SettingKey.ANALYTICS_MAX_PERIOD_YEARS_OFFSET;
import static org.hisp.dhis.util.ObjectUtils.isNull;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.table.model.Skip;
import org.hisp.dhis.db.model.Database;
import org.hisp.dhis.db.model.Logged;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.setting.SystemSettingManager;
import org.springframework.stereotype.Component;

/**
 * Component responsible for exposing analytics table settings. Provides settings living in
 * configuration files (dhis.conf) and in system settings.
 *
 * @author maikel arabori
 */
@Component
@RequiredArgsConstructor
public class AnalyticsTableSettings {
  private final DhisConfigurationProvider config;

  private final SystemSettingManager systemSettings;

  /**
   * Returns the setting indicating whether resource and analytics tables should be logged or
   * unlogged.
   *
   * @return the {@link Logged} parameter.
   */
  public Logged getTableLogged() {
    if (config.isEnabled(ANALYTICS_TABLE_UNLOGGED)) {
      return UNLOGGED;
    }

    return LOGGED;
  }

  /**
   * Returns the years' offset defined for the period generation. See {@link
   * ANALYTICS_MAX_PERIOD_YEARS_OFFSET}.
   *
   * @return the offset defined in system settings, or null if nothing is set.
   */
  public Integer getMaxPeriodYearsOffset() {
    return systemSettings.getIntSetting(ANALYTICS_MAX_PERIOD_YEARS_OFFSET) < 0
        ? null
        : systemSettings.getIntSetting(ANALYTICS_MAX_PERIOD_YEARS_OFFSET);
  }

  /**
   * Indicates whether an analytics database instance is configured.
   *
   * @return true if an analytics database instance is configured.
   */
  public boolean isAnalyticsDatabaseConfigured() {
    return config.isAnalyticsDatabaseConfigured();
  }

  /**
   * Returns the configured analytics {@link Database}. Default is {@link Database#POSTGRESQL}.
   *
   * @return the analytics {@link Database}.
   */
  public Database getAnalyticsDatabase() {
    String value = config.getProperty(ANALYTICS_DATABASE);
    String valueUpperCase = StringUtils.trimToEmpty(value).toUpperCase();
    return getAndValidateDatabase(valueUpperCase);
  }

  /**
   * Returns the {@link Database} matching the given value.
   *
   * @param value the string value.
   * @return the {@link Database}.
   * @throws IllegalArgumentException if the value does not match a valid option.
   */
  Database getAndValidateDatabase(String value) {
    Database database = EnumUtils.getEnum(Database.class, value);

    if (isNull(database)) {
      String message =
          format(
              "Property '{}' has illegal value: '{}', allowed options: {}",
              ANALYTICS_DATABASE.getKey(),
              value,
              StringUtils.join(Database.values(), ','));
      throw new IllegalArgumentException(message);
    }

    return database;
  }

  /**
   * Indicates whether to skip indexing of data element group set analytics table columns.
   *
   * @return {@link Skip#SKIP} if indexing should be skipped, {@link Skip#INCLUDE} if not.
   */
  public Skip skipIndexDataElementGroupSetColumns() {
    return toSkip(config.isEnabled(ANALYTICS_TABLE_INDEX_DATA_ELEMENT_GROUP_SET));
  }

  /**
   * Indicates whether to skip indexing of category analytics table columns.
   *
   * @return {@link Skip#SKIP} if indexing should be skipped, {@link Skip#INCLUDE} if not.
   */
  public Skip skipIndexCategoryColumns() {
    return toSkip(config.isEnabled(ANALYTICS_TABLE_INDEX_CATEGORY));
  }

  /**
   * Indicates whether to skip indexing of category option group set analytics table columns.
   *
   * @return {@link Skip#SKIP} if indexing should be skipped, {@link Skip#INCLUDE} if not.
   */
  public Skip skipIndexCategoryOptionGroupSetColumns() {
    return toSkip(config.isEnabled(ANALYTICS_TABLE_INDEX_CATEGORY_OPTION_GROUP_SET));
  }

  /**
   * Indicates whether to skip indexing of organisation unit group set analytics table columns.
   *
   * @return {@link Skip#SKIP} if indexing should be skipped, {@link Skip#INCLUDE} if not.
   */
  public Skip skipIndexOrgUnitGroupSetColumns() {
    return toSkip(config.isEnabled(ANALYTICS_TABLE_INDEX_ORG_UNIT_GROUP_SET));
  }

  /**
   * Converts the boolean enabled flag to a {@link Skip} value.
   *
   * @param enabled the boolean enabled flag.
   * @return a {@link Skip} value.
   */
  Skip toSkip(boolean enabled) {
    return enabled ? Skip.INCLUDE : Skip.SKIP;
  }
}
