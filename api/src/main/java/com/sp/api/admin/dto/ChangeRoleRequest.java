package com.sp.api.admin.dto;

import com.sp.api.user.entity.User;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChangeRoleRequest {

    @NotNull
    private User.Role role;
}
