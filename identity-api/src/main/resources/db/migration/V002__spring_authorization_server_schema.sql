IF OBJECT_ID(N'dbo.oauth2_registered_client', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.oauth2_registered_client
    (
        id varchar(100) NOT NULL CONSTRAINT PK_oauth2_registered_client PRIMARY KEY,
        client_id varchar(100) NOT NULL,
        client_id_issued_at datetime2 NOT NULL DEFAULT SYSUTCDATETIME(),
        client_secret varchar(200) NULL,
        client_secret_expires_at datetime2 NULL,
        client_name varchar(200) NOT NULL,
        client_authentication_methods varchar(1000) NOT NULL,
        authorization_grant_types varchar(1000) NOT NULL,
        redirect_uris varchar(1000) NULL,
        post_logout_redirect_uris varchar(1000) NULL,
        scopes varchar(1000) NOT NULL,
        client_settings varchar(2000) NOT NULL,
        token_settings varchar(2000) NOT NULL,
        CONSTRAINT AK_oauth2_registered_client_client_id UNIQUE (client_id)
    );
END;

IF OBJECT_ID(N'dbo.oauth2_authorization', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.oauth2_authorization
    (
        id varchar(100) NOT NULL CONSTRAINT PK_oauth2_authorization PRIMARY KEY,
        registered_client_id varchar(100) NOT NULL,
        principal_name varchar(200) NOT NULL,
        authorization_grant_type varchar(100) NOT NULL,
        authorized_scopes varchar(1000) NULL,
        attributes varchar(max) NULL,
        state varchar(500) NULL,
        authorization_code_value varchar(max) NULL,
        authorization_code_issued_at datetime2 NULL,
        authorization_code_expires_at datetime2 NULL,
        authorization_code_metadata varchar(max) NULL,
        access_token_value varchar(max) NULL,
        access_token_issued_at datetime2 NULL,
        access_token_expires_at datetime2 NULL,
        access_token_metadata varchar(max) NULL,
        access_token_type varchar(100) NULL,
        access_token_scopes varchar(1000) NULL,
        oidc_id_token_value varchar(max) NULL,
        oidc_id_token_issued_at datetime2 NULL,
        oidc_id_token_expires_at datetime2 NULL,
        oidc_id_token_metadata varchar(max) NULL,
        refresh_token_value varchar(max) NULL,
        refresh_token_issued_at datetime2 NULL,
        refresh_token_expires_at datetime2 NULL,
        refresh_token_metadata varchar(max) NULL,
        user_code_value varchar(max) NULL,
        user_code_issued_at datetime2 NULL,
        user_code_expires_at datetime2 NULL,
        user_code_metadata varchar(max) NULL,
        device_code_value varchar(max) NULL,
        device_code_issued_at datetime2 NULL,
        device_code_expires_at datetime2 NULL,
        device_code_metadata varchar(max) NULL
    );
END;

IF OBJECT_ID(N'dbo.oauth2_authorization_consent', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.oauth2_authorization_consent
    (
        registered_client_id varchar(100) NOT NULL,
        principal_name varchar(200) NOT NULL,
        authorities varchar(1000) NOT NULL,
        CONSTRAINT PK_oauth2_authorization_consent PRIMARY KEY (registered_client_id, principal_name)
    );
END;

