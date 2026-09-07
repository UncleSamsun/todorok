create table daily_note (
 user_id uuid not null,
 date date not null,
 content text not null check (length(content) <= 20000),
 version bigint not null default 0 check (version >= 0),
 primary key (user_id, date)
);
alter table task add constraint task_note_length check (length(note) <= 20000);
alter table task_series add constraint series_note_length check (length(note) <= 20000);
