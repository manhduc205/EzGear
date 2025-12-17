package com.manhduc205.ezgear.services.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.manhduc205.ezgear.components.JwtTokenUtil;

import com.manhduc205.ezgear.dtos.request.LoginRequest;
import com.manhduc205.ezgear.dtos.request.LogoutRequest;
import com.manhduc205.ezgear.dtos.request.SocialLoginRequest;
import com.manhduc205.ezgear.dtos.responses.AuthResponse;
import com.manhduc205.ezgear.exceptions.RequestException;
import com.manhduc205.ezgear.models.User;
import com.manhduc205.ezgear.repositories.UserRepository;
import com.manhduc205.ezgear.services.AuthService;
import com.manhduc205.ezgear.services.BlacklistService;
import com.manhduc205.ezgear.services.RedisService;
import com.manhduc205.ezgear.services.UserService;
import com.manhduc205.ezgear.enums.TokenType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtTokenUtil jwtTokenUtil;
    private final UserRepository userRepository;
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final RedisService  redisService;
    private final BlacklistService blacklistService;

    @Value("${google.client-id}")
    private String googleClientId;

    //Trong môi trường thực tế, nên sử dụng Authorization Server
    // chuyên dụng như Keycloak, Auth0, hoặc các nền tảng IDaaS.
    private AuthResponse generateAuthResponse(User user, Authentication authentication) {
        String accessToken = jwtTokenUtil.generateAccessToken(authentication);
        String refreshToken = jwtTokenUtil.generateRefreshToken(user.getUsername());

        // lưu refreshToken vào Redis
        redisService.saveTokenRefresh(
                user.getId(),
                refreshToken,
                jwtTokenUtil.getRefreshTokenExpiryDuration(),
                TimeUnit.MILLISECONDS
        );

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType(TokenType.ACCESS_TOKEN)
                .userId(user.getId())
                .username(user.getEmail())
                .roles(user.getUserRoles()
                        .stream()
                        .map(role -> "ROLE_" + role.getRole().getCode().toUpperCase())
                        .collect(Collectors.toSet()))
                .branchId(user.getBranch() != null ? user.getBranch().getId() : null)
                .build();
    }

    @Override
    public AuthResponse authenticateUser(LoginRequest request) {
        log.info("Authenticating user: {}", request.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return generateAuthResponse(user,authentication);
    }

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(SocialLoginRequest request) {
        try {
            // 1. Verify Token với Google
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(request.getToken());
            if (idToken == null) {
                throw new RequestException("Invalid Google Token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String googleId = payload.getSubject();
            String name = (String) payload.get("name");
            String picture = (String) payload.get("picture");

            return processSocialLogin(email, name, googleId, "GOOGLE");

        } catch (Exception e) {
            log.error("Google Login Error: ", e);
            throw new RequestException("Google Login Failed");
        }
    }

    // ================== FACEBOOK LOGIN ==================
    @Override
    @Transactional
    public AuthResponse loginWithFacebook(SocialLoginRequest request) {
        try {
            // 1. Gọi Graph API của Facebook để lấy thông tin user từ accessToken
            // API: https://graph.facebook.com/me?fields=id,name,email,picture&access_token=...

            WebClient webClient = WebClient.create();
            Map fbUserInfo = webClient.get()
                    .uri("https://graph.facebook.com/me?fields=id,name,email,picture&access_token=" + request.getToken())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(); // Block để lấy kết quả đồng bộ

            if (fbUserInfo == null || fbUserInfo.get("id") == null) {
                throw new RequestException("Invalid Facebook Token");
            }

            String facebookId = (String) fbUserInfo.get("id");
            String name = (String) fbUserInfo.get("name");
            String email = (String) fbUserInfo.get("email");

            // Lưu ý: Facebook đôi khi không trả về email (nếu đk bằng sđt).
            // Nếu không có email, bạn có thể generate email giả dạng: id@facebook.com hoặc bắt user cập nhật sau.
            if (email == null) {
                email = facebookId + "@facebook.ezgear.com";
            }

            return processSocialLogin(email, name, facebookId, "FACEBOOK");

        } catch (Exception e) {
            log.error("Facebook Login Error: ", e);
            throw new RequestException("Facebook Login Failed");
        }
    }

    // ================== HÀM XỬ LÝ CHUNG (MERGE ACCOUNT) ==================
    private AuthResponse processSocialLogin(String email, String name, String socialId, String provider) {
        // 1. Kiểm tra User có tồn tại không
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            // CASE A: User mới tinh -> Tạo mới
            user = User.builder()
                    .email(email)
                    .fullName(name)
                    .passwordHash(null) // Không có pass
                    .status(User.Status.ACTIVE)
                    .isStaff(false)
                    .userRoles(new HashSet<>()) // Cần logic set Role mặc định ở đây
                    .build();

            // Set ID tương ứng
            if ("GOOGLE".equals(provider)) user.setGoogleAccountId(socialId);
            else if ("FACEBOOK".equals(provider)) user.setFacebookAccountId(socialId);

            // Gán Role (Ví dụ lấy role USER từ DB)
            // Role userRole = roleRepository.findByName("USER");
            // user.getUserRoles().add(new UserRole(user, userRole));

            user = userRepository.save(user);
        } else {
            // CASE B: User đã tồn tại -> Update ID nếu chưa có (Merge)
            boolean isUpdated = false;
            if ("GOOGLE".equals(provider) && user.getGoogleAccountId() == null) {
                user.setGoogleAccountId(socialId);
                isUpdated = true;
            } else if ("FACEBOOK".equals(provider) && user.getFacebookAccountId() == null) {
                user.setFacebookAccountId(socialId);
                isUpdated = true;
            }

            if (isUpdated) {
                user = userRepository.save(user);
            }
        }

        // 2. Tự tạo Authentication (Vì không có password)
        // Tạo đối tượng UserDetails/CustomUserDetails từ user tìm được
        // Lưu ý: CustomUserDetails cần xử lý trường hợp password null

        // Giả sử bạn dùng CustomUserDetails:
        // CustomUserDetails customUserDetails = new CustomUserDetails(user);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user, // Hoặc customUserDetails
                null,
                user.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. Sinh Token trả về
        return generateAuthResponse(user, authentication);
    }

    @Transactional
    @Override
    public void logout(LogoutRequest logoutRequest) {
        String accessToken = logoutRequest.getAccessToken();

        if (accessToken == null || accessToken.isEmpty()) {
            throw new RequestException("Access token is required");
        }


        if (jwtTokenUtil.isTokenExpired(accessToken)) {

            throw new RequestException("Token expired");
        }
        // lấy sub từ token
        String subject = jwtTokenUtil.extractSubject(accessToken);

        User user = userService.findByEmail(subject)
                .orElseThrow(() -> new RequestException("User not found from token"));

        Long userId = user.getId();

        // Xóa Refresh Token trong Redis
        redisService.deleteTokenRefresh(userId);
        log.info("Refresh token for user {} removed from Redis", userId);

        // cho Access Token vào Blacklist
        try {
            Date exp = jwtTokenUtil.getAccessTokenExpiry(accessToken);
            long ttl = exp.getTime() - System.currentTimeMillis();
            if (ttl > 0) {
                blacklistService.addToBlacklist(accessToken, ttl);
                log.info("Access token blacklisted for {} ms", ttl);
            }
        } catch (Exception e) {
            log.warn("Error processing logout blacklist: {}", e.getMessage());
        }
    }

}
