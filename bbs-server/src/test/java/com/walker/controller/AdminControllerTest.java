package com.walker.controller;

import com.walker.vo.ResultBean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 旧版 /admin/login 已停用：不再读取 bbs_admin 明文密码表，也不再返回任何账号信息。
 */
class AdminControllerTest {

    @Test
    @DisplayName("旧版 /admin/login 返回停用提示")
    void legacyLoginDisabled() {
        ResultBean result = new AdminController().login();

        assertEquals(500, result.getCode());
        assertTrue(result.getMessage().contains("/common/login"));
    }
}
