package org.altechego.leavewise.user.mapper;

import org.altechego.leavewise.user.dto.UserResponse;
import org.altechego.leavewise.user.entity.LwUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "companyId", source = "company.id")
    @Mapping(target = "companyLocationId", source = "companyLocation.id")
    UserResponse toResponse(LwUser user);
}
