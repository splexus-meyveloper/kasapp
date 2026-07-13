package org.example.dto.response;

import java.util.List;

public record SheetPreviewResponse(
        List<List<String>> rows
) {}
