create table user_account (
    id uuid primary key,
    email varchar(254) not null unique,
    password_hash varchar(100) not null,
    created_at timestamptz not null default now()
);

create table refresh_family (
    id uuid primary key,
    user_id uuid not null references user_account(id),
    created_at timestamptz not null default now(),
    revoked_at timestamptz
);
create index refresh_family_user on refresh_family(user_id);

create table refresh_session (
    token_hash char(64) primary key,
    family_id uuid not null references refresh_family(id),
    expires_at timestamptz not null,
    consumed_at timestamptz
);
create index refresh_session_family on refresh_session(family_id);

create table login_attempt_bucket (
    bucket_key varchar(80) primary key,
    window_start timestamptz not null,
    attempts integer not null
);
