create table record_template (
    id uuid primary key,
    user_id uuid not null,
    domain varchar(20) not null check (domain in ('STUDY','WORKOUT','CLIMBING')),
    kind varchar(30) not null check (kind in ('STUDY_CATEGORY','FREE_WORKOUT','FREE_HANGBOARD','CLIMBING_SESSION')),
    current_version bigint not null check (current_version >= 1),
    revision bigint not null default 0 check (revision >= 0),
    archived_at timestamptz,
    created_at timestamptz not null default now(),
    unique (id, current_version),
    check (
        (domain='STUDY' and kind='STUDY_CATEGORY') or
        (domain='WORKOUT' and kind='FREE_WORKOUT') or
        (domain='CLIMBING' and kind in ('FREE_HANGBOARD','CLIMBING_SESSION'))
    )
);

create table template_version (
    template_id uuid not null references record_template(id),
    version bigint not null check (version >= 1),
    name varchar(120) not null check (name ~ '[^[:space:]]'),
    created_at timestamptz not null default now(),
    primary key (template_id, version)
);

alter table record_template add constraint record_template_current_version_fk
    foreign key (id, current_version) references template_version(template_id, version)
    deferrable initially deferred;

create table template_field_identity (
    template_id uuid not null,
    field_id uuid not null,
    created_version bigint not null,
    type varchar(20) not null check (type in ('NUMBER','TIME','SHORT_TEXT','CHECK','MEMO')),
    created_at timestamptz not null default now(),
    primary key (template_id, field_id),
    unique (field_id),
    unique (template_id, field_id, type),
    foreign key (template_id, created_version)
        references template_version(template_id, version)
);

create table template_field_definition (
    template_id uuid not null,
    version bigint not null,
    field_id uuid not null,
    name varchar(120) not null check (name ~ '[^[:space:]]'),
    type varchar(20) not null check (type in ('NUMBER','TIME','SHORT_TEXT','CHECK','MEMO')),
    unit varchar(40),
    position integer not null check (position >= 0),
    primary key (template_id, version, field_id),
    unique (template_id, version, position),
    foreign key (template_id, version)
        references template_version(template_id, version),
    foreign key (template_id, field_id, type)
        references template_field_identity(template_id, field_id, type),
    check (
        (type='NUMBER') or
        (type='TIME' and unit is not null and unit in ('초','분','시간')) or
        (type in ('SHORT_TEXT','CHECK','MEMO') and unit is null)
    )
);

create table template_management_command (
    user_id uuid not null,
    command_id uuid not null,
    fingerprint varchar(64) not null,
    status_code integer not null check (status_code in (200, 201)),
    response_json jsonb not null,
    created_at timestamptz not null default now(),
    primary key (user_id, command_id)
);

create index record_template_owner_page
    on record_template(user_id, created_at desc, id desc);

create function reject_template_immutable_mutation() returns trigger language plpgsql as $$
begin
    raise exception 'Template version and field history is immutable';
end $$;

create trigger immutable_template_version before update or delete on template_version
for each row execute function reject_template_immutable_mutation();

create trigger immutable_template_field_identity before update or delete on template_field_identity
for each row execute function reject_template_immutable_mutation();

create trigger immutable_template_field_definition before update or delete on template_field_definition
for each row execute function reject_template_immutable_mutation();

create function protect_record_template_identity() returns trigger language plpgsql as $$
begin
    if old.id is distinct from new.id
       or old.user_id is distinct from new.user_id
       or old.domain is distinct from new.domain
       or old.kind is distinct from new.kind
       or old.created_at is distinct from new.created_at then
        raise exception 'Template identity is immutable';
    end if;
    if new.revision <> old.revision + 1 then
        raise exception 'Template revision must advance exactly once';
    end if;
    if new.current_version = old.current_version + 1 then
        if old.archived_at is not null or new.archived_at is not null then
            raise exception 'Archived templates cannot receive new versions';
        end if;
    elsif new.current_version = old.current_version then
        if old.archived_at is not null or new.archived_at is null then
            raise exception 'Template archive is one-way';
        end if;
    else
        raise exception 'Template version must remain current or advance exactly once';
    end if;
    return new;
end $$;

create trigger immutable_record_template_identity before update on record_template
for each row execute function protect_record_template_identity();

create trigger prevent_record_template_delete before delete on record_template
for each row execute function reject_template_immutable_mutation();

create trigger immutable_template_management_command before update or delete on template_management_command
for each row execute function reject_template_immutable_mutation();
