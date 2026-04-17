alter table document_mapping
    add column event_type_id integer;

update document_mapping dm
    set event_type_id = et.id
    from event_type et
    where et.event_name = dm.event_type;

alter table document_mapping
    alter column event_type_id set not null;

alter table document_mapping
    add constraint fk_document_mapping_event_type
        foreign key (event_type_id) references event_type(id);

drop index idx_document_mapping_event_type;

create index idx_document_mapping_event_type_id on document_mapping(event_type_id);

alter table document_mapping
    drop column event_type;