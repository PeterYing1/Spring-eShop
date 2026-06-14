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

