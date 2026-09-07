create role planner_app login password 'planner-test-password';
grant connect, create on database todorok to planner_app;
revoke create on schema public from public;
