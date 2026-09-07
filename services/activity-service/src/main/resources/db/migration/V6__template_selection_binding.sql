create table template_selection_binding (
    id uuid primary key,
    user_id uuid not null,
    template_id uuid not null references record_template(id),
    selected_version bigint not null,
    target_type varchar(10) not null check (target_type in ('TASK','SERIES')),
    target_id uuid not null,
    request_id uuid not null,
    request_fingerprint varchar(64) not null,
    created_at timestamptz not null default now(),
    foreign key (template_id,selected_version) references template_version(template_id,version),
    unique (user_id,request_id),
    unique (target_type,target_id)
);
create trigger immutable_template_selection_binding before update or delete on template_selection_binding
for each row execute function reject_template_immutable_mutation();

alter table task_reference add column series_id uuid,
    add column template_binding_id uuid references template_selection_binding(id),
    add column template_id uuid,
    add column selected_template_version bigint,
    add constraint task_reference_link_complete check (
      (template_binding_id is null and template_id is null and selected_template_version is null) or
      (template_binding_id is not null and template_id is not null and selected_template_version is not null)),
    add foreign key (template_id,selected_template_version) references template_version(template_id,version);
