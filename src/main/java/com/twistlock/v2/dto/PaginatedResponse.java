package com.twistlock.v2.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PaginatedResponse {
    private int pageNumber;
    private int pageSize;
    private int totalPages;
    private List<PackageDetails> packages;
}
