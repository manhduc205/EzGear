package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.dtos.UserDTO;
import com.manhduc205.ezgear.models.Role;
import com.manhduc205.ezgear.models.User;
import com.manhduc205.ezgear.models.UserRole;
import com.manhduc205.ezgear.repositories.RoleRepository;
import com.manhduc205.ezgear.repositories.UserRepository;
import com.manhduc205.ezgear.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    @Override
    public User createUser(UserDTO userDTO) {

        String phoneNumber = userDTO.getPhoneNumber();
        if(userRepository.existsByPhone(phoneNumber)) {
            throw new DataIntegrityViolationException("Phone number already exists");
        }
        if(userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
            throw new DataIntegrityViolationException("Email already exists");
        }

        Role role = roleRepository.findById(userDTO.getRoleId())
                .orElseThrow(() -> new DataIntegrityViolationException("Role Not Found"));
        if (role.getCode().equalsIgnoreCase("SYS_ADMIN")) {
            throw new BadCredentialsException("You cannot self-register as system-admin");
        }
        // kiểm tra xem nếu có accontId, không yêu cầu pass
        User newUser = User.builder()
                .email(userDTO.getEmail())
                .phone(userDTO.getPhoneNumber())
                .fullName(userDTO.getFullName())
                .passwordHash(passwordEncoder.encode(userDTO.getPassword()))
                .status(User.Status.ACTIVE)
                .isStaff(false)
                .build();

        UserRole userRole = UserRole.builder()
                .user(newUser)
                .role(role)
                .build();

        newUser.setUserRoles(Set.of(userRole));
        return userRepository.save(newUser);

    }
}
