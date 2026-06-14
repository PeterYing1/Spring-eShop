DECLARE @now datetime2 = SYSUTCDATETIME();

MERGE dbo.Campaign AS target
USING (VALUES
    (1, N'.NET Bot Black Hoodie 50% OFF', N'Campaign Description 1', @now, DATEADD(day, 7, @now), N'http://externalcatalogbaseurltobereplaced/api/v1/campaigns/1/pic', N'1.png', NULL),
    (2, N'Roslyn Red T-Shirt 3x2', N'Campaign Description 2', DATEADD(day, -7, @now), DATEADD(day, 14, @now), N'http://externalcatalogbaseurltobereplaced/api/v1/campaigns/2/pic', N'2.png', NULL)
) AS source (Id, Name, Description, [From], [To], PictureUri, PictureName, DetailsUri)
ON target.Id = source.Id
WHEN MATCHED THEN UPDATE SET
    Name = source.Name,
    Description = source.Description,
    PictureUri = source.PictureUri,
    PictureName = source.PictureName,
    DetailsUri = source.DetailsUri
WHEN NOT MATCHED THEN
    INSERT (Id, Name, Description, [From], [To], PictureUri, PictureName, DetailsUri)
    VALUES (source.Id, source.Name, source.Description, source.[From], source.[To], source.PictureUri, source.PictureName, source.DetailsUri);

MERGE dbo.[Rule] AS target
USING (VALUES
    (1, 1, N'Campaign is only for United States users.', 3, 1),
    (2, 2, N'Campaign is only for Seattle users.', 3, 3)
) AS source (Id, CampaignId, Description, RuleTypeId, LocationId)
ON target.Id = source.Id
WHEN MATCHED THEN UPDATE SET
    CampaignId = source.CampaignId,
    Description = source.Description,
    RuleTypeId = source.RuleTypeId,
    LocationId = source.LocationId
WHEN NOT MATCHED THEN
    INSERT (Id, CampaignId, Description, RuleTypeId, LocationId)
    VALUES (source.Id, source.CampaignId, source.Description, source.RuleTypeId, source.LocationId);

