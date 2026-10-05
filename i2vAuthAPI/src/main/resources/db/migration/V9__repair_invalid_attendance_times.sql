UPDATE dbo.company_settings
SET setting_value = '10:00',
    updated_at = SYSUTCDATETIME()
WHERE setting_key = 'LATE_THRESHOLD'
  AND TRY_CONVERT(time(7), NULLIF(LTRIM(RTRIM(setting_value)), '')) IS NULL;

UPDATE dbo.company_settings
SET setting_value = '09:00',
    updated_at = SYSUTCDATETIME()
WHERE setting_key = 'OFFICIAL_START_TIME'
  AND TRY_CONVERT(time(7), NULLIF(LTRIM(RTRIM(setting_value)), '')) IS NULL;

UPDATE dbo.company_settings
SET setting_value = '18:00',
    updated_at = SYSUTCDATETIME()
WHERE setting_key = 'OFFICIAL_END_TIME'
  AND TRY_CONVERT(time(7), NULLIF(LTRIM(RTRIM(setting_value)), '')) IS NULL;
