MERGE INTO job (id, init_at, end_at, status, id_stack)
    VALUES (100, now(), now(), 'COMPLETED', 100);

MERGE INTO job (id, init_at, end_at, status, id_stack)
    VALUES (101, now(), now(), 'RUNNING', 100);

MERGE INTO job (id, init_at, end_at, status, id_stack)
    VALUES (102, now(), now(), 'FAILED', 100);
