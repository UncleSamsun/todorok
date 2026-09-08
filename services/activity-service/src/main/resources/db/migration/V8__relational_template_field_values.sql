alter table activity_record
    add constraint activity_record_template_identity_unique unique (id, template_id, template_version);
alter table template_field_definition
    add constraint template_field_definition_type_unique unique (template_id, version, field_id, type);

create table activity_field_value (
    activity_id uuid not null,
    template_id uuid not null,
    template_version bigint not null,
    field_id uuid not null,
    type varchar(20) not null,
    number_value numeric,
    time_seconds bigint,
    text_value varchar(120),
    checked boolean,
    memo_value text,
    primary key (activity_id, field_id),
    foreign key (activity_id, template_id, template_version)
        references activity_record(id, template_id, template_version),
    foreign key (template_id, template_version, field_id, type)
        references template_field_definition(template_id, version, field_id, type),
    constraint activity_field_value_type check (type in ('NUMBER','TIME','SHORT_TEXT','CHECK','MEMO')),
    constraint activity_field_value_shape check (
      (type='NUMBER' and number_value is not null and time_seconds is null and text_value is null and checked is null and memo_value is null) or
      (type='TIME' and number_value is null and time_seconds between 0 and 9007199254740991 and text_value is null and checked is null and memo_value is null) or
      (type='SHORT_TEXT' and number_value is null and time_seconds is null and text_value is not null and checked is null and memo_value is null) or
      (type='CHECK' and number_value is null and time_seconds is null and text_value is null and checked is not null and memo_value is null) or
      (type='MEMO' and number_value is null and time_seconds is null and text_value is null and checked is null and memo_value is not null)
    )
);

create index activity_field_value_template_idx
    on activity_field_value(template_id, template_version, field_id);
