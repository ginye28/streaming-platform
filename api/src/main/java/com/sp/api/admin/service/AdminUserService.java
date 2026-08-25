package com.sp.api.admin.service;

import com.sp.api.admin.dto.AdminUserResponse;
import com.sp.api.admin.dto.ChangeRoleRequest;
import com.sp.api.common.exception.BadRequestException;
import com.sp.api.common.exception.NotFoundException;
import com.sp.api.common.response.PageResponse;
import com.sp.api.user.entity.User;
import com.sp.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserRepository userRepository;

    public PageResponse<AdminUserResponse> findAll(User.Role role, Pageable pageable) {

        Page<User> page = role != null
                ? userRepository.findByRole(role, pageable)
                : userRepository.findAll(pageable);

        return PageResponse.from(page.map(AdminUserResponse::from));
    }

    @Transactional
    public AdminUserResponse changeRole(Long userId, ChangeRoleRequest request, String requesterEmail) {

        User target = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        // 스스로 권한을 내리면 되돌릴 수단이 없어진다.
        // 요청자가 ADMIN 이어야 여기 도달하므로, 이 검사만 있으면 마지막 관리자도 함께 지켜진다.
        if (target.getEmail().equals(requesterEmail) && request.getRole() != User.Role.ADMIN) {
            throw new BadRequestException("자신의 관리자 권한은 해제할 수 없습니다.");
        }

        target.changeRole(request.getRole());

        return AdminUserResponse.from(target);
    }
}
