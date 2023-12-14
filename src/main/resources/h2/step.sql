MERGE INTO step (id, uuid, payload, init_at, end_at, status, id_job, id_resource)
    VALUES (100, 'c382fe79-91d0-4524-8b8f-594816ebed33', '{"same-key": "same value"}', now(), now(), 'COMPLETED', 100, 100);
MERGE INTO step (id, uuid, payload, init_at, end_at, status, id_job, id_resource)
    VALUES (101, '2078ca1d-5728-45e3-934a-d38bfcfa4ed3', '{"same-key": "same value"}', now(), now(), 'COMPLETED', 100, 101);

MERGE INTO step (id, uuid, payload, init_at, end_at, status, id_job, id_resource)
    VALUES (102, 'c382fe79-91d0-4524-8b8f-594816ebed34', '{"same-key": "same value"}', now(), now(), 'COMPLETED', 101, 100);
MERGE INTO step (id, uuid, payload, init_at, end_at, status, id_job, id_resource)
    VALUES (103, '2078ca1d-5728-45e3-934a-d38bfcfa4ed5', '{"same-key": "same value"}', now(), now(), 'RUNNING', 101, 101);

MERGE INTO step (id, uuid, payload, init_at, end_at, status, id_job, id_resource)
    VALUES (104, '2034ca1d-5728-45e3-934a-d38bfcfa4ed5', null, now(), now(), 'FAILED', 102, 100);
