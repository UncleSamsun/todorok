create role activity_app login password 'activity-test-password';
grant connect, create on database todorok to activity_app;
revoke create on schema public from public;
