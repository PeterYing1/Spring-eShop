IF OBJECT_ID(N'dbo.Subscriptions', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Subscriptions
    (
        Id int IDENTITY(1,1) NOT NULL CONSTRAINT PK_Subscriptions PRIMARY KEY,
        [Type] int NOT NULL,
        [Date] datetime2 NOT NULL,
        DestUrl nvarchar(max) NULL,
        Token nvarchar(max) NULL,
        UserId nvarchar(max) NULL
    );
END;

