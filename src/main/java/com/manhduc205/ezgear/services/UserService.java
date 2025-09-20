package com.manhduc205.ezgear.services;


import com.manhduc205.ezgear.dto.UserDTO;
import com.manhduc205.ezgear.models.User;
import org.springframework.stereotype.Service;

import java.util.Optional;
@Service
public interface UserService {
    User createUser(UserDTO userDTO);
}
