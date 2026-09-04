package com.example.leagueticket.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record SeatBatchRequest(@NotEmpty List<@Valid RowDefinition> rows) {
    public record RowDefinition(@NotNull @Positive Integer rowNo,
                                @NotBlank @Size(max=20) String rowLabel,
                                @NotNull @Positive Integer startSeatNo,
                                @NotNull @Positive Integer seatCount) {}
}
