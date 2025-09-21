package com.manhduc205.ezgear.services;


import com.manhduc205.ezgear.dtos.UserDTO;
import com.manhduc205.ezgear.models.User;
import org.springframework.stereotype.Service;

@Service
public interface UserService {
    User createUser(UserDTO userDTO);
}
