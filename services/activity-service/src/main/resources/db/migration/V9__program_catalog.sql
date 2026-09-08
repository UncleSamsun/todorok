create table program_catalog (
    catalog_key varchar(120) not null,
    catalog_version bigint not null check (catalog_version >= 1),
    checksum char(64) not null check (checksum ~ '^[a-f0-9]{64}$'),
    name varchar(120) not null,
    source jsonb not null check (jsonb_typeof(source)='object'),
    definition jsonb not null check (jsonb_typeof(definition)='object'),
    imported_at timestamptz not null default now(),
    primary key (catalog_key, catalog_version),
    unique (catalog_key, checksum)
);
