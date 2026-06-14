IF NOT EXISTS (SELECT 1 FROM sys.sequences WHERE name = N'campaign_hilo' AND SCHEMA_NAME(schema_id) = N'dbo')
    EXEC('CREATE SEQUENCE dbo.campaign_hilo START WITH 100 INCREMENT BY 10');

IF NOT EXISTS (SELECT 1 FROM sys.sequences WHERE name = N'rule_hilo' AND SCHEMA_NAME(schema_id) = N'dbo')
    EXEC('CREATE SEQUENCE dbo.rule_hilo START WITH 100 INCREMENT BY 10');

IF OBJECT_ID(N'dbo.Campaign', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Campaign
    (
        Id int NOT NULL CONSTRAINT PK_Campaign PRIMARY KEY DEFAULT NEXT VALUE FOR dbo.campaign_hilo,
        Name nvarchar(max) NOT NULL,
        Description nvarchar(max) NOT NULL,
        [From] datetime2 NOT NULL,
        [To] datetime2 NOT NULL,
        PictureUri nvarchar(max) NOT NULL,
        PictureName nvarchar(max) NULL,
        DetailsUri nvarchar(max) NULL
    );
END;

IF OBJECT_ID(N'dbo.Rule', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.[Rule]
    (
        Id int NOT NULL CONSTRAINT PK_Rule PRIMARY KEY DEFAULT NEXT VALUE FOR dbo.rule_hilo,
        CampaignId int NOT NULL,
        Description nvarchar(max) NOT NULL,
        RuleTypeId int NOT NULL,
        LocationId int NULL,
        CONSTRAINT FK_Rule_Campaign_CampaignId FOREIGN KEY (CampaignId) REFERENCES dbo.Campaign(Id) ON DELETE CASCADE
    );
END;

