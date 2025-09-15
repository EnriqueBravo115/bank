package dev.enrique.bank.service.util;

import java.util.ArrayList;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BasicMapper {
    private final ModelMapper mapper;

    public <T, S> S convertToResponse(T data, Class<S> type) {
        return mapper.map(data, type);
    }

    public <T, S> List<S> convertToResponseList(List<T> lists, Class<S> type) {
        return lists.contains(null) ? new ArrayList<>()
                : lists.stream()
                        .map(list -> convertToResponse(list, type))
                        .toList();
    }
}
