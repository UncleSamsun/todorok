-- Do not discard or rewrite historical records to make this constraint pass.
do $$
begin
    if exists (
        select 1 from task where series_id is not null
        group by series_id, occurrence_date having count(*) > 1
    ) then
        raise exception 'Historical duplicate series occurrence dates exist; review and reconcile them explicitly before applying V8. No records were changed.';
    end if;
end
$$;

create unique index task_series_occurrence_unique
    on task(series_id, occurrence_date) where series_id is not null;
