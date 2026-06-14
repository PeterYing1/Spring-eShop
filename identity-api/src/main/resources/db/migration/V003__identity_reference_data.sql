MERGE dbo.AspNetUsers AS target
USING (VALUES
    (
        N'demouser@microsoft.com',
        N'demouser@microsoft.com',
        N'DEMOUSER@MICROSOFT.COM',
        N'demouser@microsoft.com',
        N'DEMOUSER@MICROSOFT.COM',
        N'{noop}Pass@word1',
        N'seed-security-stamp',
        N'1234567890',
        N'4012888888881881',
        N'535',
        N'12/21',
        N'DemoUser',
        1,
        N'15703 NE 61st Ct',
        N'Redmond',
        N'WA',
        N'U.S.',
        N'98052',
        N'DemoUser',
        N'DemoLastName'
    )
) AS source (Id, UserName, NormalizedUserName, Email, NormalizedEmail, PasswordHash, SecurityStamp, PhoneNumber, CardNumber, SecurityNumber, Expiration, CardHolderName, CardType, Street, City, [State], Country, ZipCode, Name, LastName)
ON target.NormalizedUserName = source.NormalizedUserName
WHEN MATCHED THEN UPDATE SET
    UserName = source.UserName,
    Email = source.Email,
    NormalizedEmail = source.NormalizedEmail,
    PhoneNumber = source.PhoneNumber,
    CardNumber = source.CardNumber,
    SecurityNumber = source.SecurityNumber,
    Expiration = source.Expiration,
    CardHolderName = source.CardHolderName,
    CardType = source.CardType,
    Street = source.Street,
    City = source.City,
    [State] = source.[State],
    Country = source.Country,
    ZipCode = source.ZipCode,
    Name = source.Name,
    LastName = source.LastName
WHEN NOT MATCHED THEN
    INSERT (Id, UserName, NormalizedUserName, Email, NormalizedEmail, PasswordHash, SecurityStamp, PhoneNumber, CardNumber, SecurityNumber, Expiration, CardHolderName, CardType, Street, City, [State], Country, ZipCode, Name, LastName)
    VALUES (source.Id, source.UserName, source.NormalizedUserName, source.Email, source.NormalizedEmail, source.PasswordHash, source.SecurityStamp, source.PhoneNumber, source.CardNumber, source.SecurityNumber, source.Expiration, source.CardHolderName, source.CardType, source.Street, source.City, source.[State], source.Country, source.ZipCode, source.Name, source.LastName);

