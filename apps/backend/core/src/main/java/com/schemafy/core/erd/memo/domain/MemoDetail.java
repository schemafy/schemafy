package com.schemafy.core.erd.memo.domain;

import java.util.List;

public record MemoDetail(
    Memo memo,
    List<MemoComment> comments) {
}
