create table task_series (
 id uuid primary key,
 user_id uuid not null,
 title varchar(120) not null check (length(trim(title)) > 0),
 task_type varchar(20) not null check (task_type in ('GENERAL','WORKOUT','STUDY','CLIMBING')),
 note text,
 start_date date not null,
 end_date date check (end_date >= start_date),
 frequency varchar(10) not null check (frequency in ('DAILY','WEEKLY','MONTHLY')),
 repeat_interval integer not null check (repeat_interval between 1 and 365),
 weekdays varchar(20) not null,
 month_day integer not null check (month_day between 1 and 31),
 archived boolean not null default false,
 version bigint not null default 0,
 unique (id,user_id)
);
alter table task add column series_id uuid;
alter table task add column occurrence_date date;
alter table task add column note text;
alter table task add constraint task_series_owner_fk foreign key (series_id,user_id) references task_series(id,user_id);
alter table task add constraint task_occurrence_required check (series_id is null or occurrence_date is not null);
create unique index task_series_one_planned on task(series_id) where status='PLANNED' and series_id is not null;
create index task_series_owner_idx on task_series(user_id,id);
