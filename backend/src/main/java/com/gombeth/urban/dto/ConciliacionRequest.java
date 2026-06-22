package com.gombeth.urban.dto;

import java.util.List;

public record ConciliacionRequest(
        List<Long> reciboIds
) {
}