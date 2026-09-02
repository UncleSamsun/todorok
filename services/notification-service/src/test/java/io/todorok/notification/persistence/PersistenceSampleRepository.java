package io.todorok.notification.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PersistenceSampleRepository extends JpaRepository<PersistenceSample, UUID> {}
