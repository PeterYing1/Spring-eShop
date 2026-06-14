IF SCHEMA_ID(N'ordering') IS NULL
    EXEC('CREATE SCHEMA ordering');

IF NOT EXISTS (SELECT 1 FROM sys.sequences WHERE name = N'orderseq' AND SCHEMA_NAME(schema_id) = N'ordering')
    EXEC('CREATE SEQUENCE ordering.orderseq START WITH 100 INCREMENT BY 10');

IF NOT EXISTS (SELECT 1 FROM sys.sequences WHERE name = N'buyerseq' AND SCHEMA_NAME(schema_id) = N'ordering')
    EXEC('CREATE SEQUENCE ordering.buyerseq START WITH 100 INCREMENT BY 10');

IF NOT EXISTS (SELECT 1 FROM sys.sequences WHERE name = N'paymentseq' AND SCHEMA_NAME(schema_id) = N'ordering')
    EXEC('CREATE SEQUENCE ordering.paymentseq START WITH 100 INCREMENT BY 10');

IF NOT EXISTS (SELECT 1 FROM sys.sequences WHERE name = N'orderitemseq')
    EXEC('CREATE SEQUENCE dbo.orderitemseq START WITH 100 INCREMENT BY 10');

IF OBJECT_ID(N'ordering.cardtypes', N'U') IS NULL
BEGIN
    CREATE TABLE ordering.cardtypes
    (
        Id int NOT NULL CONSTRAINT PK_cardtypes PRIMARY KEY,
        Name nvarchar(200) NOT NULL
    );
END;

IF OBJECT_ID(N'ordering.orderstatus', N'U') IS NULL
BEGIN
    CREATE TABLE ordering.orderstatus
    (
        Id int NOT NULL CONSTRAINT PK_orderstatus PRIMARY KEY,
        Name nvarchar(200) NOT NULL
    );
END;

IF OBJECT_ID(N'ordering.buyers', N'U') IS NULL
BEGIN
    CREATE TABLE ordering.buyers
    (
        Id int NOT NULL CONSTRAINT PK_buyers PRIMARY KEY DEFAULT NEXT VALUE FOR ordering.buyerseq,
        IdentityGuid nvarchar(200) NOT NULL,
        Name nvarchar(max) NULL,
        CONSTRAINT AK_buyers_IdentityGuid UNIQUE (IdentityGuid)
    );
END;

IF OBJECT_ID(N'ordering.paymentmethods', N'U') IS NULL
BEGIN
    CREATE TABLE ordering.paymentmethods
    (
        Id int NOT NULL CONSTRAINT PK_paymentmethods PRIMARY KEY DEFAULT NEXT VALUE FOR ordering.paymentseq,
        BuyerId int NOT NULL,
        CardTypeId int NOT NULL,
        Alias nvarchar(200) NOT NULL,
        CardNumber nvarchar(25) NOT NULL,
        SecurityNumber nvarchar(max) NULL,
        CardHolderName nvarchar(200) NOT NULL,
        Expiration datetime2 NOT NULL,
        CONSTRAINT FK_paymentmethods_buyers_BuyerId FOREIGN KEY (BuyerId) REFERENCES ordering.buyers(Id) ON DELETE CASCADE,
        CONSTRAINT FK_paymentmethods_cardtypes_CardTypeId FOREIGN KEY (CardTypeId) REFERENCES ordering.cardtypes(Id)
    );
END;

IF OBJECT_ID(N'ordering.orders', N'U') IS NULL
BEGIN
    CREATE TABLE ordering.orders
    (
        Id int NOT NULL CONSTRAINT PK_orders PRIMARY KEY DEFAULT NEXT VALUE FOR ordering.orderseq,
        BuyerId int NULL,
        OrderDate datetime2 NOT NULL,
        OrderStatusId int NOT NULL,
        PaymentMethodId int NULL,
        Description nvarchar(max) NULL,
        Address_Street nvarchar(max) NULL,
        Address_City nvarchar(max) NULL,
        Address_State nvarchar(max) NULL,
        Address_Country nvarchar(max) NULL,
        Address_ZipCode nvarchar(max) NULL,
        CONSTRAINT FK_orders_buyers_BuyerId FOREIGN KEY (BuyerId) REFERENCES ordering.buyers(Id),
        CONSTRAINT FK_orders_orderstatus_OrderStatusId FOREIGN KEY (OrderStatusId) REFERENCES ordering.orderstatus(Id),
        CONSTRAINT FK_orders_paymentmethods_PaymentMethodId FOREIGN KEY (PaymentMethodId) REFERENCES ordering.paymentmethods(Id) ON DELETE NO ACTION
    );
END;

IF OBJECT_ID(N'ordering.orderItems', N'U') IS NULL
BEGIN
    CREATE TABLE ordering.orderItems
    (
        Id int NOT NULL CONSTRAINT PK_orderItems PRIMARY KEY DEFAULT NEXT VALUE FOR dbo.orderitemseq,
        OrderId int NOT NULL,
        ProductId int NOT NULL,
        ProductName nvarchar(max) NOT NULL,
        UnitPrice decimal(18,2) NOT NULL,
        Discount decimal(18,2) NOT NULL,
        Units int NOT NULL,
        PictureUrl nvarchar(max) NULL,
        CONSTRAINT FK_orderItems_orders_OrderId FOREIGN KEY (OrderId) REFERENCES ordering.orders(Id) ON DELETE CASCADE
    );
END;

IF OBJECT_ID(N'ordering.requests', N'U') IS NULL
BEGIN
    CREATE TABLE ordering.requests
    (
        Id uniqueidentifier NOT NULL CONSTRAINT PK_requests PRIMARY KEY,
        Name nvarchar(max) NOT NULL,
        [Time] datetime2 NOT NULL
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

