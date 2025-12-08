package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.dtos.UserDTO;
import com.manhduc205.ezgear.exceptions.DataNotFoundException;
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

import java.util.Optional;
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
        boolean staffFlag = false;
        if (role.getCode().equalsIgnoreCase("SYS_ADMIN")  || role.getCode().equalsIgnoreCase("ADMIN")) {
            staffFlag = true;
        }
        // kiểm tra xem nếu có accontId, không yêu cầu pass
        User newUser = User.builder()
                .email(userDTO.getEmail())
                .phone(userDTO.getPhoneNumber())
                .fullName(userDTO.getFullName())
                .passwordHash(passwordEncoder.encode(userDTO.getPassword()))
                .status(User.Status.ACTIVE)
                .isStaff(staffFlag)
                .build();

        UserRole userRole = UserRole.builder()
                .user(newUser)
                .role(role)
                .build();

        newUser.setUserRoles(Set.of(userRole));
        return userRepository.save(newUser);

    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    @Override
    public String getUserEmail(Long userId) {
        return userRepository.findById(userId)
                .map(User::getEmail)
                .orElse("default@example.com");

    }
    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy người dùng với ID: " + id));
    }
    @Override
    public boolean isSysAdmin(User user) {
        if (user.getUserRoles() == null) return false;
        return user.getUserRoles().stream()
                .anyMatch(ur -> ur.getRole().getCode().equalsIgnoreCase("SYS_ADMIN"));
    }
}
