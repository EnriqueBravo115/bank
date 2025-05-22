package dev.enrique.bank.mapper;

import static dev.enrique.bank.commons.constants.PathConstants.PAGE_TOTAL_COUNT;

import java.util.ArrayList;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import dev.enrique.bank.dto.response.HeaderResponse;
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

    public <T, S> HeaderResponse<S> getHeaderResponse(Page<T> pageableItems, Class<S> type) {
        if (pageableItems == null) {
            throw new IllegalArgumentException("Pageable items cannot be null");
        }
        List<S> responses = convertToResponseList(pageableItems.getContent(), type);
        return constructHeaderResponse(responses, pageableItems.getTotalPages());
    }

    public <T, S> HeaderResponse<S> getHeaderResponse(List<T> items, Integer totalPages, Class<S> type) {
        List<S> responses = convertToResponseList(items, type);
        return constructHeaderResponse(responses, totalPages);
    }

    private <S> HeaderResponse<S> constructHeaderResponse(List<S> responses, Integer totalPages) {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(PAGE_TOTAL_COUNT, String.valueOf(totalPages));
        return new HeaderResponse<S>(responses, responseHeaders);
    }
}
