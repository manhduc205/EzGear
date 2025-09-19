package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.models.User;
import com.manhduc205.ezgear.repositories.UserRepository;
import com.manhduc205.ezgear.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    @Override
    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
