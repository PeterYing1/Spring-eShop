IF NOT EXISTS (SELECT 1 FROM sys.sequences WHERE name = N'catalog_hilo' AND SCHEMA_NAME(schema_id) = N'dbo')
    EXEC('CREATE SEQUENCE dbo.catalog_hilo START WITH 100 INCREMENT BY 10');

IF NOT EXISTS (SELECT 1 FROM sys.sequences WHERE name = N'catalogbrand_hilo' AND SCHEMA_NAME(schema_id) = N'dbo')
    EXEC('CREATE SEQUENCE dbo.catalogbrand_hilo START WITH 100 INCREMENT BY 10');

IF NOT EXISTS (SELECT 1 FROM sys.sequences WHERE name = N'catalogtype_hilo' AND SCHEMA_NAME(schema_id) = N'dbo')
    EXEC('CREATE SEQUENCE dbo.catalogtype_hilo START WITH 100 INCREMENT BY 10');

IF OBJECT_ID(N'dbo.CatalogBrand', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.CatalogBrand
    (
        Id int NOT NULL CONSTRAINT PK_CatalogBrand PRIMARY KEY DEFAULT NEXT VALUE FOR dbo.catalogbrand_hilo,
        Brand nvarchar(100) NOT NULL
    );
END;

IF OBJECT_ID(N'dbo.CatalogType', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.CatalogType
    (
        Id int NOT NULL CONSTRAINT PK_CatalogType PRIMARY KEY DEFAULT NEXT VALUE FOR dbo.catalogtype_hilo,
        [Type] nvarchar(100) NOT NULL
    );
END;

IF OBJECT_ID(N'dbo.Catalog', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Catalog
    (
        Id int NOT NULL CONSTRAINT PK_Catalog PRIMARY KEY DEFAULT NEXT VALUE FOR dbo.catalog_hilo,
        Name nvarchar(50) NOT NULL,
        Description nvarchar(max) NOT NULL,
        Price decimal(18,2) NOT NULL,
        PictureFileName nvarchar(max) NULL,
        CatalogTypeId int NOT NULL,
        CatalogBrandId int NOT NULL,
        AvailableStock int NOT NULL CONSTRAINT DF_Catalog_AvailableStock DEFAULT 0,
        RestockThreshold int NOT NULL CONSTRAINT DF_Catalog_RestockThreshold DEFAULT 0,
        MaxStockThreshold int NOT NULL CONSTRAINT DF_Catalog_MaxStockThreshold DEFAULT 0,
        OnReorder bit NOT NULL CONSTRAINT DF_Catalog_OnReorder DEFAULT 0,
        CONSTRAINT FK_Catalog_CatalogBrand_CatalogBrandId FOREIGN KEY (CatalogBrandId) REFERENCES dbo.CatalogBrand(Id),
        CONSTRAINT FK_Catalog_CatalogType_CatalogTypeId FOREIGN KEY (CatalogTypeId) REFERENCES dbo.CatalogType(Id)
    );
END;

IF OBJECT_ID(N'dbo.IntegrationEventLog', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.IntegrationEventLog
    (
        EventId uniqueidentifier NOT NULL CONSTRAINT PK_IntegrationEventLog PRIMARY KEY,
        EventTypeName nvarchar(max) NOT NULL,
        [State] int NOT NULL,
        TimesSent int NOT NULL,
        CreationTime datetime2 NOT NULL,
        Content nvarchar(max) NOT NULL,
        TransactionId nvarchar(max) NULL
    );
END;

