create table planner_creation_command (
    owner_id uuid not null,
    command_id uuid not null,
    fingerprint varchar(64) not null,
    target_type varchar(10) not null check (target_type in ('TASK','SERIES')),
    target_id uuid not null unique,
    state varchar(10) not null default 'PENDING' check (state in ('PENDING','COMPLETE')),
    result jsonb,
    created_at timestamptz not null default now(),
    primary key (owner_id,command_id),
    check ((state='PENDING' and result is null) or (state='COMPLETE' and result is not null))
);

alter table task add column template_binding_id uuid, add column template_id uuid,
    add column selected_template_version bigint, add column template_name varchar(120), add column template_field_summary varchar(240),
    add constraint task_template_link_complete check (
      num_nonnulls(template_binding_id,template_id,selected_template_version,template_name,template_field_summary) in (0,5)
      and (template_binding_id is null or (task_type<>'GENERAL' and selected_template_version>=1)));
alter table task_series add column template_binding_id uuid, add column template_id uuid,
    add column selected_template_version bigint, add column template_name varchar(120), add column template_field_summary varchar(240),
    add constraint series_template_link_complete check (
      num_nonnulls(template_binding_id,template_id,selected_template_version,template_name,template_field_summary) in (0,5)
      and (template_binding_id is null or (task_type<>'GENERAL' and selected_template_version>=1)));

create function protect_planner_template_link() returns trigger language plpgsql as $$
begin
    if row(old.template_binding_id,old.template_id,old.selected_template_version,old.template_name,old.template_field_summary)
       is distinct from row(new.template_binding_id,new.template_id,new.selected_template_version,new.template_name,new.template_field_summary) then
        raise exception 'Template link is immutable';
    end if;
    return new;
end $$;
create trigger immutable_task_template_link before update on task for each row execute function protect_planner_template_link();
create trigger immutable_series_template_link before update on task_series for each row execute function protect_planner_template_link();

create function protect_planner_creation_command() returns trigger language plpgsql as $$
begin
    if TG_OP='DELETE' then raise exception 'Creation commands cannot be deleted'; end if;
    if old.state='COMPLETE' or new.state<>'COMPLETE' or new.result is null
       or row(old.owner_id,old.command_id,old.fingerprint,old.target_type,old.target_id,old.created_at)
        is distinct from row(new.owner_id,new.command_id,new.fingerprint,new.target_type,new.target_id,new.created_at) then
        raise exception 'Creation command identity and result are immutable';
    end if;
    return new;
end $$;
create trigger immutable_creation_command before update or delete on planner_creation_command
for each row execute function protect_planner_creation_command();
