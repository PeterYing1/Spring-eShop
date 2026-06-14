IF OBJECT_ID(N'dbo.AspNetUsers', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.AspNetUsers
    (
        Id nvarchar(450) NOT NULL CONSTRAINT PK_AspNetUsers PRIMARY KEY,
        UserName nvarchar(256) NOT NULL,
        NormalizedUserName nvarchar(256) NOT NULL,
        Email nvarchar(256) NOT NULL,
        NormalizedEmail nvarchar(256) NOT NULL,
        EmailConfirmed bit NOT NULL CONSTRAINT DF_AspNetUsers_EmailConfirmed DEFAULT 0,
        PasswordHash nvarchar(max) NOT NULL,
        SecurityStamp nvarchar(max) NOT NULL,
        ConcurrencyStamp nvarchar(max) NULL,
        PhoneNumber nvarchar(max) NULL,
        PhoneNumberConfirmed bit NOT NULL CONSTRAINT DF_AspNetUsers_PhoneNumberConfirmed DEFAULT 0,
        TwoFactorEnabled bit NOT NULL CONSTRAINT DF_AspNetUsers_TwoFactorEnabled DEFAULT 0,
        LockoutEnd datetimeoffset NULL,
        LockoutEnabled bit NOT NULL CONSTRAINT DF_AspNetUsers_LockoutEnabled DEFAULT 0,
        AccessFailedCount int NOT NULL CONSTRAINT DF_AspNetUsers_AccessFailedCount DEFAULT 0,
        CardNumber nvarchar(max) NOT NULL,
        SecurityNumber nvarchar(max) NOT NULL,
        Expiration nvarchar(max) NOT NULL,
        CardHolderName nvarchar(max) NOT NULL,
        CardType int NOT NULL,
        Street nvarchar(max) NOT NULL,
        City nvarchar(max) NOT NULL,
        [State] nvarchar(max) NOT NULL,
        Country nvarchar(max) NOT NULL,
        ZipCode nvarchar(max) NOT NULL,
        Name nvarchar(max) NOT NULL,
        LastName nvarchar(max) NOT NULL
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'UserNameIndex' AND object_id = OBJECT_ID(N'dbo.AspNetUsers'))
    CREATE UNIQUE INDEX UserNameIndex ON dbo.AspNetUsers(NormalizedUserName);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'EmailIndex' AND object_id = OBJECT_ID(N'dbo.AspNetUsers'))
    CREATE INDEX EmailIndex ON dbo.AspNetUsers(NormalizedEmail);

