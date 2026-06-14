MERGE dbo.CatalogBrand AS target
USING (VALUES
    (1, N'Azure'),
    (2, N'.NET'),
    (3, N'Visual Studio'),
    (4, N'SQL Server'),
    (5, N'Other'),
    (6, N'CatalogBrandTestOne'),
    (7, N'CatalogBrandTestTwo')
) AS source (Id, Brand)
ON target.Id = source.Id
WHEN MATCHED THEN UPDATE SET Brand = source.Brand
WHEN NOT MATCHED THEN INSERT (Id, Brand) VALUES (source.Id, source.Brand);

MERGE dbo.CatalogType AS target
USING (VALUES
    (1, N'Mug'),
    (2, N'T-Shirt'),
    (3, N'Sheet'),
    (4, N'USB Memory Stick'),
    (5, N'CatalogTypeTestOne'),
    (6, N'CatalogTypeTestTwo')
) AS source (Id, [Type])
ON target.Id = source.Id
WHEN MATCHED THEN UPDATE SET [Type] = source.[Type]
WHEN NOT MATCHED THEN INSERT (Id, [Type]) VALUES (source.Id, source.[Type]);

MERGE dbo.Catalog AS target
USING (VALUES
    (1, N'.NET Bot Black Hoodie', N'.NET Bot Black Hoodie, and more', 19.50, N'1.png', 2, 2, 100, 0, 0, 0),
    (2, N'.NET Black & White Mug', N'.NET Black & White Mug', 8.50, N'2.png', 1, 2, 89, 0, 0, 1),
    (3, N'Prism White T-Shirt', N'Prism White T-Shirt', 12.00, N'3.png', 2, 5, 56, 0, 0, 0),
    (4, N'.NET Foundation T-shirt', N'.NET Foundation T-shirt', 12.00, N'4.png', 2, 2, 120, 0, 0, 0),
    (5, N'Roslyn Red Sheet', N'Roslyn Red Sheet', 8.50, N'5.png', 3, 5, 55, 0, 0, 0),
    (6, N'.NET Blue Hoodie', N'.NET Blue Hoodie', 12.00, N'6.png', 2, 2, 17, 0, 0, 0),
    (7, N'Roslyn Red T-Shirt', N'Roslyn Red T-Shirt', 12.00, N'7.png', 2, 5, 8, 0, 0, 0),
    (8, N'Kudu Purple Hoodie', N'Kudu Purple Hoodie', 8.50, N'8.png', 2, 5, 34, 0, 0, 0),
    (9, N'Cup<T> White Mug', N'Cup<T> White Mug', 12.00, N'9.png', 1, 5, 76, 0, 0, 0),
    (10, N'.NET Foundation Sheet', N'.NET Foundation Sheet', 12.00, N'10.png', 3, 2, 11, 0, 0, 0),
    (11, N'Cup<T> Sheet', N'Cup<T> Sheet', 8.50, N'11.png', 3, 2, 3, 0, 0, 0),
    (12, N'Prism White TShirt', N'Prism White TShirt', 12.00, N'12.png', 2, 5, 0, 0, 0, 0),
    (13, N'pepito', N'De los Palotes', 12.00, N'12.png', 1, 5, 0, 0, 0, 0)
) AS source (Id, Name, Description, Price, PictureFileName, CatalogTypeId, CatalogBrandId, AvailableStock, RestockThreshold, MaxStockThreshold, OnReorder)
ON target.Id = source.Id
WHEN MATCHED THEN UPDATE SET
    Name = source.Name,
    Description = source.Description,
    Price = source.Price,
    PictureFileName = source.PictureFileName,
    CatalogTypeId = source.CatalogTypeId,
    CatalogBrandId = source.CatalogBrandId,
    AvailableStock = source.AvailableStock,
    RestockThreshold = source.RestockThreshold,
    MaxStockThreshold = source.MaxStockThreshold,
    OnReorder = source.OnReorder
WHEN NOT MATCHED THEN
    INSERT (Id, Name, Description, Price, PictureFileName, CatalogTypeId, CatalogBrandId, AvailableStock, RestockThreshold, MaxStockThreshold, OnReorder)
    VALUES (source.Id, source.Name, source.Description, source.Price, source.PictureFileName, source.CatalogTypeId, source.CatalogBrandId, source.AvailableStock, source.RestockThreshold, source.MaxStockThreshold, source.OnReorder);