MERGE dbo.oauth2_registered_client AS target
USING (VALUES
    ('js', 'js', NULL, 'eShop SPA OpenId Client', 'none', 'authorization_code', 'http://localhost:5104/', 'http://localhost:5104/', 'openid,profile,orders,basket,locations,marketing,webshoppingagg,orders.signalrhub,webhooks', '{"settings.client.require-proof-key":true,"settings.client.require-authorization-consent":false}', '{"settings.token.access-token-time-to-live":["java.time.Duration",7200.000000000]}'),
    ('xamarin', 'xamarin', '{noop}secret', 'eShop Xamarin OpenId Client', 'client_secret_basic', 'authorization_code,refresh_token', 'http://localhost:5105/xamarin', 'http://localhost:5105/xamarin/Account/Redirecting', 'openid,profile,offline_access,orders,basket,locations,marketing,mobileshoppingagg,webhooks', '{"settings.client.require-proof-key":true,"settings.client.require-authorization-consent":false}', '{"settings.token.access-token-time-to-live":["java.time.Duration",7200.000000000]}'),
    ('mvc', 'mvc', '{noop}secret', 'MVC Client', 'client_secret_basic', 'authorization_code,refresh_token', 'http://localhost:5100/signin-oidc', 'http://localhost:5100/signout-callback-oidc', 'openid,profile,offline_access,orders,basket,locations,marketing,webshoppingagg,orders.signalrhub,webhooks', '{"settings.client.require-proof-key":false,"settings.client.require-authorization-consent":false}', '{"settings.token.access-token-time-to-live":["java.time.Duration",7200.000000000],"settings.token.id-token-time-to-live":["java.time.Duration",7200.000000000]}'),
    ('webhooksclient', 'webhooksclient', '{noop}secret', 'Webhooks Client', 'client_secret_basic', 'authorization_code,refresh_token', 'http://localhost:5113/signin-oidc', 'http://localhost:5113/signout-callback-oidc', 'openid,profile,offline_access,webhooks', '{"settings.client.require-proof-key":false,"settings.client.require-authorization-consent":false}', '{"settings.token.access-token-time-to-live":["java.time.Duration",7200.000000000],"settings.token.id-token-time-to-live":["java.time.Duration",7200.000000000]}'),
    ('mvctest', 'mvctest', '{noop}secret', 'MVC Client Test', 'client_secret_basic', 'authorization_code,refresh_token', 'http://localhost:5100/signin-oidc', 'http://localhost:5100/signout-callback-oidc', 'openid,profile,offline_access,orders,basket,locations,marketing,webshoppingagg,webhooks', '{"settings.client.require-proof-key":false,"settings.client.require-authorization-consent":false}', '{}'),
    ('locationsswaggerui', 'locationsswaggerui', NULL, 'Locations Swagger UI', 'none', 'authorization_code', 'http://localhost:5109/swagger/oauth2-redirect.html', 'http://localhost:5109/swagger/', 'locations', '{"settings.client.require-proof-key":true,"settings.client.require-authorization-consent":false}', '{}'),
    ('marketingswaggerui', 'marketingswaggerui', NULL, 'Marketing Swagger UI', 'none', 'authorization_code', 'http://localhost:5110/swagger/oauth2-redirect.html', 'http://localhost:5110/swagger/', 'marketing', '{"settings.client.require-proof-key":true,"settings.client.require-authorization-consent":false}', '{}'),
    ('basketswaggerui', 'basketswaggerui', NULL, 'Basket Swagger UI', 'none', 'authorization_code', 'http://localhost:5103/swagger/oauth2-redirect.html', 'http://localhost:5103/swagger/', 'basket', '{"settings.client.require-proof-key":true,"settings.client.require-authorization-consent":false}', '{}'),
    ('orderingswaggerui', 'orderingswaggerui', NULL, 'Ordering Swagger UI', 'none', 'authorization_code', 'http://localhost:5102/swagger/oauth2-redirect.html', 'http://localhost:5102/swagger/', 'orders', '{"settings.client.require-proof-key":true,"settings.client.require-authorization-consent":false}', '{}'),
    ('mobileshoppingaggswaggerui', 'mobileshoppingaggswaggerui', NULL, 'Mobile Shopping Aggregattor Swagger UI', 'none', 'authorization_code', 'http://localhost:5120/swagger/oauth2-redirect.html', 'http://localhost:5120/swagger/', 'mobileshoppingagg', '{"settings.client.require-proof-key":true,"settings.client.require-authorization-consent":false}', '{}'),
    ('webshoppingaggswaggerui', 'webshoppingaggswaggerui', NULL, 'Web Shopping Aggregattor Swagger UI', 'none', 'authorization_code', 'http://localhost:5121/swagger/oauth2-redirect.html', 'http://localhost:5121/swagger/', 'webshoppingagg,basket', '{"settings.client.require-proof-key":true,"settings.client.require-authorization-consent":false}', '{}'),
    ('webhooksswaggerui', 'webhooksswaggerui', NULL, 'WebHooks Service Swagger UI', 'none', 'authorization_code', 'http://localhost:5112/swagger/oauth2-redirect.html', 'http://localhost:5112/swagger/', 'webhooks', '{"settings.client.require-proof-key":true,"settings.client.require-authorization-consent":false}', '{}')
) AS source (id, client_id, client_secret, client_name, client_authentication_methods, authorization_grant_types, redirect_uris, post_logout_redirect_uris, scopes, client_settings, token_settings)
ON target.client_id = source.client_id
WHEN MATCHED THEN UPDATE SET
    client_secret = source.client_secret,
    client_name = source.client_name,
    client_authentication_methods = source.client_authentication_methods,
    authorization_grant_types = source.authorization_grant_types,
    redirect_uris = source.redirect_uris,
    post_logout_redirect_uris = source.post_logout_redirect_uris,
    scopes = source.scopes,
    client_settings = source.client_settings,
    token_settings = source.token_settings
WHEN NOT MATCHED THEN
    INSERT (id, client_id, client_secret, client_name, client_authentication_methods, authorization_grant_types, redirect_uris, post_logout_redirect_uris, scopes, client_settings, token_settings)
    VALUES (source.id, source.client_id, source.client_secret, source.client_name, source.client_authentication_methods, source.authorization_grant_types, source.redirect_uris, source.post_logout_redirect_uris, source.scopes, source.client_settings, source.token_settings);
