package com.awl.hackathontesttaskbackend.service;


import com.awl.hackathontesttaskbackend.dto.user.UpdateEmailDto;
import com.awl.hackathontesttaskbackend.dto.user.UpdateOptionalUserInfoDto;
import com.awl.hackathontesttaskbackend.dto.user.UpdatePasswordDto;
import com.awl.hackathontesttaskbackend.dto.user.UserDto;
import com.awl.hackathontesttaskbackend.enums.AuthProvider;
import com.awl.hackathontesttaskbackend.enums.ERole;
import com.awl.hackathontesttaskbackend.exeptions.*;
import com.awl.hackathontesttaskbackend.model.Need;
import com.awl.hackathontesttaskbackend.model.User;
import com.awl.hackathontesttaskbackend.repository.NeedRepository;
import com.awl.hackathontesttaskbackend.repository.UserRepository;
import com.awl.hackathontesttaskbackend.request.SignupRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {
    public static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final NeedRepository needRepository;
    private final EmailSenderService emailSenderService;
    private final BCryptPasswordEncoder passwordEncoder;


    public UserService(UserRepository userRepository, NeedRepository needRepository, EmailSenderService emailSenderService, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.needRepository = needRepository;
        this.emailSenderService = emailSenderService;
        this.passwordEncoder = passwordEncoder;
    }

    public UserDto getCurrentUserDto(Principal principal) {
        return getUserDtoByPrincipal(principal);
    }
    public User getCurrentUser(Principal principal) {
        return getUserByPrincipal(principal);
    }

    private UserDto getUserDtoByPrincipal(Principal principal) {
        String email = principal.getName();
        return userRepository.findUserDtoByEmail(email).orElseThrow(() -> new EmailNotFoundException("User not found with email " + email));
    }
    private User getUserByPrincipal(Principal principal) {
        String email = principal.getName();
        return userRepository.findUserByEmail(email).orElseThrow(() -> new EmailNotFoundException("User not found with email " + email));
    }

    public void createUser(SignupRequest userIn) {
        User user = new User();
        user.setEmail(userIn.getEmail());
        user.setImageUrl("https://cdn.pixabay.com/photo/2014/03/25/16/54/user-297566_640.png");
        user.setName(userIn.getName());
        user.setIsHelper(userIn.getIsHelper());
        user.setProvider(AuthProvider.local);
        user.setPassword(passwordEncoder.encode(userIn.getPassword()));
        user.getRoles().add(ERole.USER);
        System.out.println(userRepository.findUserByEmail(user.getEmail()).isPresent());
        try {
            userRepository.save(user);
            LOG.info("Saving User {}", userIn.getEmail());
        } catch (Exception e) {
            LOG.error("Error during registration. {}",e.getMessage());
            throw new EmailAlreadyExistException("The user with email " + user.getEmail() + " already exist. Please check credentials");
        }


    }
    public User updateOptionalInfoUser(UpdateOptionalUserInfoDto updateOptionalUserInfoDto, Principal principal) {
        User user = getUserByPrincipal(principal);
        user.setImageUrl(updateOptionalUserInfoDto.getImageUrl());
        user.setBio(updateOptionalUserInfoDto.getBio());
        user.setPhoneNumber(updateOptionalUserInfoDto.getPhoneNumber());
        user.setName(updateOptionalUserInfoDto.getName());
        return userRepository.save(user);
    }

    public String updateEmail(UpdateEmailDto updateEmailDto, Principal principal) {
        User user = getUserByPrincipal(principal);
        String email  = updateEmailDto.getEmail();
        boolean isPresent = userRepository.findUserByEmail(email).isPresent();
        if(!isPresent){
            user.setEmail(email);
            userRepository.save(user);
            return email;
        }
        else {
            throw new EmailAlreadyExistException("Email already used");
        }
    }


    public String updatePassword(UpdatePasswordDto updatePasswordDto, Principal principal) {
        User user = getUserByPrincipal(principal);
        boolean isMatchesPassword = isTruePassword(updatePasswordDto,user);
        if (isMatchesPassword){
            user.setPassword(passwordEncoder.encode(updatePasswordDto.getNewPassword()));
            return "Password change successfully";
        }
        else {
            throw new OldPasswordIsIncorrectException("Old password is incorrect");
        }
    }

    public boolean isTruePassword(UpdatePasswordDto updatePasswordDto,User user){
        if(user != null){
            return passwordEncoder.matches(updatePasswordDto.getOldPassword(),user.getPassword());
        }
        else {
            return false;
        }
    }
    public void forgotPassword(String email) {
        User user = userRepository.findUserByEmail(email).orElse(null);
        if (user == null) {
            LOG.error("User with email:{} not found ", email);
            throw new UserNotExistException("User with email: "+email+" not found");
        } else {
            String resetPasswordToken = UUID.randomUUID().toString();
            setNewResetPasswordToken(resetPasswordToken, email);
            String message = String.format(
                    "Hello, %s! \n" +
                            "Welcome to Hackathon. Please, visit next link: http://localhost:3000/forget/reset_password?token=%s",
                    user.getUsername(),
                    resetPasswordToken
            );
            emailSenderService.sendMail(email, "FORGOT YOUR PASSWORD", message);
        }
    }
    public User getByResetToken(String resetPasswordToken) {
        return userRepository.findUserByResetPasswordToken(resetPasswordToken).orElseThrow(() -> new UserNotExistException("User with token: " + resetPasswordToken + " not found"));
    }
    private void setNewResetPasswordToken(String token, String email) {
        User user = userRepository.findUserByEmail(email).orElse(null);
        if (user != null) {
            user.setResetPasswordToken(token);
            userRepository.save(user);
        }

    }
    public void updateForgotPassword(User user, String newPassword) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encodePassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodePassword);
        user.setResetPasswordToken(null);
        userRepository.save(user);
    }

    public void addNeedToSaved(Long needId, Principal principal) {
        Need needToAdd = needRepository.findNeedById(needId).orElseThrow(()-> new NeedNotFoundException("Need not found"));
        User user = getUserByPrincipal(principal);
        user.getSelectedNeeded().add(needToAdd);
        userRepository.save(user);
    }

    public List<Need> getSavedNeedToCurrentUser(Principal principal) {
        User user = getUserByPrincipal(principal);
        return user.getSelectedNeeded();
    }

    public List<Need> getNeedToCurrentUser(Principal principal) {
        User user = getUserByPrincipal(principal);
        return user.getNeedList();
    }
}
