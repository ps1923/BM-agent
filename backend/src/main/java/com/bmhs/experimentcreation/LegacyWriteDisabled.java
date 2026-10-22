package com.bmhs.experimentcreation;

import org.springframework.http.HttpStatus;

public final class LegacyWriteDisabled {
    private LegacyWriteDisabled() {}

    public static ApiException exception() {
        return new ApiException(HttpStatus.GONE, "LEGACY_WRITE_DISABLED",
                "旧工作区和 DSH 写入入口已停用；历史记录仍可读取，请使用 Git 提交流程。");
    }
}
