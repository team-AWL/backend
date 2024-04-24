package com.awl.hackathontesttaskbackend.repository;


import com.awl.hackathontesttaskbackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<Long, User> {
}
