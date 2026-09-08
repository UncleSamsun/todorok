alter table user_preference
  add column notifications_enabled boolean not null default false,
  add column summary_time char(5) not null default '08:00'
    check (summary_time ~ '^(?:[01][0-9]|2[0-3]):[0-5][0-9]$');
