-- Existing rows retain every original detail and history value. Only the format is classified.
alter table activity_record
    add column detail_format varchar(10) not null default 'LEGACY',
    add column template_id uuid,
    add column template_version bigint,
    add column template_binding_id uuid references template_selection_binding(id),
    add column template_snapshot jsonb,
    add constraint activity_template_version_fk foreign key (template_id,template_version)
        references template_version(template_id,version),
    add constraint activity_record_format check (
      (detail_format in ('LEGACY','STANDARD') and template_id is null and template_version is null
        and template_binding_id is null and template_snapshot is null) or
      (detail_format='TEMPLATE' and template_id is not null and template_version is not null
        and template_binding_id is not null and template_snapshot is not null
        and jsonb_typeof(template_snapshot)='object'));
alter table activity_record alter column detail_format set default 'STANDARD';

create function preserve_activity_template_identity() returns trigger language plpgsql as $$
begin
    if old.detail_format is distinct from new.detail_format
       or old.template_id is distinct from new.template_id
       or old.template_version is distinct from new.template_version
       or old.template_binding_id is distinct from new.template_binding_id
       or old.template_snapshot is distinct from new.template_snapshot then
        raise exception 'Activity template snapshot and provenance are immutable';
    end if;
    return new;
end $$;
create trigger immutable_activity_template before update on activity_record
for each row execute function preserve_activity_template_identity();
