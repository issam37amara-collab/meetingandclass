package com.site.meetingandclass.repository;

import com.site.meetingandclass.model.ClassTimetable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassTimetableRepository extends JpaRepository<ClassTimetable, Long> {

    List<ClassTimetable> findByTitleContainingIgnoreCase(String title);
}
