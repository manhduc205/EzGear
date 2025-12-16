package com.manhduc205.ezgear.services;


import com.manhduc205.ezgear.dtos.UserDTO;
import com.manhduc205.ezgear.dtos.responses.UserResponse;
import com.manhduc205.ezgear.models.User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public interface UserService {
    User createUser(UserDTO userDTO);
    Optional<User> findByEmail(String email);
    UserResponse getMyProfile(Long userId);
    String getUserEmail(Long userId);
    User getUserById(Long id);
    boolean isSysAdmin(User user);
}
