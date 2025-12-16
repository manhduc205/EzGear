package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.components.Translator;
import com.manhduc205.ezgear.dtos.UserDTO;
import com.manhduc205.ezgear.dtos.responses.UserResponse;
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
            throw new DataIntegrityViolationException(Translator.toLocale("error.user.phone_exists"));
        }
        if(userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
            throw new DataIntegrityViolationException(Translator.toLocale("error.user.email_exists"));
        }

        Role role = roleRepository.findById(userDTO.getRoleId())
                .orElseThrow(() -> new DataIntegrityViolationException(Translator.toLocale("error.role.not_found")));
        if (role.getCode().equalsIgnoreCase("SYS_ADMIN")) {
            throw new BadCredentialsException(Translator.toLocale("error.user.cannot_create_sys_admin"));
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
    public UserResponse getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DataNotFoundException(Translator.toLocale("error.user.not_found")));

        // Tái sử dụng hàm static fromUser
        return UserResponse.fromUser(user);
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException(Translator.toLocale("error.user.not_found_by_id", id)));
    }
    @Override
    public boolean isSysAdmin(User user) {
        if (user.getUserRoles() == null) return false;
        return user.getUserRoles().stream()
                .anyMatch(ur -> ur.getRole().getCode().equalsIgnoreCase("SYS_ADMIN"));
    }
}
