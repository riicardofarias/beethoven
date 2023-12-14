create table if not exists stack
(
    id          bigint auto_increment primary key,
    external_id varchar(255)         null,
    name        varchar(255)         not null,
    description varchar(255)         null,
    deleted     tinyint(1) default 0 null
);
create index stack_external_id_index on stack (external_id);

create table if not exists job
(
    id       bigint auto_increment primary key,
    init_at  datetime     not null,
    end_at   datetime     null,
    status   varchar(255) not null,
    id_stack bigint       not null,
    constraint job_stack_id_fk
        foreign key (id_stack) references stack (id)
);

create table if not exists resource
(
    id             bigint auto_increment primary key,
    name           varchar(255)         null,
    routing        varchar(255)         null,
    resource_order int                  not null,
    deleted        tinyint(1) default 0 null,
    id_stack       bigint               not null,

    constraint resource_stack_id_fk foreign key (id_stack) references stack (id)
);

create table if not exists step
(
    id           bigint auto_increment primary key,
    uuid         varchar(255) not null,
    payload      json         null,
    error_tracer mediumtext   null,
    init_at      datetime     not null,
    end_at       datetime     null,
    status       varchar(255) not null,
    id_job       bigint       not null,
    id_resource  bigint       not null,

    constraint step_job_id_fk foreign key (id_job) references job (id),
    constraint step_resource_id_fk foreign key (id_resource) references resource (id)
);

