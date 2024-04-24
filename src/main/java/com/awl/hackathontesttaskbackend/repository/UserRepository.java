package com.awl.hackathontesttaskbackend.repository;


import com.awl.hackathontesttaskbackend.dto.UserDto;
import com.awl.hackathontesttaskbackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Query("select new com.awl.hackathontesttaskbackend.dto.UserDto(u.id, u.email, u.phoneNumber, u.imageUrl, u.firstName, u.lastName,u.isHelper) from User as u where u.email = :email")
    Optional<UserDto> findUserDtoByEmail(@Param("email") String email);
    Optional<User> findUserById(Long id);
    Optional<User> findUserByEmail(String email);


}
