package com.glvov.springairag.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "loaded_document")
@Getter
@Setter
@Accessors(chain = true)
public class LoadedDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "filename")
    private String filename;

    @Column(name = "content_hash")
    private String contentHash; // hash of the loaded file to read or not read the file

    @Column(name = "chunk_count")
    private int chunkCount;

    @CreationTimestamp
    @TimeZoneStorage(TimeZoneStorageType.NORMALIZE)
    private ZonedDateTime loadedAt;
}
