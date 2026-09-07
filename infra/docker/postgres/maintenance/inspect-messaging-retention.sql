select 'planner.outbox_event' as relation,
       count(*) as row_count,
       min(created_at) as oldest_recorded_at
  from planner.outbox_event
union all
select 'activity.outbox_event', count(*), min(created_at)
  from activity.outbox_event
union all
select 'planner.processed_event', count(*), min(processed_at)
  from planner.processed_event
union all
select 'activity.processed_event', count(*), min(processed_at)
  from activity.processed_event
union all
select 'notification.processed_event', count(*), min(processed_at)
  from notification.processed_event
order by relation;
