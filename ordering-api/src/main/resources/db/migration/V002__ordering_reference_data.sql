MERGE ordering.cardtypes AS target
USING (VALUES
    (1, N'Amex'),
    (2, N'Visa'),
    (3, N'MasterCard')
) AS source (Id, Name)
ON target.Id = source.Id
WHEN MATCHED THEN UPDATE SET Name = source.Name
WHEN NOT MATCHED THEN INSERT (Id, Name) VALUES (source.Id, source.Name);

MERGE ordering.orderstatus AS target
USING (VALUES
    (1, N'submitted'),
    (2, N'awaitingvalidation'),
    (3, N'stockconfirmed'),
    (4, N'paid'),
    (5, N'shipped'),
    (6, N'cancelled')
) AS source (Id, Name)
ON target.Id = source.Id
WHEN MATCHED THEN UPDATE SET Name = source.Name
WHEN NOT MATCHED THEN INSERT (Id, Name) VALUES (source.Id, source.Name);

