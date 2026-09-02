create table service_metadata (
    service_name varchar(40) primary key,
    schema_version integer not null,
    installed_at timestamptz not null default now()
);

insert into service_metadata(service_name, schema_version)
values ('notification-service', 1);
