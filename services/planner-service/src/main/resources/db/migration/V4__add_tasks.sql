create table task (
 id uuid primary key,
 user_id uuid not null,
 title varchar(120) not null check (length(trim(title)) > 0),
 task_type varchar(20) not null check (task_type in ('GENERAL','WORKOUT','STUDY','CLIMBING')),
 scheduled_date date not null,
 status varchar(20) not null check (status in ('PLANNED','COMPLETED','SKIPPED','DELETED')),
 version bigint not null default 0 check (version >= 0)
);
create index task_user_scheduled_date_idx on task(user_id, scheduled_date);
