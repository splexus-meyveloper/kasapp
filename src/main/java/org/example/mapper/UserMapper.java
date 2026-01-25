package org.example.mapper;

import org.example.dto.request.RegisterRequest;
import org.example.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring",unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);
    User fromRegisterDto(RegisterRequest request);
}
