package io.todorok.planner.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PersistenceSampleRepository extends JpaRepository<PersistenceSample, UUID> {}
