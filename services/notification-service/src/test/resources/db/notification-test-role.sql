create role notification_app login password 'notification-test-password';
grant connect, create on database todorok to notification_app;
revoke create on schema public from public;
