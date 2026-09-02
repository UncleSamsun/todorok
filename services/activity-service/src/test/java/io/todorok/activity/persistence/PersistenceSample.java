package io.todorok.activity.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

@Entity(name = "ActivityPersistenceSample")
@Table(name = "persistence_sample", schema = "activity")
class PersistenceSample {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Version
    @Column(nullable = false)
    private long version;

    protected PersistenceSample() {}

    PersistenceSample(String name) {
        this.id = UUID.randomUUID();
        this.name = name;
    }

    void rename(String name) {
        this.name = name;
    }

    UUID id() {
        return id;
    }

    String name() {
        return name;
    }

    long version() {
        return version;
    }
}
