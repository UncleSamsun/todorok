create table push_subscription (
    id uuid primary key,
    user_id uuid not null,
    endpoint_hash char(64) not null unique,
    secret text,
    active boolean not null default true,
    updated_at timestamptz not null default now(),
    check ((active and secret is not null) or (not active and secret is null))
);
create index push_subscription_owner on push_subscription(user_id) where active;

alter table notification_delivery drop constraint notification_delivery_check;
alter table notification_delivery
    add column lease_token uuid,
    add column lease_until timestamptz,
    add column next_attempt_at timestamptz not null default now(),
    add column error_code varchar(40),
    add column targets_created boolean not null default false;
create index notification_delivery_due on notification_delivery(next_attempt_at,scheduled_for) where status='PENDING';

create table notification_attempt (
    delivery_id uuid not null references notification_delivery(id),
    subscription_id uuid not null references push_subscription(id),
    status varchar(12) not null check (status in ('PENDING','DELIVERED','FAILED')),
    attempts integer not null default 0,
    error_code varchar(40),
    primary key(delivery_id,subscription_id)
);
